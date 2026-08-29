ALTER TABLE notification_delivery_attempt
    ADD COLUMN owner_user_id UUID,
    ADD COLUMN server_id UUID,
    ADD COLUMN server_display_name VARCHAR(255),
    ADD COLUMN map_id VARCHAR(255),
    ADD COLUMN map_display_name VARCHAR(255),
    ADD COLUMN player_count INTEGER,
    ADD COLUMN max_players INTEGER,
    ADD COLUMN game_mode VARCHAR(128);

ALTER TABLE notification_delivery_attempt
    ALTER COLUMN subscription_id DROP NOT NULL;

ALTER TABLE notification_delivery_attempt
    DROP CONSTRAINT IF EXISTS notification_delivery_attempt_subscription_id_fkey;

ALTER TABLE notification_delivery_attempt
    ADD CONSTRAINT fk_delivery_attempt_subscription
        FOREIGN KEY (subscription_id)
        REFERENCES server_subscription(id)
        ON DELETE SET NULL;

CREATE INDEX idx_delivery_attempt_status_time
    ON notification_delivery_attempt(status, attempted_at DESC);

CREATE INDEX idx_delivery_attempt_channel_time
    ON notification_delivery_attempt(channel_type, attempted_at DESC);

CREATE INDEX idx_delivery_attempt_server_time
    ON notification_delivery_attempt(server_id, attempted_at DESC);

CREATE INDEX idx_delivery_attempt_map_time
    ON notification_delivery_attempt(map_id, attempted_at DESC);

CREATE INDEX idx_delivery_attempt_owner_time
    ON notification_delivery_attempt(owner_user_id, attempted_at DESC);
