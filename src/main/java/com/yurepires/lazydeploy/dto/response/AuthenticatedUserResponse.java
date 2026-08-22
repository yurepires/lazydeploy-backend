package com.yurepires.lazydeploy.dto.response;

import com.yurepires.lazydeploy.security.AuthenticatedUser;

import java.util.UUID;

public record AuthenticatedUserResponse(UUID id, String email) {

    public static AuthenticatedUserResponse from(AuthenticatedUser user) {
        return new AuthenticatedUserResponse(user.userId(), user.email());
    }
}
