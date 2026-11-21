CREATE TABLE claim_snapshot (
    claim_id        UUID          PRIMARY KEY,
    claim_number    VARCHAR(32)   NOT NULL,
    market          VARCHAR(8)    NOT NULL,
    status          VARCHAR(24)   NOT NULL,
    claim_type      VARCHAR(16)   NOT NULL,
    claimed_amount  DECIMAL(19,2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL,
    audit_flag      BOOLEAN       NOT NULL,
    updated_at      TIMESTAMP     NOT NULL
);

CREATE INDEX idx_snapshot_market_status ON claim_snapshot (market, status);

CREATE TABLE processed_event (
    event_id        UUID          PRIMARY KEY,
    processed_at    TIMESTAMP     NOT NULL
);
