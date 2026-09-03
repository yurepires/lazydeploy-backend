package com.yurepires.lazydeploy.dto.response;

import com.yurepires.lazydeploy.model.persistence.ServerSubscription;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        ServerReferenceResponse server,
        List<RuleResponse> rules,
        List<ChannelResponse> channels,
        CurrentServerStatusResponse currentStatus
) {

    public SubscriptionResponse(
            UUID id,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt,
            ServerReferenceResponse server,
            List<RuleResponse> rules,
            List<ChannelResponse> channels
    ) {
        this(
                id,
                enabled,
                createdAt,
                updatedAt,
                server,
                rules,
                channels,
                unavailableStatus()
        );
    }

    public SubscriptionResponse {
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

        if (currentStatus == null) {
            currentStatus = unavailableStatus();
        }
    }

    public static SubscriptionResponse from(ServerSubscription subscription) {
        return from(
                subscription,
                unavailableStatus()
        );
    }

    public static SubscriptionResponse from(
            ServerSubscription subscription,
            CurrentServerStatusResponse currentStatus
    ) {
        List<RuleResponse> rules = subscription.rules().stream()
                .map(RuleResponse::from)
                .toList();
        List<ChannelResponse> channels = subscription.channels().stream()
                .map(ChannelResponse::from)
                .toList();

        return new SubscriptionResponse(
                subscription.id(),
                subscription.enabled(),
                subscription.createdAt(),
                subscription.updatedAt(),
                ServerReferenceResponse.from(subscription.server(), subscription.externalGuid()),
                rules,
                channels,
                currentStatus
        );
    }

    private static CurrentServerStatusResponse unavailableStatus() {
        return new CurrentServerStatusResponse(
                false,
                "NOT_OBSERVED_YET",
                null,
                null,
                null,
                null,
                null
        );
    }
}
