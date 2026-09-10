package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.config.ProviderProperties;
import com.yurepires.lazydeploy.exception.ExternalProviderException;
import com.yurepires.lazydeploy.integration.gametools.GameToolsServerDiscoveryProvider;
import com.yurepires.lazydeploy.security.SecurityTestMatrix;
import com.yurepires.lazydeploy.service.observability.ExternalProviderHealthTracker;
import com.yurepires.lazydeploy.service.observability.ExternalProviderMetrics;
import com.yurepires.lazydeploy.service.observability.GameToolsMetrics;
import com.yurepires.lazydeploy.service.observability.LogSanitizer;
import com.yurepires.lazydeploy.model.server.ServerSearchQuery;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        "lazydeploy.limits.max-request-body-bytes=256"
})
@AutoConfigureMockMvc
class SecuritySmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectUnauthenticatedProtectedRequest() throws Exception {
        mockMvc.perform(get("/api/bf4/subscriptions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void shouldRejectCrossUserResourceWithNotFound() throws Exception {
        MockHttpSession ownerSession = login(uniqueEmail());
        String subscriptionId = createSubscription(ownerSession);
        MockHttpSession otherUserSession = login(uniqueEmail());

        mockMvc.perform(get("/api/bf4/subscriptions/" + subscriptionId)
                        .session(otherUserSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldRejectMutationWithoutCsrf() throws Exception {
        MockHttpSession session = login(uniqueEmail());

        mockMvc.perform(post("/api/bf4/subscriptions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serverGuid\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("CSRF_VALIDATION_FAILED"));
    }

    @Test
    void shouldRejectUnknownCorsOriginWithoutReflectingIt() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Origin", "https://attacker.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void shouldReturn429AndRetryAfterWhenLoginIpLimitIsExceeded() throws Exception {
        String body = loginBody("smoke-rate-limit@example.test");
        String remoteAddress = "198.51.100.240";

        performLogin(body, remoteAddress).andExpect(status().isUnauthorized());
        performLogin(body, remoteAddress).andExpect(status().isUnauthorized());

        performLogin(body, remoteAddress)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "600"))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void shouldRejectOversizedPageSizeBeforeExecutingHistoryQuery() throws Exception {
        MockHttpSession session = login(uniqueEmail());

        mockMvc.perform(get("/api/bf4/notifications")
                        .session(session)
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PAGE_SIZE_LIMIT_EXCEEDED"));
    }

    @Test
    void shouldRejectOversizedJsonBodyBeforeControllerExecution() throws Exception {
        String oversizedEmail = "s".repeat(300) + "@example.test";

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(oversizedEmail)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.errorCode").value("REQUEST_TOO_LARGE"));
    }

    @Test
    void shouldKeepSensitiveActuatorEndpointUnavailable() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldConvertProviderTimeoutIntoControlledFailure() {
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> Mono.never())
                .build();
        ProviderProperties.ProviderSettings shortTimeout =
                new ProviderProperties.ProviderSettings(
                        25,
                        25,
                        1,
                        new ProviderProperties.RetrySettings(false, 1, 0)
                );
        ProviderProperties providerProperties = new ProviderProperties(
                shortTimeout,
                shortTimeout,
                shortTimeout
        );
        GameToolsServerDiscoveryProvider provider = new GameToolsServerDiscoveryProvider(
                client,
                GameToolsMetrics.noop(),
                new ExternalProviderHealthTracker(),
                ExternalProviderMetrics.noop(),
                null,
                providerProperties,
                new com.yurepires.lazydeploy.service.observability.SecurityEventLogger(
                        new LogSanitizer()
                )
        );

        assertThatThrownBy(() -> provider.search(new ServerSearchQuery("timeout", 1)))
                .isInstanceOf(ExternalProviderException.class)
                .satisfies(exception -> assertThat(((ExternalProviderException) exception).category())
                        .hasToString("TIMEOUT"));
    }

    @Test
    void shouldRemoveRepresentativeSecretsFromSanitizedLogs() {
        LogSanitizer sanitizer = new LogSanitizer();
        String sensitiveMessage = "password=plain-password token=csrf-token "
                + "authorization=Bearer-secret cookie=session-cookie "
                + "smtpSecret=smtp-password databasePassword=db-password";

        String sanitizedMessage = sanitizer.sanitize(sensitiveMessage);

        assertThat(sanitizedMessage)
                .doesNotContain(
                        "plain-password",
                        "csrf-token",
                        "Bearer-secret",
                        "session-cookie",
                        "smtp-password",
                        "db-password"
                );
    }

    @Test
    void shouldExposeTheSmokeChecksDocumentedByTheMatrix() {
        assertThat(SecurityTestMatrix.criticalSmokeChecks()).hasSize(10);
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

    private MockHttpSession login(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        String remoteAddress = "198.51.100." + (10 + Math.abs(email.hashCode() % 200));
        MvcResult result = performLogin(loginBody(email), remoteAddress)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String createSubscription(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/bf4/subscriptions")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serverGuid\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asString();
    }

    private String registerBody(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"password-123\"}";
    }

    private String loginBody(String email) {
        return registerBody(email);
    }

    private String uniqueEmail() {
        return "security-smoke-" + UUID.randomUUID() + "@example.test";
    }
}
