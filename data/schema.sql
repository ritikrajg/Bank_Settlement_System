CREATE TABLE incoming_transactions (
    txn_id BIGSERIAL PRIMARY KEY,
    source_ref VARCHAR(100) NOT NULL,
    source_system VARCHAR(20) NOT NULL,
    source_bank VARCHAR(50) NOT NULL,
    destination_bank VARCHAR(50) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    txn_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    value_date DATE NOT NULL,
    ingested_at TIMESTAMP NOT NULL,
    raw_payload TEXT
);

CREATE TABLE settlement_batches (
    batch_id BIGSERIAL PRIMARY KEY,
    batch_ref VARCHAR(100) NOT NULL UNIQUE,
    settlement_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_items INT NOT NULL DEFAULT 0,
    total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    run_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE TABLE settlement_records (
    record_id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES settlement_batches(batch_id),
    incoming_txn_id BIGINT NOT NULL REFERENCES incoming_transactions(txn_id),
    source_bank VARCHAR(50) NOT NULL,
    destination_bank VARCHAR(50) NOT NULL,
    settled_amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason TEXT,
    settled_at TIMESTAMP NOT NULL
);

CREATE TABLE netting_positions (
    position_id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES settlement_batches(batch_id),
    bank_code VARCHAR(50) NOT NULL,
    position_date DATE NOT NULL,
    items_delivered INT NOT NULL DEFAULT 0,
    items_received INT NOT NULL DEFAULT 0,
    amount_to_receive NUMERIC(18,2) NOT NULL DEFAULT 0,
    amount_to_pay NUMERIC(18,2) NOT NULL DEFAULT 0,
    net_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    direction VARCHAR(10) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    UNIQUE (batch_id, bank_code)
);