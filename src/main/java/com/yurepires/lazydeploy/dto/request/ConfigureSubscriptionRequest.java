package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Locale;

/**
 * Configuração completa de uma subscription em uma única operação.
 */
public record ConfigureSubscriptionRequest(
        @NotBlank String serverGuid,
        @Size(max = 255) String displayName,
        Boolean enabled,
        @NotNull List<@Valid ConfigureRuleRequest> rules,
        @NotNull List<@Valid ConfigureChannelRequest> channels
) {

    public ConfigureSubscriptionRequest {
        if (serverGuid != null) {
            serverGuid = serverGuid.trim().toLowerCase(Locale.ROOT);
        }

        if (enabled == null) {
            enabled = Boolean.TRUE;
        }

        if (rules != null) {
            rules = List.copyOf(rules);
        }

        if (channels != null) {
            channels = List.copyOf(channels);
        }
    }
}
