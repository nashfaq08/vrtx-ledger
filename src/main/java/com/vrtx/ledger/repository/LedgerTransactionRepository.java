package com.vrtx.ledger.repository;

import com.vrtx.ledger.domain.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {

    /** Idempotency lookup: a key uniquely identifies one logical operation. */
    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);

    /** Idempotency lookup that eagerly fetches entries for response mapping. */
    @Query("select distinct t from LedgerTransaction t left join fetch t.entries where t.idempotencyKey = :key")
    Optional<LedgerTransaction> findByIdempotencyKeyWithEntries(@Param("key") String key);

    /** Fetch a transaction with its entries initialised (for the GET endpoint). */
    @Query("select distinct t from LedgerTransaction t left join fetch t.entries where t.id = :id")
    Optional<LedgerTransaction> findByIdWithEntries(@Param("id") UUID id);

    /**
     * Sum of principal already refunded against an original transaction.
     * Used to prevent over-refunding (refund linking + correctness).
     */
    @Query("""
            select coalesce(sum(t.amount), 0)
            from LedgerTransaction t
            where t.relatedTransaction.id = :originalId and t.type = com.vrtx.ledger.domain.TransactionType.REFUND
            """)
    BigDecimal sumRefundedAmount(@Param("originalId") UUID originalId);
}
