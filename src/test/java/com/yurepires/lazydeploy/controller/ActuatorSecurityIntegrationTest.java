package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.entity.UserEntity;
import com.yurepires.lazydeploy.entity.UserRole;
import com.yurepires.lazydeploy.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldExposeHealthWithoutAuthenticationAndHideDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void shouldProtectInfoAndMetricsEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectNonAdminUsersFromOperationalEndpoints() throws Exception {
        MockHttpSession session = loginAs(UserRole.USER);

        mockMvc.perform(get("/actuator/info").session(session))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/metrics").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminUsersToAccessOperationalEndpoints() throws Exception {
        MockHttpSession session = loginAs(UserRole.ADMIN);

        mockMvc.perform(get("/actuator/info").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void shouldExposeLivenessAndReadinessGroups() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void shouldNotExposeEnvironmentEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession loginAs(UserRole role) throws Exception {
        String email = "actuator-" + UUID.randomUUID() + "@example.com";
        String password = "password-123";
        Instant currentTime = Instant.now();
        UserEntity user = new UserEntity(
                null,
                email,
                passwordEncoder.encode(password),
                role,
                true,
                currentTime,
                currentTime
        );
        userRepository.saveAndFlush(user);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
