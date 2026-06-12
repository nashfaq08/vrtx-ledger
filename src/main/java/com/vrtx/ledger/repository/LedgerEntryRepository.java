package com.vrtx.ledger.repository;

import com.vrtx.ledger.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    /**
     * Ledger-based balance: SUM(credits) - SUM(debits) for an account.
     * Native query so the CASE is evaluated entirely in PostgreSQL.
     */
    @Query(value = """
            SELECT COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE -amount END), 0)
            FROM ledger_entry
            WHERE account_id = :accountId
            """, nativeQuery = true)
    BigDecimal computeBalance(@Param("accountId") UUID accountId);

    /** All entries for an account, oldest first (for the ledger statement view). */
    @Query("select e from LedgerEntry e where e.account.id = :accountId order by e.createdAt asc, e.id asc")
    List<LedgerEntry> findByAccountOrdered(@Param("accountId") UUID accountId);

    /** Grand total of all debits across the whole ledger (reconciliation). */
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM ledger_entry WHERE direction = 'DEBIT'", nativeQuery = true)
    BigDecimal totalDebits();

    /** Grand total of all credits across the whole ledger (reconciliation). */
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM ledger_entry WHERE direction = 'CREDIT'", nativeQuery = true)
    BigDecimal totalCredits();

    /**
     * Returns the ids of any transaction whose entries do NOT net to zero.
     * In a correct ledger this is always empty.
     */
    @Query(value = """
            SELECT transaction_id
            FROM ledger_entry
            GROUP BY transaction_id
            HAVING COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE -amount END), 0) <> 0
            """, nativeQuery = true)
    List<UUID> findUnbalancedTransactionIds();
}
