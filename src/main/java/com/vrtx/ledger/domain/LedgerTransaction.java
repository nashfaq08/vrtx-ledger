package com.vrtx.ledger.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Atomic, immutable unit of bookkeeping. Groups two or more {@link LedgerEntry}
 * rows whose debits and credits net to zero.
 */
@Entity
@Table(name = "ledger_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerTransaction {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private TransactionStatus status;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    /** Principal amount, denormalised for reporting / refund math. */
    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private BigDecimal fee;

    /** Links a REFUND back to the original PAYMENT (transaction linking). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_transaction_id", updatable = false)
    private LedgerTransaction relatedTransaction;

    @Column(updatable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LedgerEntry> entries = new ArrayList<>();

    public LedgerTransaction(TransactionType type, String idempotencyKey, String currency,
                             BigDecimal amount, BigDecimal fee,
                             LedgerTransaction relatedTransaction, String description) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.status = TransactionStatus.POSTED;
        this.idempotencyKey = idempotencyKey;
        this.currency = currency;
        this.amount = amount;
        this.fee = fee;
        this.relatedTransaction = relatedTransaction;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public void addEntry(LedgerEntry entry) {
        entry.attachTo(this);
        this.entries.add(entry);
    }

    public List<LedgerEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public UUID getRelatedTransactionId() {
        return relatedTransaction == null ? null : relatedTransaction.getId();
    }
}
