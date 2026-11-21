CREATE TABLE policy (
    id              UUID         PRIMARY KEY,
    policy_number   VARCHAR(64)  NOT NULL UNIQUE,
    market          VARCHAR(8)   NOT NULL,
    product_type    VARCHAR(16)  NOT NULL,
    currency        VARCHAR(3)   NOT NULL,
    owner_subject   VARCHAR(128),
    status          VARCHAR(16)  NOT NULL,
    version         BIGINT       NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);

CREATE TABLE claimant_contact (
    id              UUID         PRIMARY KEY,
    contact_id      UUID         NOT NULL UNIQUE,
    owner_subject   VARCHAR(128) UNIQUE,
    market          VARCHAR(8)   NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    email           VARCHAR(254),
    phone           VARCHAR(32),
    email_opt_in    BOOLEAN      NOT NULL,
    sms_opt_in      BOOLEAN      NOT NULL,
    version         BIGINT       NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
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
