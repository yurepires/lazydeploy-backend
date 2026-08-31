package com.yurepires.lazydeploy.security;

import com.yurepires.lazydeploy.entity.UserEntity;
import com.yurepires.lazydeploy.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LazyDeployUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmailNormalizer emailNormalizer;

    public LazyDeployUserDetailsService(
            UserRepository userRepository,
            EmailNormalizer emailNormalizer
    ) {
        this.userRepository = userRepository;
        this.emailNormalizer = emailNormalizer;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String normalizedEmail = emailNormalizer.normalize(username);
        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));

        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                user.getRole()
        );
    }
}
