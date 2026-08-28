CREATE TABLE battlefield_map (
    id VARCHAR(128) PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    expansion VARCHAR(128),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
