package com.yurepires.lazydeploy.exception;

public class RateLimitExceededException extends ApplicationException {

    private static final String DETAIL =
            "Muitas tentativas foram realizadas. Tente novamente mais tarde.";

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("RATE_LIMIT_EXCEEDED", DETAIL);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
