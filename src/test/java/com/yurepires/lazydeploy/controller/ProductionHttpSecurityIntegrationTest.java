package com.yurepires.lazydeploy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProductionHttpSecurityIntegrationTest {

    private static final String PRODUCTION_FRONTEND_ORIGIN =
            "https://lazydeploy.pages.dev";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Test
    void shouldAllowOnlyConfiguredProductionOrigin() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Origin", PRODUCTION_FRONTEND_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        PRODUCTION_FRONTEND_ORIGIN
                ))
                .andExpect(header().string(
                        "Access-Control-Allow-Credentials",
                        "true"
                ));

        mockMvc.perform(get("/api/auth/me")
                        .header("Origin", "http://localhost:4200"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void shouldConfigureSecureCrossSiteCsrfCookieInProduction() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")
                        .secure(true)
                        .header("Origin", PRODUCTION_FRONTEND_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        PRODUCTION_FRONTEND_ORIGIN
                ))
                .andReturn();

        jakarta.servlet.http.Cookie csrfCookie = result.getResponse()
                .getCookie("XSRF-TOKEN");
        org.assertj.core.api.Assertions.assertThat(csrfCookie).isNotNull();
        org.assertj.core.api.Assertions.assertThat(csrfCookie.getAttribute("SameSite"))
                .isEqualTo("None");
        org.assertj.core.api.Assertions.assertThat(csrfCookie.getSecure()).isTrue();
        org.assertj.core.api.Assertions.assertThat(csrfCookie.isHttpOnly()).isFalse();
        org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getHeader("Set-Cookie")
                )
                .contains("Path=/");
    }

    @Test
    void shouldEnableHstsOnlyForSecureProductionResponses() throws Exception {
        mockMvc.perform(get("/api/auth/csrf")
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Strict-Transport-Security",
                        "max-age=31536000"
                ));

        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    void shouldUseSecureCrossSiteSessionCookiePolicyInProduction() {
        org.assertj.core.api.Assertions.assertThat(
                        environment.getProperty(
                                "server.servlet.session.cookie.secure",
                                Boolean.class
                        )
                )
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(
                        environment.getProperty(
                                "server.servlet.session.cookie.http-only",
                                Boolean.class
                        )
                )
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(
                        environment.getProperty(
                                "server.servlet.session.cookie.same-site"
                        )
                )
                .isEqualTo("none");
    }

    @Test
    void shouldHandleProductionPreflightWithoutAuthentication() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", PRODUCTION_FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header(
                                "Access-Control-Request-Headers",
                                "content-type,x-xsrf-token"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        PRODUCTION_FRONTEND_ORIGIN
                ))
                .andExpect(header().string(
                        "Access-Control-Allow-Credentials",
                        "true"
                ));
    }
}
