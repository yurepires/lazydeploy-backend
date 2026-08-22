CREATE TABLE round_instance (
    id UUID PRIMARY KEY,
    server_id UUID NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    map_id VARCHAR(255) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    first_observed_round_time BIGINT NOT NULL,
    ended_at TIMESTAMPTZ
);

CREATE TABLE server_state (
    server_id UUID PRIMARY KEY REFERENCES server(id) ON DELETE CASCADE,
    round_instance_id UUID NOT NULL REFERENCES round_instance(id) ON DELETE RESTRICT,
    map_id VARCHAR(255) NOT NULL,
    previous_round_time_seconds BIGINT NOT NULL,
    round_detected_at TIMESTAMPTZ NOT NULL,
    last_observed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE notification_state (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL REFERENCES server_subscription(id) ON DELETE CASCADE,
    round_instance_id UUID NOT NULL REFERENCES round_instance(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    last_attempt_at TIMESTAMPTZ,
    notified_at TIMESTAMPTZ,
    CONSTRAINT uk_notification_state_subscription_round UNIQUE (subscription_id, round_instance_id)
);

CREATE INDEX idx_round_instance_server_detected ON round_instance(server_id, detected_at DESC);
CREATE INDEX idx_notification_state_subscription_status ON notification_state(subscription_id, status);
