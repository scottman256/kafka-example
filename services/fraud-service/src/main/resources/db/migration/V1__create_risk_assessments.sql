CREATE TABLE risk_assessments (
    id               UUID                     PRIMARY KEY,
    event_id         UUID                     NOT NULL UNIQUE,
    transaction_id   UUID                     NOT NULL UNIQUE,
    payer            VARCHAR(100)             NOT NULL,
    payee            VARCHAR(100)             NOT NULL,
    normalized_payer VARCHAR(100)             NOT NULL,
    normalized_payee VARCHAR(100)             NOT NULL,
    amount           DECIMAL(12, 2)           NOT NULL,
    occurred_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    risk_score       INTEGER                  NOT NULL,
    risk_level       VARCHAR(10)              NOT NULL,
    kafka_partition  INTEGER                  NOT NULL,
    kafka_offset     BIGINT                   NOT NULL,
    assessed_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_risk_assessments_payer_occurred_at ON risk_assessments (normalized_payer, occurred_at);
CREATE INDEX idx_risk_assessments_payer_payee ON risk_assessments (normalized_payer, normalized_payee);
CREATE INDEX idx_risk_assessments_assessed_at ON risk_assessments (assessed_at);

CREATE TABLE triggered_rules (
    risk_assessment_id UUID        NOT NULL REFERENCES risk_assessments (id),
    rule_code          VARCHAR(30) NOT NULL,
    PRIMARY KEY (risk_assessment_id, rule_code)
);
