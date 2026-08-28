package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.constraints.NotNull;

public record PatchSubscriptionRequest(@NotNull Boolean enabled) {
}
