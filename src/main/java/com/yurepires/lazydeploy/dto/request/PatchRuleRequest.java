package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.constraints.NotNull;

public record PatchRuleRequest(@NotNull Boolean enabled) {
}
