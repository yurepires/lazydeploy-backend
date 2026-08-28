package com.yurepires.lazydeploy.service.auth;

import com.yurepires.lazydeploy.dto.response.AuthenticatedUserResponse;
import com.yurepires.lazydeploy.entity.UserEntity;
import com.yurepires.lazydeploy.exception.UserNotFoundException;
import com.yurepires.lazydeploy.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserProfileService {

    private final UserRepository userRepository;

    public CurrentUserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthenticatedUserResponse findById(UUID userId) {
        return userRepository.findById(userId)
                .filter(UserEntity::isEnabled)
                .map(user -> new AuthenticatedUserResponse(user.getId(), user.getEmail()))
                .orElseThrow(UserNotFoundException::new);
    }
}
