CREATE TABLE IF NOT EXISTS defi_airdrops (
    id                  BIGSERIAL           PRIMARY KEY,
    name                VARCHAR(160)        NOT NULL,
    symbol              VARCHAR(20)         NOT NULL,
    logo_url            VARCHAR(512),
    estimated_value_min NUMERIC(20, 2)      NOT NULL DEFAULT 0,
    estimated_value_max NUMERIC(20, 2)      NOT NULL DEFAULT 0,
    status              VARCHAR(10)         NOT NULL DEFAULT 'LIVE'
                            CHECK (status IN ('LIVE','SOON','ENDED')),
    category            VARCHAR(20)         NOT NULL DEFAULT 'OTHER'
                            CHECK (category IN ('L2','DEFI','BRIDGE','AI','OTHER')),
    deadline            TIMESTAMP WITH TIME ZONE,
    steps               TEXT                NOT NULL DEFAULT '',
    is_hot              BOOLEAN             NOT NULL DEFAULT FALSE,
    is_pro              BOOLEAN             NOT NULL DEFAULT FALSE,
    llama_slug          VARCHAR(200)        UNIQUE,
    last_refreshed_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_defi_airdrops_status   ON defi_airdrops (status);
CREATE INDEX IF NOT EXISTS idx_defi_airdrops_category ON defi_airdrops (category);
CREATE INDEX IF NOT EXISTS idx_defi_airdrops_is_hot   ON defi_airdrops (is_hot);
CREATE INDEX IF NOT EXISTS idx_defi_airdrops_deadline ON defi_airdrops (deadline);
