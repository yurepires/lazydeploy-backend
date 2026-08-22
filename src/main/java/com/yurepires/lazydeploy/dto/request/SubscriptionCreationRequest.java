package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SubscriptionCreationRequest(
        @NotBlank String serverGuid,
        String displayName,
        List<@Valid NotificationRuleRequest> rules,
        List<@Valid NotificationChannelRequest> channels
) {
    public SubscriptionCreationRequest {
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
}
