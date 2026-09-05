package com.yurepires.lazydeploy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "lazydeploy.security.rate-limit.login.ip.capacity=1000",
        "lazydeploy.security.rate-limit.login.ip.window=10m",
        "lazydeploy.security.rate-limit.login.identity.capacity=2",
        "lazydeploy.security.rate-limit.login.identity.window=10m"
})
@AutoConfigureMockMvc
class RateLimitIdentityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldApplyIdentityLimitToNormalizedEmailAcrossDifferentIps() throws Exception {
        String firstBody = loginBody(" Identity-Limit@Example.Test ");
        String secondBody = loginBody("identity-limit@example.test");

        performLogin(firstBody, "198.51.100.20").andExpect(status().isUnauthorized());
        performLogin(secondBody, "198.51.100.21").andExpect(status().isUnauthorized());

        performLogin(firstBody, "198.51.100.22")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "600"));
    }

    private ResultActions performLogin(String body, String remoteAddress) throws Exception {
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
