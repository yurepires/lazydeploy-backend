package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record CreateSubscriptionRequest(
        @NotBlank String serverGuid,
        String displayName,
        Boolean enabled
) {

    public CreateSubscriptionRequest {
        if (serverGuid != null) {
            serverGuid = serverGuid.trim().toLowerCase(Locale.ROOT);
        }

        if (enabled == null) {
            enabled = Boolean.TRUE;
        }
    }
}
