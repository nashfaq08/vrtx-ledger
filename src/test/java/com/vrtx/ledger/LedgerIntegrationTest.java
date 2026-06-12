package com.vrtx.ledger;

import com.vrtx.ledger.dto.PaymentRequest;
import com.vrtx.ledger.dto.RefundRequest;
import com.vrtx.ledger.dto.TransactionResponse;
import com.vrtx.ledger.exception.InsufficientFundsException;
import com.vrtx.ledger.service.BalanceService;
import com.vrtx.ledger.service.LedgerService;
import com.vrtx.ledger.service.ReconciliationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end tests against a real PostgreSQL (Testcontainers) so the Flyway
 * schema, locking and SQL balance queries are all exercised. Requires Docker.
 */
@SpringBootTest
@Testcontainers
class LedgerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("vrtx_ledger")
            .withUsername("vrtx")
            .withPassword("vrtx");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MERCHANT = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired LedgerService ledgerService;
    @Autowired BalanceService balanceService;
    @Autowired ReconciliationService reconciliationService;

    @Test
    void paymentMovesMoneyAndStaysBalanced() {
        BigDecimal userBefore = balanceService.balanceOf(USER);

        TransactionResponse tx = ledgerService.pay(new PaymentRequest(
                USER, MERCHANT, new BigDecimal("100.00"), null, key("pay"), "test"));

        // 100 debit user, 97 credit merchant, 3 credit fees (3% default)
        assertThat(tx.amount()).isEqualByComparingTo("100.00");
        assertThat(tx.fee()).isEqualByComparingTo("3.00");
        assertThat(tx.entries()).hasSize(3);

        assertThat(balanceService.balanceOf(USER)).isEqualByComparingTo(userBefore.subtract(new BigDecimal("100.00")));
        assertThat(balanceService.balanceOf(MERCHANT)).isEqualByComparingTo("97.00");

        assertThat(reconciliationService.reconcile().balanced()).isTrue();
    }

    @Test
    void idempotentPaymentReturnsSameTransaction() {
        String key = key("idem");
        PaymentRequest req = new PaymentRequest(USER, MERCHANT, new BigDecimal("10.00"), null, key, null);

        TransactionResponse first = ledgerService.pay(req);
        TransactionResponse second = ledgerService.pay(req);

        assertThat(second.id()).isEqualTo(first.id());
    }

    @Test
    void partialRefundReversesProportionalFeeAndLinksToOriginal() {
        TransactionResponse payment = ledgerService.pay(new PaymentRequest(
                USER, MERCHANT, new BigDecimal("100.00"), null, key("pay"), null));

        TransactionResponse refund = ledgerService.refund(new RefundRequest(
                payment.id(), new BigDecimal("50.00"), key("refund"), null));

        assertThat(refund.relatedTransactionId()).isEqualTo(payment.id());
        assertThat(refund.amount()).isEqualByComparingTo("50.00");
        assertThat(refund.fee()).isEqualByComparingTo("1.50"); // half of the original 3.00
        assertThat(reconciliationService.reconcile().balanced()).isTrue();
    }

    @Test
    void overdraftIsRejectedForUserAccount() {
        assertThatThrownBy(() -> ledgerService.pay(new PaymentRequest(
                USER, MERCHANT, new BigDecimal("999999.00"), null, key("over"), null)))
                .isInstanceOf(InsufficientFundsException.class);
    }

    private String key(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
