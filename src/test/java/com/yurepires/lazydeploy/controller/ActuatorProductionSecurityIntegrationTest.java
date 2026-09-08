package com.yurepires.lazydeploy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ActuatorProductionSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeOnlyAggregateHealthAndItsProbeGroups() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").doesNotExist());

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void shouldNotExposeInfoOrMetricsInProduction() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/info")
                        .with(adminUser()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/actuator/metrics")
                        .with(adminUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotExposeSensitiveEndpointsEvenInProduction() throws Exception {
        String[] sensitiveEndpoints = {
                "env",
                "configprops",
                "beans",
                "mappings",
                "heapdump",
                "threaddump",
                "loggers",
                "scheduledtasks",
                "caches",
                "httpexchanges",
                "startup",
                "shutdown"
        };

        for (String endpoint : sensitiveEndpoints) {
            mockMvc.perform(get("/actuator/" + endpoint)
                            .with(adminUser()))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void shouldKeepActuatorRootOutsideTheBusinessApi() throws Exception {
        mockMvc.perform(get("/api/actuator/health"))
                .andExpect(status().isNotFound());
    }

    private RequestPostProcessor adminUser() {
        return user("actuator-admin").roles("ADMIN");
    }
}
