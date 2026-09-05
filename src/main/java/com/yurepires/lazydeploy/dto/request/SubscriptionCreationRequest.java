package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Locale;

public record SubscriptionCreationRequest(
        @NotBlank String serverGuid,
        @Size(max = 255) String displayName,
        List<@Valid NotificationRuleRequest> rules,
        List<@Valid NotificationChannelRequest> channels,
        Boolean enabled
) {
    public SubscriptionCreationRequest(
            String serverGuid,
            String displayName,
            List<NotificationRuleRequest> rules,
            List<NotificationChannelRequest> channels
    ) {
        this(serverGuid, displayName, rules, channels, Boolean.TRUE);
    }

    public SubscriptionCreationRequest {
        if (serverGuid != null) {
            serverGuid = serverGuid.trim().toLowerCase(Locale.ROOT);
        }

        if (enabled == null) {
            enabled = Boolean.TRUE;
        }

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
