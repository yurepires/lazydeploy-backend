package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.entity.NotificationDeliveryAttemptEntity;
import com.yurepires.lazydeploy.entity.ServerEntity;
import com.yurepires.lazydeploy.entity.ServerIdentifierEntity;
import com.yurepires.lazydeploy.entity.ServerSubscriptionEntity;
import com.yurepires.lazydeploy.repository.NotificationDeliveryAttemptRepository;
import com.yurepires.lazydeploy.repository.ServerRepository;
import com.yurepires.lazydeploy.repository.ServerIdentifierRepository;
import com.yurepires.lazydeploy.repository.ServerSubscriptionRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private ServerSubscriptionRepository subscriptionRepository;

    @Autowired
    private ServerIdentifierRepository identifierRepository;

    @Autowired
    private NotificationDeliveryAttemptRepository attemptRepository;

    @Test
    void shouldListOwnedHistoryWithSnapshotAndFilter() throws Exception {
        String email = "history-" + UUID.randomUUID() + "@example.test";
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID userId = UUID.fromString(
                objectMapper.readTree(registration.getResponse().getContentAsString()).get("id").asString()
        );

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);

        UUID serverId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-28T18:00:00Z");
        serverRepository.save(new ServerEntity(serverId, "History server", true, now, now));
        identifierRepository.save(new ServerIdentifierEntity(
                UUID.randomUUID(),
                serverId,
                "BATTLELOG",
                "GUID",
                "history-guid-" + serverId
        ));
        subscriptionRepository.save(new ServerSubscriptionEntity(
                subscriptionId,
                userId,
                serverId,
                true,
                now,
                now
        ));
        UUID notificationId = UUID.randomUUID();
        attemptRepository.save(new NotificationDeliveryAttemptEntity(
                notificationId,
                subscriptionId,
                userId,
                UUID.randomUUID(),
                serverId,
                "History server",
                "MP_Prison",
                "Operation Locker",
                55,
                64,
                "Conquest",
                "EMAIL",
                "SUCCESS",
                now,
                now,
                null,
                null,
                Map.of()
        ));

        mockMvc.perform(get("/api/bf4/notifications")
                        .session(session)
                        .param("status", "SUCCESS")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(notificationId.toString()))
                .andExpect(jsonPath("$.content[0].server.id").value(serverId.toString()))
                .andExpect(jsonPath("$.content[0].map.displayName").value("Operation Locker"))
                .andExpect(jsonPath("$.content[0].players.current").value(55))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));

        mockMvc.perform(delete("/api/bf4/subscriptions/" + subscriptionId)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/bf4/notifications")
                        .session(session)
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(notificationId.toString()));
    }
}
