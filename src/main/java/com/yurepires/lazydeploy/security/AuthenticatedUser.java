package com.yurepires.lazydeploy.security;

import com.yurepires.lazydeploy.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class AuthenticatedUser implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final UserRole role;

    public AuthenticatedUser(
            UUID userId,
            String email,
            String passwordHash,
            boolean enabled
    ) {
        this(userId, email, passwordHash, enabled, UserRole.USER);
    }

    public AuthenticatedUser(
            UUID userId,
            String email,
            String passwordHash,
            boolean enabled,
            UserRole role
    ) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        if (role == null) {
            this.role = UserRole.USER;
        } else {
            this.role = role;
        }
    }

    public UUID userId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public String email() {
        return email;
    }

    public UserRole role() {
        return role;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
