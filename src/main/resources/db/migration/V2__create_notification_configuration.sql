CREATE TABLE notification_rule (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL REFERENCES server_subscription(id) ON DELETE CASCADE,
    type VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE notification_rule_parameter (
    id UUID PRIMARY KEY,
    rule_id UUID NOT NULL REFERENCES notification_rule(id) ON DELETE CASCADE,
    parameter_key VARCHAR(128) NOT NULL,
    parameter_value TEXT NOT NULL,
    value_type VARCHAR(32) NOT NULL,
    CONSTRAINT uk_rule_parameter UNIQUE (rule_id, parameter_key)
);

CREATE TABLE notification_channel_configuration (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL REFERENCES server_subscription(id) ON DELETE CASCADE,
    type VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE notification_channel_parameter (
    id UUID PRIMARY KEY,
    channel_configuration_id UUID NOT NULL REFERENCES notification_channel_configuration(id) ON DELETE CASCADE,
    parameter_key VARCHAR(128) NOT NULL,
    parameter_value TEXT NOT NULL,
    value_type VARCHAR(32) NOT NULL,
    CONSTRAINT uk_channel_parameter UNIQUE (channel_configuration_id, parameter_key)
);

CREATE INDEX idx_notification_rule_subscription ON notification_rule(subscription_id);
CREATE INDEX idx_notification_channel_subscription ON notification_channel_configuration(subscription_id);
