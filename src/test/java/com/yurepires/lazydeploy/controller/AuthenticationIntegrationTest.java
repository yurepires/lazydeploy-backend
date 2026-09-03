package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.entity.UserEntity;
import com.yurepires.lazydeploy.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterUserAtApiAuthRegister() throws Exception {
        String rawPassword = "correct horse battery";
        String email = uniqueEmail();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(" User@Example.COM ", rawPassword)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.com"));

        UserEntity user = userRepository.findByEmail("user@example.com").orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(user.getPasswordHash()).startsWith("{bcrypt}");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void shouldAcceptAngularCsrfCookieAndHeaderPair() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        String csrfToken = csrfResult.getResponse().getContentAsString();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        assertThat(csrfToken).isNotBlank();
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getValue()).isEqualTo(csrfToken);

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueEmail(), "password-123")))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        register(email, "password-123");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email.toUpperCase(), "password-456")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void shouldRejectInvalidRegistrationPayload() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldLoginUserAtApiAuthLoginAndReturnCurrentUserFromSession() throws Exception {
        String email = uniqueEmail();
        register(email, "password-123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "password-123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult
                .getRequest()
                .getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void shouldRejectInvalidCredentialsWithGenericResponse() throws Exception {
        String email = uniqueEmail();
        register(email, "password-123");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("Email ou senha inválidos"));
    }

    @Test
    void shouldRejectUnknownEmailWithTheSameGenericResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(uniqueEmail(), "password-123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("Email ou senha inválidos"));
    }

    @Test
    void shouldRejectUnauthenticatedAccessToApiAuthMe() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void shouldLogoutUserAtApiAuthLogoutAndInvalidateSession() throws Exception {
        String email = uniqueEmail();
        register(email, "password-123");
        MockHttpSession session = login(email, "password-123");

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void shouldRejectLogoutWithoutAnAuthenticatedSession() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void shouldReturn404ForOldRegisterEndpoint() throws Exception {
        mockMvc.perform(post("/api/bf4/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueEmail(), "password-123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404ForOldLoginEndpoint() throws Exception {
        mockMvc.perform(post("/api/bf4/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(uniqueEmail(), "password-123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404ForOldLogoutEndpoint() throws Exception {
        mockMvc.perform(post("/api/bf4/auth/logout")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404ForOldMeEndpoint() throws Exception {
        mockMvc.perform(get("/api/bf4/auth/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotAllowUserToReadAnotherUsersSubscription() throws Exception {
        String firstEmail = uniqueEmail();
        String secondEmail = uniqueEmail();
        register(firstEmail, "password-123");
        register(secondEmail, "password-123");

        MockHttpSession firstSession = login(firstEmail, "password-123");
        MvcResult creationResult = mockMvc.perform(post("/api/bf4/subscriptions")
                        .with(csrf())
                        .session(firstSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serverGuid": "%s",
                                  "displayName": "Shared server",
                                  "rules": [],
                                  "channels": []
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();

        String subscriptionId = objectMapper.readTree(
                creationResult.getResponse().getContentAsString()
        ).get("id").asString();
        MockHttpSession secondSession = login(secondEmail, "password-123");

        mockMvc.perform(get("/api/bf4/subscriptions/" + subscriptionId)
                        .session(secondSession))
                .andExpect(status().isNotFound());
    }

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, password)))
                .andExpect(status().isCreated());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String registerBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private String loginBody(String email, String password) {
        return registerBody(email, password);
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.test";
    }
}
