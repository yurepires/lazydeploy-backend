CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE server (
    id UUID PRIMARY KEY,
    display_name VARCHAR(255),
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE server_identifier (
    id UUID PRIMARY KEY,
    server_id UUID NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    provider VARCHAR(64) NOT NULL,
    identifier_type VARCHAR(64) NOT NULL,
    identifier_value VARCHAR(255) NOT NULL,
    CONSTRAINT uk_server_identifier UNIQUE (provider, identifier_type, identifier_value)
);

CREATE TABLE server_subscription (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    server_id UUID NOT NULL REFERENCES server(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_subscription_user_server UNIQUE (user_id, server_id)
);

CREATE INDEX idx_server_subscription_user ON server_subscription(user_id);
CREATE INDEX idx_server_subscription_server ON server_subscription(server_id);
