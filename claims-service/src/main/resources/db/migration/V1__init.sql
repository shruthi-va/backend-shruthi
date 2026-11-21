CREATE TABLE claim (
    id                  UUID          PRIMARY KEY,
    claim_number        VARCHAR(32)   NOT NULL UNIQUE,
    market              VARCHAR(8)    NOT NULL,
    channel             VARCHAR(16)   NOT NULL,
    policy_id           UUID          NOT NULL,
    policy_number       VARCHAR(64)   NOT NULL,
    owner_subject       VARCHAR(128),
    contact_id          UUID,
    claim_type          VARCHAR(16)   NOT NULL,
    date_of_loss        DATE          NOT NULL,
    filed_at            TIMESTAMP     NOT NULL,
    currency            VARCHAR(3)    NOT NULL,
    claimed_amount      DECIMAL(19,2) NOT NULL,
    status              VARCHAR(24)   NOT NULL,
    audit_flag          BOOLEAN       NOT NULL,
    proposed_reserve    DECIMAL(19,2),
    approved_reserve    DECIMAL(19,2),
    version             BIGINT        NOT NULL,
    updated_at          TIMESTAMP     NOT NULL
);

CREATE INDEX idx_claim_owner ON claim (owner_subject);
CREATE INDEX idx_claim_market_status ON claim (market, status);

CREATE TABLE claim_status_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_id        UUID          NOT NULL REFERENCES claim (id),
    from_status     VARCHAR(24),
    to_status       VARCHAR(24)   NOT NULL,
    actor_subject   VARCHAR(128)  NOT NULL,
    changed_at      TIMESTAMP     NOT NULL
);

CREATE INDEX idx_history_claim ON claim_status_history (claim_id, changed_at);

CREATE TABLE policy_projection (
    policy_id       UUID         PRIMARY KEY,
    policy_number   VARCHAR(64)  NOT NULL,
    market          VARCHAR(8)   NOT NULL,
    currency        VARCHAR(3)   NOT NULL,
    owner_subject   VARCHAR(128),
    status          VARCHAR(16)  NOT NULL,
    version         BIGINT       NOT NULL
);

CREATE TABLE processed_event (
    event_id        UUID         PRIMARY KEY,
    processed_at    TIMESTAMP    NOT NULL
);

CREATE TABLE outbox_event (
    id              UUID         PRIMARY KEY,
    topic           VARCHAR(128) NOT NULL,
    message_key     VARCHAR(128) NOT NULL,
    payload         CLOB         NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    published_at    TIMESTAMP
);

CREATE INDEX idx_outbox_unpublished ON outbox_event (published_at, created_at);
