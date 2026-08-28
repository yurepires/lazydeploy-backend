package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.repository.ServerRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServerRepository serverRepository;

    @Test
    void shouldCompleteSubscriptionCrudAndPreserveSharedServer() throws Exception {
        MockHttpSession session = login(uniqueEmail());
        String serverGuid = UUID.randomUUID().toString();

        MvcResult creationResult = mockMvc.perform(post("/api/bf4/subscriptions")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionBody(serverGuid, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.server.guid").value(serverGuid))
                .andReturn();

        JsonNode createdSubscription = objectMapper.readTree(
                creationResult.getResponse().getContentAsString()
        );
        String subscriptionId = createdSubscription.get("id").asString();
        UUID serverId = UUID.fromString(createdSubscription.get("server").get("id").asString());

        mockMvc.perform(post("/api/bf4/subscriptions")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionBody(serverGuid, true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SUBSCRIPTION_ALREADY_EXISTS"));

        mockMvc.perform(patch("/api/bf4/subscriptions/" + subscriptionId)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(put("/api/bf4/subscriptions/" + subscriptionId)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(delete("/api/bf4/subscriptions/" + subscriptionId)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());

        assertThat(serverRepository.findById(serverId)).isPresent();
    }

    @Test
    void shouldCompleteRuleCrudAndValidateRuleDefinitions() throws Exception {
        MockHttpSession session = login(uniqueEmail());
        String subscriptionId = createSubscription(session);

        MvcResult creationResult = mockMvc.perform(post("/api/bf4/subscriptions/" + subscriptionId + "/rules")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "MAP_IN",
                                  "parameters": {"values": ["MP_Prison", "XP0_Metro"]}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("MAP_IN"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn();
        String ruleId = objectMapper.readTree(
                creationResult.getResponse().getContentAsString()
        ).get("id").asString();

        mockMvc.perform(post("/api/bf4/subscriptions/" + subscriptionId + "/rules")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"UNKNOWN\",\"parameters\":{}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_RULE_TYPE"));

        mockMvc.perform(post("/api/bf4/subscriptions/" + subscriptionId + "/rules")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"MAP_IN\",\"parameters\":{\"values\":[]}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RULE_PARAMETERS"));

        mockMvc.perform(patch("/api/bf4/subscriptions/" + subscriptionId + "/rules/" + ruleId)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(put("/api/bf4/subscriptions/" + subscriptionId + "/rules/" + ruleId)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PLAYER_COUNT_AT_LEAST",
                                  "enabled": true,
                                  "parameters": {"value": 40}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PLAYER_COUNT_AT_LEAST"));

        mockMvc.perform(delete("/api/bf4/subscriptions/" + subscriptionId + "/rules/" + ruleId)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/bf4/subscriptions/" + subscriptionId + "/rules")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldCompleteEmailChannelCrudAndValidateConfiguration() throws Exception {
        MockHttpSession session = login(uniqueEmail());
        String subscriptionId = createSubscription(session);

        MvcResult creationResult = mockMvc.perform(post("/api/bf4/subscriptions/" + subscriptionId + "/channels")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "EMAIL",
                                  "parameters": {"recipient": "first@example.com"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("EMAIL"))
                .andReturn();
        String channelId = objectMapper.readTree(
                creationResult.getResponse().getContentAsString()
        ).get("id").asString();

        mockMvc.perform(post("/api/bf4/subscriptions/" + subscriptionId + "/channels")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EMAIL\",\"parameters\":{\"recipient\":\"second@example.com\"}}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CHANNEL_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/bf4/subscriptions/" + subscriptionId + "/channels")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EMAIL\",\"parameters\":{\"recipient\":\"invalid\"}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CHANNEL_CONFIGURATION"));

        mockMvc.perform(patch("/api/bf4/subscriptions/" + subscriptionId + "/channels/" + channelId)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(put("/api/bf4/subscriptions/" + subscriptionId + "/channels/" + channelId)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EMAIL\",\"parameters\":{\"recipient\":\"updated@example.com\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parameters.recipient").value("updated@example.com"));

        mockMvc.perform(delete("/api/bf4/subscriptions/" + subscriptionId + "/channels/" + channelId)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/bf4/subscriptions/" + subscriptionId + "/channels")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldApplyOwnershipToRulesAndChannels() throws Exception {
        MockHttpSession ownerSession = login(uniqueEmail());
        String subscriptionId = createSubscription(ownerSession);
        String ruleId = createRule(ownerSession, subscriptionId);
        String channelId = createChannel(ownerSession, subscriptionId);
        MockHttpSession otherUserSession = login(uniqueEmail());

        mockMvc.perform(get("/api/bf4/subscriptions")
                        .session(otherUserSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/bf4/subscriptions/" + subscriptionId + "/rules/" + ruleId)
                        .session(otherUserSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/api/bf4/subscriptions/" + subscriptionId + "/channels/" + channelId)
                        .session(otherUserSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(delete("/api/bf4/subscriptions/" + subscriptionId)
                        .with(csrf())
                        .session(otherUserSession))
                .andExpect(status().isNotFound());
    }

    private String createSubscription(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/bf4/subscriptions")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionBody(UUID.randomUUID().toString(), true)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asString();
    }

    private String createRule(MockHttpSession session, String subscriptionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/bf4/subscriptions/" + subscriptionId + "/rules")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"MAP_IN\",\"parameters\":{\"values\":[\"MP_Prison\"]}}"))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asString();
    }

    private String createChannel(MockHttpSession session, String subscriptionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/bf4/subscriptions/" + subscriptionId + "/channels")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EMAIL\",\"parameters\":{\"recipient\":\"owner@example.com\"}}"))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asString();
    }

    private MockHttpSession login(String email) throws Exception {
        mockMvc.perform(post("/api/bf4/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-123\"}"))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/bf4/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String subscriptionBody(String serverGuid, boolean enabled) {
        return """
                {
                  "serverGuid": "%s",
                  "displayName": "Test server",
                  "enabled": %s
                }
                """.formatted(serverGuid, enabled);
    }

    private String uniqueEmail() {
        return "crud-" + UUID.randomUUID() + "@example.test";
    }
}
