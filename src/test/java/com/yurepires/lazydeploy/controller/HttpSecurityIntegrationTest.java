package com.yurepires.lazydeploy.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class HttpSecurityIntegrationTest {

    private static final String LOCAL_FRONTEND_ORIGIN = "http://localhost:4200";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Test
    void shouldAllowConfiguredDevelopmentOriginWithCredentials() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Origin", LOCAL_FRONTEND_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        LOCAL_FRONTEND_ORIGIN
                ))
                .andExpect(header().string(
                        "Access-Control-Allow-Credentials",
                        "true"
                ));
    }

    @Test
    void shouldNotAuthorizeUnknownOrigin() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Origin", "https://attacker.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void shouldHandleValidPreflightBeforeAuthentication() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", LOCAL_FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header(
                                "Access-Control-Request-Headers",
                                "content-type,x-xsrf-token"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        LOCAL_FRONTEND_ORIGIN
                ))
                .andExpect(header().string(
                        "Access-Control-Allow-Credentials",
                        "true"
                ))
                .andExpect(header().string(
                        "Access-Control-Allow-Methods",
                        containsString("POST")
                ))
                .andExpect(header().string(
                        "Access-Control-Max-Age",
                        "3600"
                ));
    }

    @Test
    void shouldRejectInvalidPreflightMethod() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", LOCAL_FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "TRACE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidPreflightHeader() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", LOCAL_FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "X-Unknown-Header"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotApplyApiCorsPolicyToActuator() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("Origin", LOCAL_FRONTEND_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void shouldIssueAngularCsrfCookieWithDevelopmentAttributes() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")
                        .header("Origin", LOCAL_FRONTEND_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        LOCAL_FRONTEND_ORIGIN
                ))
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        String setCookie = result.getResponse().getHeader("Set-Cookie");

        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getAttribute("SameSite"))
                .isEqualTo("Lax");
        assertThat(csrfCookie.isHttpOnly()).isFalse();
        assertThat(csrfCookie.getSecure()).isFalse();
        assertThat(setCookie).contains("Path=/");
    }

    @Test
    void shouldSetSecurityHeadersOnApiResponses() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string(
                        "Referrer-Policy",
                        "strict-origin-when-cross-origin"
                ))
                .andExpect(header().string(
                        "Content-Security-Policy",
                        "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"
                ))
                .andExpect(header().string(
                        "Permissions-Policy",
                        "camera=(), microphone=(), geolocation=()"
                ))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    void shouldUseDevelopmentSessionCookiePolicy() {
        assertThat(environment.getProperty(
                "server.servlet.session.cookie.secure",
                Boolean.class
        ))
                .isFalse();
        assertThat(environment.getProperty(
                "server.servlet.session.cookie.same-site"
        ))
                .isEqualTo("lax");
    }

    @Test
    void shouldReturnGenericProblemDetailForMissingCsrfToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"user@example.test\",\"password\":\"password\"}")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(jsonPath("$.detail").value("Não foi possível validar a requisição."))
                .andExpect(jsonPath("$.errorCode").value("CSRF_VALIDATION_FAILED"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void shouldReturnGenericProblemDetailForInvalidCsrfToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .cookie(new Cookie("XSRF-TOKEN", "expected-token"))
                                .header("X-XSRF-TOKEN", "invalid-token")
                                .contentType("application/json")
                                .content("{\"email\":\"user@example.test\",\"password\":\"password\"}")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(jsonPath("$.detail").value("Não foi possível validar a requisição."))
                .andExpect(jsonPath("$.errorCode").value("CSRF_VALIDATION_FAILED"));
    }
}
