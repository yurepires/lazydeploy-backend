package com.yurepires.lazydeploy.service.validation;

import com.yurepires.lazydeploy.config.BusinessLimitProperties;
import com.yurepires.lazydeploy.exception.ChannelLimitReachedException;
import com.yurepires.lazydeploy.exception.RuleLimitReachedException;
import com.yurepires.lazydeploy.exception.SubscriptionLimitReachedException;
import com.yurepires.lazydeploy.service.observability.BusinessLimitMetrics;
import com.yurepires.lazydeploy.repository.ServerSubscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Centraliza somente os limites de quantidade do domínio.
 * Validações específicas de parâmetros continuam nos seus validators.
 */
@Service
public class BusinessLimitService {

    private final ServerSubscriptionRepository subscriptionRepository;
    private final BusinessLimitProperties properties;
    private final BusinessLimitMetrics metrics;

    public BusinessLimitService(
            ServerSubscriptionRepository subscriptionRepository,
            BusinessLimitProperties properties,
            BusinessLimitMetrics metrics
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.properties = properties;
        this.metrics = metrics;
    }

    public void ensureSubscriptionCapacity(UUID userId) {
        long currentCount = subscriptionRepository.countByUserId(userId);
        if (currentCount >= properties.subscriptionsPerUser()) {
            metrics.recordRejection("subscriptions");
            throw new SubscriptionLimitReachedException();
        }
    }

    public void ensureRuleCapacity(int currentRuleCount) {
        if (currentRuleCount >= properties.rulesPerSubscription()) {
            metrics.recordRejection("rules");
            throw new RuleLimitReachedException();
        }
    }

    public void ensureChannelCapacity(int currentChannelCount) {
        if (currentChannelCount >= properties.channelsPerSubscription()) {
            metrics.recordRejection("channels");
            throw new ChannelLimitReachedException();
        }
    }

    public void validateRuleCollectionSize(int requestedRuleCount) {
        if (requestedRuleCount > properties.rulesPerSubscription()) {
            metrics.recordRejection("rules");
            throw new RuleLimitReachedException();
        }
    }

    public void validateChannelCollectionSize(int requestedChannelCount) {
        if (requestedChannelCount > properties.channelsPerSubscription()) {
            metrics.recordRejection("channels");
            throw new ChannelLimitReachedException();
        }
    }

    public int maximumMapsPerMapInRule() {
        return properties.mapsPerMapInRule();
    }

    public void recordMapLimitRejection() {
        metrics.recordRejection("maps");
    }

    public BusinessLimitProperties properties() {
        return properties;
    }
}
