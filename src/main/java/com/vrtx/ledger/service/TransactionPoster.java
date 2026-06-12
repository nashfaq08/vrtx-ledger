package com.vrtx.ledger.service;

import com.vrtx.ledger.domain.Account;
import com.vrtx.ledger.domain.AccountType;
import com.vrtx.ledger.domain.EntryDirection;
import com.vrtx.ledger.domain.LedgerEntry;
import com.vrtx.ledger.domain.LedgerTransaction;
import com.vrtx.ledger.domain.TransactionType;
import com.vrtx.ledger.dto.PaymentRequest;
import com.vrtx.ledger.dto.RefundRequest;
import com.vrtx.ledger.dto.SettlementRequest;
import com.vrtx.ledger.dto.TransferRequest;
import com.vrtx.ledger.exception.AccountNotFoundException;
import com.vrtx.ledger.exception.CurrencyMismatchException;
import com.vrtx.ledger.exception.InsufficientFundsException;
import com.vrtx.ledger.exception.InvalidRefundException;
import com.vrtx.ledger.exception.InvalidTransactionException;
import com.vrtx.ledger.exception.TransactionNotFoundException;
import com.vrtx.ledger.exception.UnbalancedTransactionException;
import com.vrtx.ledger.repository.AccountRepository;
import com.vrtx.ledger.repository.LedgerEntryRepository;
import com.vrtx.ledger.repository.LedgerTransactionRepository;
import com.vrtx.ledger.service.fee.FeeCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionPoster {

    static final int MONEY_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final AccountRepository accountRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final FeeCalculator feeCalculator;

    public TransactionPoster(AccountRepository accountRepository,
                             LedgerTransactionRepository transactionRepository,
                             LedgerEntryRepository entryRepository,
                             FeeCalculator feeCalculator) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.feeCalculator = feeCalculator;
    }

    @Transactional
    public LedgerTransaction createPayment(PaymentRequest req) {
        Map<UUID, Account> accounts = lockAll(List.of(req.payerAccountId(), req.payeeAccountId()));
        Account payer = accounts.get(req.payerAccountId());
        Account payee = accounts.get(req.payeeAccountId());

        String currency = requireSameCurrency(payer, payee);
        BigDecimal amount = scale(req.amount());
        BigDecimal fee = req.fee() != null ? scale(req.fee()) : feeCalculator.calculate(amount);
        if (fee.compareTo(amount) > 0) {
            throw new InvalidTransactionException("Fee cannot exceed the payment amount");
        }
        Account feesAccount = loadFeesAccount(currency);
        BigDecimal merchantPortion = amount.subtract(fee);

        List<EntrySpec> specs = new ArrayList<>();
        specs.add(EntrySpec.debit(payer, amount));
        specs.add(EntrySpec.credit(payee, merchantPortion));
        if (fee.signum() > 0) {
            specs.add(EntrySpec.credit(feesAccount, fee));
        }

        return post(TransactionType.PAYMENT, currency, amount, fee, null,
                req.description(), req.idempotencyKey(), specs);
    }

    @Transactional
    public LedgerTransaction createTransfer(TransferRequest req) {
        if (req.fromAccountId().equals(req.toAccountId())) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }
        Map<UUID, Account> accounts = lockAll(List.of(req.fromAccountId(), req.toAccountId()));
        Account from = accounts.get(req.fromAccountId());
        Account to = accounts.get(req.toAccountId());

        String currency = requireSameCurrency(from, to);
        BigDecimal amount = scale(req.amount());

        List<EntrySpec> specs = List.of(
                EntrySpec.debit(from, amount),
                EntrySpec.credit(to, amount));

        return post(TransactionType.TRANSFER, currency, amount, BigDecimal.ZERO, null,
                req.description(), req.idempotencyKey(), specs);
    }

    @Transactional
    public LedgerTransaction createSettlement(SettlementRequest req) {
        if (req.merchantAccountId().equals(req.settlementAccountId())) {
            throw new InvalidTransactionException("Merchant and settlement accounts must differ");
        }
        Map<UUID, Account> accounts = lockAll(List.of(req.merchantAccountId(), req.settlementAccountId()));
        Account merchant = accounts.get(req.merchantAccountId());
        Account settlement = accounts.get(req.settlementAccountId());

        String currency = requireSameCurrency(merchant, settlement);

        // If no amount supplied, settle the merchant's full available balance.
        BigDecimal amount = req.amount() != null
                ? scale(req.amount())
                : entryRepository.computeBalance(merchant.getId());
        if (amount.signum() <= 0) {
            throw new InvalidTransactionException("Nothing to settle: merchant balance is " + amount);
        }

        List<EntrySpec> specs = List.of(
                EntrySpec.debit(merchant, amount),
                EntrySpec.credit(settlement, amount));

        return post(TransactionType.SETTLEMENT, currency, amount, BigDecimal.ZERO, null,
                req.description(), req.idempotencyKey(), specs);
    }

    @Transactional
    public LedgerTransaction createRefund(RefundRequest req) {
        LedgerTransaction original = transactionRepository.findById(req.originalTransactionId())
                .orElseThrow(() -> new TransactionNotFoundException(req.originalTransactionId()));
        if (original.getType() != TransactionType.PAYMENT) {
            throw new InvalidRefundException("Only PAYMENT transactions can be refunded");
        }

        BigDecimal alreadyRefunded = transactionRepository.sumRefundedAmount(original.getId());
        BigDecimal remaining = original.getAmount().subtract(alreadyRefunded);
        BigDecimal refundAmount = req.amount() != null ? scale(req.amount()) : remaining;

        if (refundAmount.signum() <= 0) {
            throw new InvalidRefundException("Refund amount must be positive");
        }
        if (refundAmount.compareTo(remaining) > 0) {
            throw new InvalidRefundException("Refund exceeds remaining refundable amount: requested="
                    + refundAmount + " remaining=" + remaining);
        }

        // Identify the original legs by role.
        Account payer = null;
        Account merchant = null;
        Account feesAccount = null;
        for (LedgerEntry e : original.getEntries()) {
            if (e.getDirection() == EntryDirection.DEBIT) {
                payer = e.getAccount();
            } else if (e.getAccount().getType() == AccountType.FEES) {
                feesAccount = e.getAccount();
            } else {
                merchant = e.getAccount();
            }
        }
        if (payer == null || merchant == null) {
            throw new InvalidRefundException("Original payment has an unexpected structure");
        }

        // Proportional fee reversal so partial refunds stay balanced.
        BigDecimal ratio = refundAmount.divide(original.getAmount(), 10, ROUNDING);
        BigDecimal refundFee = scale(original.getFee().multiply(ratio));
        BigDecimal merchantPortion = refundAmount.subtract(refundFee);
        if (merchantPortion.signum() < 0) {
            refundFee = refundAmount;
            merchantPortion = BigDecimal.ZERO;
        }

        // Lock the same accounts (sorted inside lockAll)
        List<UUID> ids = new ArrayList<>(List.of(payer.getId(), merchant.getId()));
        if (feesAccount != null) {
            ids.add(feesAccount.getId());
        }
        Map<UUID, Account> locked = lockAll(ids);
        payer = locked.get(payer.getId());
        merchant = locked.get(merchant.getId());
        if (feesAccount != null) {
            feesAccount = locked.get(feesAccount.getId());
        }

        String currency = original.getCurrency();
        List<EntrySpec> specs = new ArrayList<>();
        specs.add(EntrySpec.credit(payer, refundAmount));
        if (merchantPortion.signum() > 0) {
            specs.add(EntrySpec.debit(merchant, merchantPortion));
        }
        if (refundFee.signum() > 0 && feesAccount != null) {
            specs.add(EntrySpec.debit(feesAccount, refundFee));
        }

        return post(TransactionType.REFUND, currency, refundAmount, refundFee, original,
                req.description(), req.idempotencyKey(), specs);
    }

    // CORE transaction posting
    private LedgerTransaction post(TransactionType type, String currency, BigDecimal amount,
                                   BigDecimal fee, LedgerTransaction related, String description,
                                   String idempotencyKey, List<EntrySpec> specs) {
        validateBalanced(specs, currency);
        enforceFunds(specs);

        LedgerTransaction txn = new LedgerTransaction(type, idempotencyKey, currency, amount, fee,
                related, description);
        for (EntrySpec spec : specs) {
            txn.addEntry(new LedgerEntry(spec.account(), spec.direction(), spec.amount(), currency));
        }

        LedgerTransaction saved = transactionRepository.save(txn);
        return saved;
    }

    // Sum of debits must equal sum of credits and check the currency mismatch
    private void validateBalanced(List<EntrySpec> specs, String currency) {
        if (specs.size() < 2) {
            throw new UnbalancedTransactionException(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (EntrySpec s : specs) {
            if (s.amount().signum() <= 0) {
                throw new InvalidTransactionException("Entry amounts must be positive");
            }
            if (!s.account().getCurrency().equals(currency)) {
                throw new CurrencyMismatchException("Account " + s.account().getId()
                        + " currency " + s.account().getCurrency() + " != transaction currency " + currency);
            }
            if (s.direction() == EntryDirection.DEBIT) {
                debits = debits.add(s.amount());
            } else {
                credits = credits.add(s.amount());
            }
        }
        if (debits.compareTo(credits) != 0) {
            throw new UnbalancedTransactionException(debits, credits);
        }
    }

    // Non-overdraft accounts (USER, MERCHANT) may not be driven below zero. */
    private void enforceFunds(List<EntrySpec> specs) {
        Map<UUID, BigDecimal> netByAccount = new LinkedHashMap<>();
        Map<UUID, Account> accountById = new LinkedHashMap<>();
        for (EntrySpec s : specs) {
            accountById.put(s.account().getId(), s.account());
            netByAccount.merge(s.account().getId(), s.signedAmount(), BigDecimal::add);
        }
        for (Map.Entry<UUID, BigDecimal> e : netByAccount.entrySet()) {
            Account account = accountById.get(e.getKey());
            BigDecimal net = e.getValue();
            if (net.signum() < 0 && !account.getType().allowsOverdraft()) {
                BigDecimal before = entryRepository.computeBalance(account.getId());
                BigDecimal after = before.add(net);
                if (after.signum() < 0) {
                    throw new InsufficientFundsException(account.getId(), before, net.negate());
                }
            }
        }
    }

    // Locks every involved account to achieve concurrency
    private Map<UUID, Account> lockAll(List<UUID> ids) {
        Map<UUID, Account> result = new LinkedHashMap<>();
        ids.stream().distinct().sorted().forEach(id -> {
            Account account = accountRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new AccountNotFoundException(id));
            result.put(id, account);
        });
        return result;
    }

    private Account loadFeesAccount(String currency) {
        return accountRepository.findAll().stream()
                .filter(a -> a.getType() == AccountType.FEES && a.getCurrency().equals(currency))
                .findFirst()
                .orElseThrow(() -> new InvalidTransactionException(
                        "No FEES account configured for currency " + currency));
    }

    private String requireSameCurrency(Account a, Account b) {
        if (!a.getCurrency().equals(b.getCurrency())) {
            throw new CurrencyMismatchException("Accounts have different currencies: "
                    + a.getCurrency() + " vs " + b.getCurrency());
        }
        return a.getCurrency();
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING);
    }
}
