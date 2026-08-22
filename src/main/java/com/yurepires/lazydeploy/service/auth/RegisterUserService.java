package com.yurepires.lazydeploy.service.auth;

import com.yurepires.lazydeploy.dto.request.RegisterUserRequest;
import com.yurepires.lazydeploy.dto.response.AuthenticatedUserResponse;
import com.yurepires.lazydeploy.entity.UserEntity;
import com.yurepires.lazydeploy.exception.EmailAlreadyRegisteredException;
import com.yurepires.lazydeploy.repository.UserRepository;
import com.yurepires.lazydeploy.security.EmailNormalizer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class RegisterUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNormalizer emailNormalizer;
    private final Clock clock;

    public RegisterUserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailNormalizer emailNormalizer,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNormalizer = emailNormalizer;
        this.clock = clock;
    }

    public AuthenticatedUserResponse register(RegisterUserRequest request) {
        String normalizedEmail = emailNormalizer.normalize(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        Instant currentTime = Instant.now(clock);
        UserEntity user = new UserEntity(
                null,
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                true,
                currentTime,
                currentTime
        );

        try {
            UserEntity savedUser = userRepository.saveAndFlush(user);
            return new AuthenticatedUserResponse(savedUser.getId(), savedUser.getEmail());
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }
    }
}
