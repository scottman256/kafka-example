CREATE TABLE transactions (
    id               UUID                     PRIMARY KEY,
    transaction_date DATE                     NOT NULL,
    payer            VARCHAR(100)             NOT NULL,
    payee            VARCHAR(100)             NOT NULL,
    amount           DECIMAL(12, 2)           NOT NULL,
    currency         VARCHAR(3)               NOT NULL,
    memo             VARCHAR(255),
    source_bank      VARCHAR(100)             NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_transactions_created_at ON transactions (created_at);

CREATE TABLE kafka_publish_log (
    id              UUID                     PRIMARY KEY,
    event_id        UUID                     NOT NULL UNIQUE,
    transaction_id  UUID                     NOT NULL UNIQUE REFERENCES transactions (id),
    topic           VARCHAR(255)             NOT NULL,
    message_key     VARCHAR(100)             NOT NULL,
    payload         VARCHAR(4000)            NOT NULL,
    status          VARCHAR(20)              NOT NULL,
    kafka_partition INTEGER,
    kafka_offset    BIGINT,
    kafka_timestamp TIMESTAMP WITH TIME ZONE,
    error_message   VARCHAR(1000),
    logged_at       TIMESTAMP WITH TIME ZONE NOT NULL
);
