CREATE TABLE airdrops (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120)     NOT NULL,
    description TEXT             NOT NULL,
    token       VARCHAR(20)      NOT NULL,
    protocol    VARCHAR(80)      NOT NULL,
    chain       VARCHAR(40)      NOT NULL DEFAULT 'Ethereum',
    estimated_value NUMERIC(18, 2) NOT NULL DEFAULT 0,
    ends_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active   BOOLEAN          NOT NULL DEFAULT TRUE,
    website_url VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_airdrops_active ON airdrops (is_active);
CREATE INDEX idx_airdrops_ends_at ON airdrops (ends_at);
CREATE INDEX idx_airdrops_value  ON airdrops (estimated_value DESC);
