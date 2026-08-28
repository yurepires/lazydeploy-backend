package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateSubscriptionRequest(
        @NotNull Boolean enabled
) {
}
