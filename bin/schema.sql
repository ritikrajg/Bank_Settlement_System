-- ============================================================
--  Bank Settlement Application — Full Database Schema
--  Database : bank_settlement (PostgreSQL 14+)
--
--  Setup:
--    createdb -U postgres bank_settlement
--    psql -U postgres -d bank_settlement -f schema.sql
-- ============================================================


-- ============================================================
--  TABLE : incoming_transactions
--  Stores raw transactions ingested from external source systems.
-- ============================================================
CREATE TABLE incoming_transactions (
    txn_id           VARCHAR(50)  PRIMARY KEY,

    source_system    VARCHAR(20)  NOT NULL,
    source_bank      VARCHAR(50)  NOT NULL,
    destination_bank VARCHAR(50)  NOT NULL,

    from_account     VARCHAR(50)  NOT NULL,
    to_account       VARCHAR(50)  NOT NULL,

    amount           NUMERIC(18,2) NOT NULL,

    status           VARCHAR(20)  NOT NULL,

    value_date       DATE,
    ingested_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
--  TABLE : settlement_batches
--  One row per settlement run (e.g. NEFT batch, RTGS batch).
-- ============================================================
CREATE TABLE settlement_batches (
    batch_id         VARCHAR(100) PRIMARY KEY,

    settlement_date  DATE         NOT NULL,
    status           VARCHAR(30)  NOT NULL,

    total_items      INT          NOT NULL DEFAULT 0,
    total_amount     NUMERIC(18,2) NOT NULL DEFAULT 0,

    run_by           VARCHAR(100) NOT NULL,

    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at     TIMESTAMP
);


-- ============================================================
--  TABLE : settlement_records
--  Individual settled (or failed) transaction records per batch.
-- ============================================================
CREATE TABLE settlement_records (
    record_id        BIGSERIAL    PRIMARY KEY,

    batch_id         VARCHAR(100),
    incoming_txn_id  VARCHAR(50)  NOT NULL,

    source_bank      VARCHAR(50)  NOT NULL,
    destination_bank VARCHAR(50)  NOT NULL,

    settled_amount   NUMERIC(18,2) NOT NULL,

    status           VARCHAR(30)  NOT NULL,
    failure_reason   TEXT,

    settled_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sr_batch FOREIGN KEY (batch_id)
        REFERENCES settlement_batches(batch_id),

    CONSTRAINT fk_sr_txn FOREIGN KEY (incoming_txn_id)
        REFERENCES incoming_transactions(txn_id)
);


-- ============================================================
--  TABLE : npci_accounts
--  In-DB mirror of each member bank's NPCI settlement account.
--  Seeded with zero balance; updated during settlement phase.
-- ============================================================
CREATE TABLE npci_accounts (
    bank_code   VARCHAR(20)   PRIMARY KEY,
    balance     NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency    VARCHAR(3)    NOT NULL DEFAULT 'INR',
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
--  TABLE : netting_positions
--  Stores each bank's gross-debit, gross-credit and net position
--  calculated during the Netting Phase (Phase 3).
--
--  FIX: Added UNIQUE (batch_id, bank_code) so that
--       NettingPositionDAO can safely use ON CONFLICT upserts,
--       making the netting phase idempotent / re-runnable.
-- ============================================================
CREATE TABLE netting_positions (
    position_id         BIGSERIAL     PRIMARY KEY,
    batch_id            VARCHAR(100)  NOT NULL,
    bank_code           VARCHAR(20)   NOT NULL,
    gross_debit_amount  NUMERIC(18,2) NOT NULL DEFAULT 0,
    gross_credit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    net_amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
    direction           VARCHAR(20)   NOT NULL,   -- NET_CREDIT | NET_DEBIT | FLAT
    currency            VARCHAR(3)    NOT NULL DEFAULT 'INR',
    position_date       DATE          NOT NULL,
    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,

    -- ✅ FIX: unique constraint required for ON CONFLICT upsert in NettingPositionDAO
    CONSTRAINT uq_netting_batch_bank
        UNIQUE (batch_id, bank_code),

    CONSTRAINT fk_netting_batch
        FOREIGN KEY (batch_id) REFERENCES settlement_batches(batch_id)
);


-- ============================================================
--  TABLE : settlement_instructions
--  Instructions raised after netting to move funds via NPCI.
-- ============================================================
CREATE TABLE settlement_instructions (
    instruction_id   VARCHAR(100)  PRIMARY KEY,
    batch_id         VARCHAR(100)  NOT NULL,
    bank_code        VARCHAR(20)   NOT NULL,
    instruction_type VARCHAR(30)   NOT NULL,
    amount           NUMERIC(18,2) NOT NULL,
    currency         VARCHAR(3)    NOT NULL DEFAULT 'INR',
    status           VARCHAR(20)   NOT NULL,
    value_date       DATE          NOT NULL,
    created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_instruction_batch
        FOREIGN KEY (batch_id) REFERENCES settlement_batches(batch_id)
);


-- ============================================================
--  TABLE : customer
-- ============================================================
CREATE TABLE customer (
    id               BIGSERIAL    PRIMARY KEY,

    first_name       VARCHAR(50)  NOT NULL,
    last_name        VARCHAR(50)  NOT NULL,

    email            VARCHAR(100) UNIQUE NOT NULL,

    kyc_status       VARCHAR(20)  NOT NULL,
    customer_tier    VARCHAR(20),

    onboarding_date  DATE,

    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
--  TABLE : account
-- ============================================================
CREATE TABLE account (
    id               BIGSERIAL    PRIMARY KEY,

    account_number   VARCHAR(20)  UNIQUE NOT NULL,
    account_type     VARCHAR(20)  NOT NULL,

    balance          NUMERIC(18,2) NOT NULL,

    currency         VARCHAR(3)   NOT NULL,

    customer_id      BIGINT,
    bank_id          VARCHAR(20),

    status           VARCHAR(20)  NOT NULL,

    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id)
        REFERENCES customer(id)
);


-- ============================================================
--  SEED DATA : npci_accounts
-- ============================================================
INSERT INTO npci_accounts (bank_code, balance, currency) VALUES
    ('SBI',    0.00, 'INR'),
    ('HDFC',   0.00, 'INR'),
    ('ICICI',  0.00, 'INR'),
    ('PNB',    0.00, 'INR'),
    ('AXIS',   0.00, 'INR'),
    ('BOB',    0.00, 'INR'),
    ('CANARA', 0.00, 'INR');


-- ============================================================
--  SEED DATA : customer
-- ============================================================
INSERT INTO customer (first_name, last_name, email, kyc_status, customer_tier, onboarding_date)
VALUES
    ('Ritik',  'Gupta',  'ritik@sbi.com',    'VERIFIED', 'RETAIL',    '2024-01-10'),
    ('Aman',   'Sharma', 'aman@hdfc.com',    'VERIFIED', 'PREMIUM',   '2023-11-05'),
    ('Neha',   'Verma',  'neha@icici.com',   'VERIFIED', 'RETAIL',    '2024-02-15'),
    ('Raj',    'Singh',  'raj@pnb.com',      'VERIFIED', 'CORPORATE', '2022-09-20'),
    ('Simran', 'Kaur',   'simran@axis.com',  'VERIFIED', 'RETAIL',    '2024-03-01'),
    ('Vikas',  'Yadav',  'vikas@bob.com',    'VERIFIED', 'RETAIL',    '2023-12-12'),
    ('Anjali', 'Nair',   'anjali@canara.com','VERIFIED', 'PREMIUM',   '2024-01-25');


-- ============================================================
--  SEED DATA : account
-- ============================================================
INSERT INTO account (account_number, account_type, balance, currency, customer_id, bank_id, status)
VALUES
    -- SBI (customer_id = 1)
    ('SBI1001', 'SAVINGS', 500000.00, 'INR', 1, 'SBI', 'ACTIVE'),
    ('SBI1002', 'SAVINGS', 200000.00, 'USD', 1, 'SBI', 'ACTIVE'),
    ('SBI1003', 'SAVINGS', 480000.00, 'INR', 1, 'SBI', 'ACTIVE'),
    ('SBI1004', 'SAVINGS', 210000.00, 'USD', 1, 'SBI', 'ACTIVE'),
    ('SBI1005', 'SAVINGS', 460000.00, 'INR', 1, 'SBI', 'ACTIVE'),
    ('SBI1006', 'SAVINGS', 220000.00, 'USD', 1, 'SBI', 'ACTIVE'),
    ('SBI1007', 'SAVINGS', 440000.00, 'INR', 1, 'SBI', 'ACTIVE'),

    -- HDFC (customer_id = 2)
    ('HDFC2001', 'SAVINGS', 400000.00, 'INR', 2, 'HDFC', 'ACTIVE'),
    ('HDFC2002', 'SAVINGS', 150000.00, 'USD', 2, 'HDFC', 'ACTIVE'),
    ('HDFC2003', 'SAVINGS', 420000.00, 'INR', 2, 'HDFC', 'ACTIVE'),
    ('HDFC2004', 'SAVINGS', 165000.00, 'USD', 2, 'HDFC', 'ACTIVE'),
    ('HDFC2005', 'SAVINGS', 430000.00, 'INR', 2, 'HDFC', 'ACTIVE'),
    ('HDFC2006', 'SAVINGS', 175000.00, 'USD', 2, 'HDFC', 'ACTIVE'),
    ('HDFC2007', 'SAVINGS', 440000.00, 'INR', 2, 'HDFC', 'ACTIVE'),
    ('HDFC2008', 'SAVINGS', 185000.00, 'USD', 2, 'HDFC', 'ACTIVE'),

    -- ICICI (customer_id = 3)
    ('ICICI3001', 'SAVINGS', 350000.00, 'INR', 3, 'ICICI', 'ACTIVE'),
    ('ICICI3002', 'SAVINGS', 120000.00, 'USD', 3, 'ICICI', 'ACTIVE'),
    ('ICICI3003', 'SAVINGS', 360000.00, 'INR', 3, 'ICICI', 'ACTIVE'),
    ('ICICI3004', 'SAVINGS', 130000.00, 'USD', 3, 'ICICI', 'ACTIVE'),
    ('ICICI3005', 'SAVINGS', 370000.00, 'INR', 3, 'ICICI', 'ACTIVE'),
    ('ICICI3006', 'SAVINGS', 140000.00, 'USD', 3, 'ICICI', 'ACTIVE'),
    ('ICICI3007', 'SAVINGS', 380000.00, 'INR', 3, 'ICICI', 'ACTIVE'),
    ('ICICI3008', 'SAVINGS', 150000.00, 'USD', 3, 'ICICI', 'ACTIVE'),

    -- PNB (customer_id = 4)
    ('PNB4001', 'CURRENT', 450000.00, 'INR', 4, 'PNB', 'ACTIVE'),
    ('PNB4002', 'CURRENT', 100000.00, 'USD', 4, 'PNB', 'ACTIVE'),
    ('PNB4003', 'CURRENT', 460000.00, 'INR', 4, 'PNB', 'ACTIVE'),
    ('PNB4004', 'CURRENT', 110000.00, 'USD', 4, 'PNB', 'ACTIVE'),
    ('PNB4005', 'CURRENT', 470000.00, 'INR', 4, 'PNB', 'ACTIVE'),
    ('PNB4006', 'CURRENT', 120000.00, 'USD', 4, 'PNB', 'ACTIVE'),
    ('PNB4007', 'CURRENT', 480000.00, 'INR', 4, 'PNB', 'ACTIVE'),

    -- AXIS (customer_id = 5)
    ('AXIS5001', 'SAVINGS', 380000.00, 'INR', 5, 'AXIS', 'ACTIVE'),
    ('AXIS5002', 'SAVINGS', 110000.00, 'USD', 5, 'AXIS', 'ACTIVE'),
    ('AXIS5003', 'SAVINGS', 390000.00, 'INR', 5, 'AXIS', 'ACTIVE'),
    ('AXIS5004', 'SAVINGS', 120000.00, 'USD', 5, 'AXIS', 'ACTIVE'),
    ('AXIS5005', 'SAVINGS', 400000.00, 'INR', 5, 'AXIS', 'ACTIVE'),
    ('AXIS5006', 'SAVINGS', 130000.00, 'USD', 5, 'AXIS', 'ACTIVE'),
    ('AXIS5007', 'SAVINGS', 410000.00, 'INR', 5, 'AXIS', 'ACTIVE'),
    ('AXIS5008', 'SAVINGS', 140000.00, 'USD', 5, 'AXIS', 'ACTIVE'),

    -- BOB (customer_id = 6)
    ('BOB6001', 'CURRENT', 420000.00, 'INR', 6, 'BOB', 'ACTIVE'),
    ('BOB6002', 'CURRENT',  90000.00, 'USD', 6, 'BOB', 'ACTIVE'),
    ('BOB6003', 'CURRENT', 430000.00, 'INR', 6, 'BOB', 'ACTIVE'),
    ('BOB6004', 'CURRENT', 100000.00, 'USD', 6, 'BOB', 'ACTIVE'),
    ('BOB6005', 'CURRENT', 440000.00, 'INR', 6, 'BOB', 'ACTIVE'),
    ('BOB6006', 'CURRENT', 110000.00, 'USD', 6, 'BOB', 'ACTIVE'),
    ('BOB6007', 'CURRENT', 450000.00, 'INR', 6, 'BOB', 'ACTIVE'),
    ('BOB6008', 'CURRENT', 120000.00, 'USD', 6, 'BOB', 'ACTIVE'),

    -- CANARA (customer_id = 7)
    ('CAN7001', 'SAVINGS', 390000.00, 'INR', 7, 'CANARA', 'ACTIVE'),
    ('CAN7002', 'SAVINGS', 115000.00, 'USD', 7, 'CANARA', 'ACTIVE'),
    ('CAN7003', 'SAVINGS', 400000.00, 'INR', 7, 'CANARA', 'ACTIVE'),
    ('CAN7004', 'SAVINGS', 125000.00, 'USD', 7, 'CANARA', 'ACTIVE'),
    ('CAN7005', 'SAVINGS', 410000.00, 'INR', 7, 'CANARA', 'ACTIVE'),
    ('CAN7006', 'SAVINGS', 135000.00, 'USD', 7, 'CANARA', 'ACTIVE'),
    ('CAN7007', 'SAVINGS', 420000.00, 'INR', 7, 'CANARA', 'ACTIVE');