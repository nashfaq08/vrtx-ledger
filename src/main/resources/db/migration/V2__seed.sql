INSERT INTO account (id, name, type, currency) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Demo User',        'USER',       'USD'),
    ('22222222-2222-2222-2222-222222222222', 'Demo Merchant',    'MERCHANT',   'USD'),
    ('33333333-3333-3333-3333-333333333333', 'Platform Fees',    'FEES',       'USD'),
    ('44444444-4444-4444-4444-444444444444', 'Bank Settlement',  'SETTLEMENT', 'USD');

INSERT INTO ledger_transaction (id, type, status, idempotency_key, currency, amount, fee, description)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ADJUSTMENT', 'POSTED',
        'seed-opening-balance-user', 'USD', 1000.0000, 0, 'Opening balance for demo user');

INSERT INTO ledger_entry (id, transaction_id, account_id, direction, amount, currency) VALUES
    ('a1111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     '11111111-1111-1111-1111-111111111111', 'CREDIT', 1000.0000, 'USD'),
    ('a2222222-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     '44444444-4444-4444-4444-444444444444', 'DEBIT',  1000.0000, 'USD');
