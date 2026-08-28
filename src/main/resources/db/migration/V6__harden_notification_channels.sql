ALTER TABLE notification_channel_configuration
    ADD COLUMN created_at TIMESTAMPTZ;

ALTER TABLE notification_channel_configuration
    ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE notification_channel_configuration
SET created_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE notification_channel_configuration
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE notification_channel_configuration
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE notification_channel_configuration
    ADD CONSTRAINT uk_channel_subscription_type UNIQUE (subscription_id, type);
