-- ============================================================================
-- VRTX Ledger - core schema (double-entry, append-only)
-- ============================================================================

CREATE TABLE account (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(32)  NOT NULL CHECK (type IN ('USER', 'MERCHANT', 'FEES', 'SETTLEMENT')),
    currency    CHAR(3)      NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- A transaction is the atomic unit. It groups two or more ledger entries that
-- must net to zero. `amount`/`fee` are denormalised summaries for reporting
-- and refund math; the ledger entries remain the single source of truth.
CREATE TABLE ledger_transaction (
    id                      UUID         PRIMARY KEY,
    type                    VARCHAR(32)  NOT NULL CHECK (type IN ('PAYMENT', 'REFUND', 'TRANSFER', 'SETTLEMENT', 'ADJUSTMENT')),
    status                  VARCHAR(32)  NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED', 'FAILED')),
    idempotency_key         VARCHAR(255) NOT NULL,
    currency                CHAR(3)      NOT NULL,
    amount                  NUMERIC(19,4) NOT NULL CHECK (amount >= 0),
    fee                     NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (fee >= 0),
    related_transaction_id  UUID         REFERENCES ledger_transaction(id),
    description             TEXT,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_idempotency_key UNIQUE (idempotency_key)
);

CREATE TABLE ledger_entry (
    id              UUID          PRIMARY KEY,
    transaction_id  UUID          NOT NULL REFERENCES ledger_transaction(id),
    account_id      UUID          NOT NULL REFERENCES account(id),
    direction       VARCHAR(8)    NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount          NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency        CHAR(3)       NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_entry_account       ON ledger_entry (account_id);
CREATE INDEX idx_entry_transaction   ON ledger_entry (transaction_id);
CREATE INDEX idx_txn_related         ON ledger_transaction (related_transaction_id);
CREATE INDEX idx_txn_type            ON ledger_transaction (type);

-- ----------------------------------------------------------------------------
-- Append-only enforcement at the database level.
-- The ledger may only ever be INSERTed into. Any UPDATE or DELETE is rejected,
-- guaranteeing immutability even if application code is bypassed.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION ledger_prevent_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Ledger is append-only: % is not permitted on %', TG_OP, TG_TABLE_NAME
        USING ERRCODE = 'restrict_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_entry_immutable
    BEFORE UPDATE OR DELETE ON ledger_entry
    FOR EACH ROW EXECUTE FUNCTION ledger_prevent_mutation();

CREATE TRIGGER trg_transaction_immutable
    BEFORE UPDATE OR DELETE ON ledger_transaction
    FOR EACH ROW EXECUTE FUNCTION ledger_prevent_mutation();
