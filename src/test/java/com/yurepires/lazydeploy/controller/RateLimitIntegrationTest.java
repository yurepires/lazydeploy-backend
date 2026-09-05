package com.yurepires.lazydeploy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "lazydeploy.security.rate-limit.login.ip.capacity=2",
        "lazydeploy.security.rate-limit.login.ip.window=10m",
        "lazydeploy.security.rate-limit.login.identity.capacity=1000",
        "lazydeploy.security.rate-limit.login.identity.window=10m",
        "lazydeploy.limits.max-request-body-bytes=100"
})
@AutoConfigureMockMvc
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnGeneric429ResponseWhenLoginIpLimitIsExceeded() throws Exception {
        String body = loginBody("unknown-ip-limit@example.test");

        performLogin(body, "198.51.100.10").andExpect(status().isUnauthorized());
        performLogin(body, "198.51.100.10").andExpect(status().isUnauthorized());

        performLogin(body, "198.51.100.10")
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(header().string("Retry-After", "600"))
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.detail").value(
                        "Muitas tentativas foram realizadas. Tente novamente mais tarde."
                ));
    }

    @Test
    void shouldRejectOversizedJsonBodyBeforeControllerExecution() throws Exception {
        String oversizedEmail = "a".repeat(150) + "@example.test";

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(oversizedEmail)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.errorCode").value("REQUEST_TOO_LARGE"));
    }

    private ResultActions performLogin(
            String body,
            String remoteAddress
    ) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr(remoteAddress);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String loginBody(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}";
    }
}
