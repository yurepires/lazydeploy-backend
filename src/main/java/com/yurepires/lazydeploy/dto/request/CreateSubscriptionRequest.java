package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CreateSubscriptionRequest(
        @NotBlank String serverGuid,
        @Size(max = 255) String displayName,
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
