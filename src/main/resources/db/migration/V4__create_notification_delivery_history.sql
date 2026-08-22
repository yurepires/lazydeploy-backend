CREATE TABLE notification_delivery_attempt (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL REFERENCES server_subscription(id) ON DELETE CASCADE,
    round_instance_id UUID NOT NULL REFERENCES round_instance(id) ON DELETE CASCADE,
    channel_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    error_code VARCHAR(128),
    error_message VARCHAR(1000),
    metadata JSONB
);

CREATE INDEX idx_delivery_attempt_subscription_time
    ON notification_delivery_attempt(subscription_id, attempted_at DESC);
