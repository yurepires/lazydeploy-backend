package com.yurepires.lazydeploy.dto.response;

import com.yurepires.lazydeploy.model.persistence.ServerSubscription;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Representação da subscription após a configuração atômica.
 */
public record ConfiguredSubscriptionResponse(
        UUID id,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        ServerReferenceResponse server,
        List<RuleResponse> rules,
        List<ChannelResponse> channels
) {

    public ConfiguredSubscriptionResponse {
        if (rules == null) {
            rules = List.of();
        } else {
            rules = List.copyOf(rules);
        }

        if (channels == null) {
            channels = List.of();
        } else {
            channels = List.copyOf(channels);
        }
    }

    public static ConfiguredSubscriptionResponse from(ServerSubscription subscription) {
        List<RuleResponse> rules = subscription.rules().stream()
                .map(RuleResponse::from)
                .toList();
        List<ChannelResponse> channels = subscription.channels().stream()
                .map(ChannelResponse::from)
                .toList();

        return new ConfiguredSubscriptionResponse(
                subscription.id(),
                subscription.enabled(),
                subscription.createdAt(),
                subscription.updatedAt(),
                ServerReferenceResponse.from(subscription.server(), subscription.externalGuid()),
                rules,
                channels
        );
    }
}
