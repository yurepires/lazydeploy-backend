package com.yurepires.lazydeploy.service.observability;

/** Tipos controlados de eventos de segurança e abuso. */
public enum SecurityEventType {
    AUTH_LOGIN_SUCCESS,
    AUTH_LOGIN_FAILURE,
    AUTH_RATE_LIMITED,
    REGISTER_RATE_LIMITED,
    CSRF_REJECTED,
    CORS_REJECTED,
    RATE_LIMIT_REJECTED,
    BUSINESS_LIMIT_REJECTED,
    UNAUTHORIZED_REQUEST,
    ACCESS_DENIED,
    RESOURCE_NOT_OWNED,
    PROVIDER_FAILURE,
    RESOURCE_SATURATION
}
