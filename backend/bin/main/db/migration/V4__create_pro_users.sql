CREATE TABLE IF NOT EXISTS pro_users (
    id              BIGSERIAL           PRIMARY KEY,
    email           VARCHAR(255)        NOT NULL UNIQUE,
    license_key     VARCHAR(128)        NOT NULL UNIQUE,
    is_active       BOOLEAN             NOT NULL DEFAULT TRUE,
    activated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    gumroad_sale_id VARCHAR(128),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pro_users_email       ON pro_users (email);
CREATE INDEX IF NOT EXISTS idx_pro_users_license_key ON pro_users (license_key);
CREATE INDEX IF NOT EXISTS idx_pro_users_is_active   ON pro_users (is_active);
