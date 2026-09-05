package com.yurepires.lazydeploy.security.ratelimit;

import com.yurepires.lazydeploy.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Centraliza a criação das chaves usadas pelas políticas de rate limiting.
 * Nenhum desses valores é usado como tag de métrica ou escrito em logs.
 */
@Component
public class RateLimitKeyResolver {

    private final ClientIpResolver clientIpResolver;

    public RateLimitKeyResolver(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    public String resolveClientIp(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }

    public String resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            return null;
        }
        return authenticatedUser.userId().toString();
    }

    public String normalizeLoginEmail(String email) {
        if (email == null) {
            return null;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isBlank()) {
            return null;
        }
        return normalizedEmail;
    }
}
