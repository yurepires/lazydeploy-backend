package com.yurepires.lazydeploy.security.ratelimit;

public record RateLimitDecision(
        boolean allowed,
        long retryAfterSeconds,
        String policy
) {

    public RateLimitDecision {
        if (retryAfterSeconds < 0) {
            retryAfterSeconds = 0;
        }
        if (policy == null || policy.isBlank()) {
            policy = "unknown";
        }
    }
}
