package com.yurepires.lazydeploy.security;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityTestMatrixTest {

    @Test
    void shouldContainUniqueCriticalEndpointsWithAtLeastOneProtection() {
        Map<String, Set<SecurityTestMatrix.Protection>> matrix =
                SecurityTestMatrix.expectedProtections();

        assertThat(matrix).isNotEmpty();
        assertThat(matrix.keySet()).doesNotHaveDuplicates();
        assertThat(matrix.values())
                .allMatch(protections -> !protections.isEmpty());
    }

    @Test
    void shouldKeepReleaseBlockersInTheCentralMatrix() {
        Map<String, Set<SecurityTestMatrix.Protection>> matrix =
                SecurityTestMatrix.expectedProtections();

        assertThat(matrix.get("POST /api/auth/login"))
                .contains(
                        SecurityTestMatrix.Protection.CSRF,
                        SecurityTestMatrix.Protection.IP_RATE_LIMIT,
                        SecurityTestMatrix.Protection.IDENTITY_RATE_LIMIT,
                        SecurityTestMatrix.Protection.SESSION_FIXATION
                );
        assertThat(matrix.get("PATCH /api/bf4/subscriptions/{id}"))
                .contains(
                        SecurityTestMatrix.Protection.AUTHENTICATION,
                        SecurityTestMatrix.Protection.CSRF,
                        SecurityTestMatrix.Protection.OWNERSHIP
                );
        assertThat(matrix.get("GET /actuator/metrics"))
                .contains(
                        SecurityTestMatrix.Protection.AUTHENTICATION,
                        SecurityTestMatrix.Protection.AUTHORIZATION
                );
    }
}
