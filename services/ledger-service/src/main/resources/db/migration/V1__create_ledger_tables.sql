CREATE TABLE accounts (
    id              UUID                     PRIMARY KEY,
    name            VARCHAR(100)             NOT NULL,
    normalized_name VARCHAR(100)             NOT NULL UNIQUE,
    balance         DECIMAL(15, 2)           NOT NULL,
    entry_count     INTEGER                  NOT NULL,
    opened_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE journal_entries (
    id               UUID                     PRIMARY KEY,
    event_id         UUID                     NOT NULL UNIQUE,
    transaction_id   UUID                     NOT NULL UNIQUE,
    transaction_date DATE                     NOT NULL,
    payer            VARCHAR(100)             NOT NULL,
    payee            VARCHAR(100)             NOT NULL,
    amount           DECIMAL(12, 2)           NOT NULL,
    source_bank      VARCHAR(100)             NOT NULL,
    kafka_partition  INTEGER                  NOT NULL,
    kafka_offset     BIGINT                   NOT NULL,
    posted_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE journal_lines (
    id               UUID           PRIMARY KEY,
    journal_entry_id UUID           NOT NULL REFERENCES journal_entries (id),
    account_id       UUID           NOT NULL REFERENCES accounts (id),
    amount           DECIMAL(12, 2) NOT NULL
);

CREATE INDEX idx_journal_lines_account_id ON journal_lines (account_id);

CREATE TABLE bank_summaries (
    id                UUID           PRIMARY KEY,
    bank_name         VARCHAR(100)   NOT NULL UNIQUE,
    total_outflow     DECIMAL(15, 2) NOT NULL,
    transaction_count INTEGER        NOT NULL
);
