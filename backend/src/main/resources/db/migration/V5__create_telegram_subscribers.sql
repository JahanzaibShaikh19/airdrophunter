CREATE TABLE IF NOT EXISTS telegram_subscribers (
    id            BIGSERIAL PRIMARY KEY,
    chat_id       BIGINT NOT NULL UNIQUE,
    email         VARCHAR(255),
    is_subscribed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_telegram_subscribers_chat_id ON telegram_subscribers (chat_id);
CREATE INDEX IF NOT EXISTS idx_telegram_subscribers_is_subscribed ON telegram_subscribers (is_subscribed);

-- Add tracking columns to airdrops to prevent duplicate Telegram alerts
ALTER TABLE defi_airdrops ADD COLUMN IF NOT EXISTS notified_new_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE defi_airdrops ADD COLUMN IF NOT EXISTS notified_hot_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE defi_airdrops ADD COLUMN IF NOT EXISTS notified_deadline_at TIMESTAMP WITH TIME ZONE;
