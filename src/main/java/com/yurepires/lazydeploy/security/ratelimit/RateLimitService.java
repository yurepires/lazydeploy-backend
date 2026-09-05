package com.yurepires.lazydeploy.security.ratelimit;

import com.yurepires.lazydeploy.config.RateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Serviço de rate limiting em memória baseado em token bucket.
 *
 * <p>O cache separa as chaves por política, aplica expiração por inatividade e
 * limita a quantidade máxima de entradas para impedir crescimento ilimitado.</p>
 */
@Service
public class RateLimitService {

    private static final long TOKEN_COST = 1L;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> buckets;

    public RateLimitService(RateLimitProperties properties) {
        this.properties = properties;
        this.buckets = Caffeine.newBuilder()
                .maximumSize(properties.cache().maximumSize())
                .expireAfterAccess(properties.cache().expireAfterAccess())
                .build();
    }

    public RateLimitDecision tryConsume(
            String policyName,
            String key,
            RateLimitProperties.Policy policy
    ) {
        if (!properties.enabled()) {
            return new RateLimitDecision(true, 0, policyName);
        }

        String cacheKey = createCacheKey(policyName, key);
        Bucket bucket = buckets.get(cacheKey, ignored -> createBucket(policy));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(TOKEN_COST);

        if (probe.isConsumed()) {
            return new RateLimitDecision(true, 0, policyName);
        }

        return new RateLimitDecision(
                false,
                retryAfterSeconds(probe.getNanosToWaitForRefill()),
                policyName
        );
    }

    private Bucket createBucket(RateLimitProperties.Policy policy) {
        Duration refillPeriod = policy.window();
        Refill refill = Refill.intervally(policy.capacity(), refillPeriod);
        Bandwidth limit = Bandwidth.classic(policy.capacity(), refill);
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String createCacheKey(String policyName, String key) {
        String safePolicyName = safeValue(policyName);
        String safeKey = safeValue(key);
        return safePolicyName + "\u0000" + safeKey;
    }

    private String safeValue(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    private long retryAfterSeconds(long nanosToWaitForRefill) {
        if (nanosToWaitForRefill <= 0) {
            return 1;
        }

        long completeSeconds = nanosToWaitForRefill / NANOS_PER_SECOND;
        boolean hasPartialSecond = nanosToWaitForRefill % NANOS_PER_SECOND != 0;
        if (hasPartialSecond) {
            completeSeconds++;
        }
        return Math.max(1, completeSeconds);
    }
}
