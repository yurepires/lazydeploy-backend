package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.repository.ServerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AtomicSubscriptionConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServerRepository serverRepository;

    @Test
    void shouldCreateSubscriptionWithRulesAndChannelsAtomically() throws Exception {
        MockHttpSession session = login(uniqueEmail());

        mockMvc.perform(post("/api/bf4/subscriptions/configure")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configurationBody(UUID.randomUUID().toString(), true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.server.guid").isNotEmpty())
                .andExpect(jsonPath("$.rules.length()").value(2))
                .andExpect(jsonPath("$.rules[0].type").value("MAP_IN"))
                .andExpect(jsonPath("$.channels.length()").value(1))
                .andExpect(jsonPath("$.channels[0].type").value("EMAIL"));
    }

    @Test
    void shouldRejectDuplicateRulesBeforeCreatingServer() throws Exception {
        MockHttpSession session = login(uniqueEmail());
        long serverCountBefore = serverRepository.count();
        String serverGuid = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/bf4/subscriptions/configure")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configurationWithDuplicateRules(serverGuid)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RULE_TYPE"));

        assertThat(serverRepository.count()).isEqualTo(serverCountBefore);
    }

    @Test
    void shouldRejectConfigurationWithoutAnActiveChannelWhenEnabled() throws Exception {
        MockHttpSession session = login(uniqueEmail());

        mockMvc.perform(post("/api/bf4/subscriptions/configure")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configurationWithDisabledChannel()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("NO_ACTIVE_NOTIFICATION_CHANNEL"));
    }

    @Test
    void shouldRejectDuplicateChannelsWithUnprocessableEntity() throws Exception {
        MockHttpSession session = login(uniqueEmail());

        mockMvc.perform(post("/api/bf4/subscriptions/configure")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configurationWithDuplicateChannels()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_CHANNEL_TYPE"));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/bf4/subscriptions/configure")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configurationBody(UUID.randomUUID().toString(), true)))
                .andExpect(status().isUnauthorized());
    }

    private String configurationBody(String serverGuid, boolean enabled) {
        return """
                {
                  "serverGuid": "%s",
                  "displayName": "Configured server",
                  "enabled": %s,
                  "rules": [
                    {"type": "MAP_IN", "parameters": {"values": ["MP_Prison"]}},
                    {"type": "PLAYER_COUNT_AT_LEAST", "parameters": {"value": 40}}
                  ],
                  "channels": [
                    {"type": "EMAIL", "enabled": true}
                  ]
                }
                """.formatted(serverGuid, enabled);
    }

    private String configurationWithDuplicateRules(String serverGuid) {
        return """
                {
                  "serverGuid": "%s",
                  "rules": [
                    {"type": "MAP_IN", "parameters": {"values": ["MP_Prison"]}},
                    {"type": "map_in", "parameters": {"values": ["XP0_Metro"]}}
                  ],
                  "channels": [{"type": "EMAIL"}]
                }
                """.formatted(serverGuid);
    }

    private String configurationWithDisabledChannel() {
        return """
                {
                  "serverGuid": "%s",
                  "rules": [{"type": "MAP_IN", "parameters": {"values": ["MP_Prison"]}}],
                  "channels": [{"type": "EMAIL", "enabled": false}]
                }
                """.formatted(UUID.randomUUID());
    }

    private String configurationWithDuplicateChannels() {
        return """
                {
                  "serverGuid": "%s",
                  "rules": [{"type": "MAP_IN", "parameters": {"values": ["MP_Prison"]}}],
                  "channels": [
                    {"type": "EMAIL"},
                    {"type": "email"}
                  ]
                }
                """.formatted(UUID.randomUUID());
    }

    private MockHttpSession login(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-123\"}"))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String uniqueEmail() {
        return "configure-" + UUID.randomUUID() + "@example.test";
    }
}
