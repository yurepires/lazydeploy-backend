package com.yurepires.lazydeploy.exception;

/** Classifica falhas ocorridas durante uma chamada a um provider externo. */
public enum ExternalProviderFailureCategory {
    TIMEOUT,
    CONNECTION_ERROR,
    CLIENT_ERROR,
    SERVER_ERROR,
    INVALID_RESPONSE,
    RATE_LIMITED_BY_PROVIDER,
    CONCURRENCY_LIMIT_REACHED,
    UNKNOWN
}
