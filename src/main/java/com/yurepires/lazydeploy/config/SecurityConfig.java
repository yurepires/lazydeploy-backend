package com.yurepires.lazydeploy.config;

import com.yurepires.lazydeploy.security.SecurityErrorResponseHandler;
import com.yurepires.lazydeploy.security.ratelimit.RateLimitFilter;
import com.yurepires.lazydeploy.security.ratelimit.RateLimitKeyResolver;
import com.yurepires.lazydeploy.security.ratelimit.RateLimitResponseWriter;
import com.yurepires.lazydeploy.security.ratelimit.RateLimitService;
import com.yurepires.lazydeploy.security.request.RequestBodyLimitFilter;
import com.yurepires.lazydeploy.security.request.RequestBodyLimitResponseWriter;
import com.yurepires.lazydeploy.service.observability.BusinessLimitMetrics;
import com.yurepires.lazydeploy.service.observability.SecurityMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    @Bean
    public RateLimitFilter rateLimitFilter(
            RateLimitProperties properties,
            RateLimitService rateLimitService,
            RateLimitKeyResolver keyResolver,
            SecurityMetrics securityMetrics,
            RateLimitResponseWriter responseWriter,
            ObjectMapper objectMapper
    ) {
        return new RateLimitFilter(
                properties,
                rateLimitService,
                keyResolver,
                securityMetrics,
                responseWriter,
                objectMapper
        );
    }

    /**
     * O filtro é inserido na cadeia do Spring Security, depois do contexto e
     * da autenticação anônima. A desativação do registro servlet evita uma
     * segunda execução fora da cadeia de segurança.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            RateLimitFilter rateLimitFilter
    ) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(rateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public RequestBodyLimitFilter requestBodyLimitFilter(
            BusinessLimitProperties properties,
            BusinessLimitMetrics metrics,
            RequestBodyLimitResponseWriter responseWriter
    ) {
        return new RequestBodyLimitFilter(properties, metrics, responseWriter);
    }

    @Bean
    public FilterRegistrationBean<RequestBodyLimitFilter> requestBodyLimitFilterRegistration(
            RequestBodyLimitFilter requestBodyLimitFilter
    ) {
        FilterRegistrationBean<RequestBodyLimitFilter> registration =
                new FilterRegistrationBean<>(requestBodyLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            SecurityErrorResponseHandler securityErrorResponseHandler,
            RateLimitFilter rateLimitFilter,
            RequestBodyLimitFilter requestBodyLimitFilter
    ) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Angular envia o token cru do cookie XSRF-TOKEN. O handler XOR
                        // padrão do Spring Security espera um token mascarado diferente.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/csrf"
                        ).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(
                                "/actuator/info",
                                "/actuator/metrics",
                                "/actuator/metrics/**"
                        ).hasRole("ADMIN")
                        .requestMatchers("/actuator/**").authenticated()
                        .requestMatchers("/api/auth/**").authenticated()
                        // O namespace antigo não possui handlers; mantê-lo fora do wildcard
                        // protegido permite que chamadas a rotas removidas retornem 404.
                        .requestMatchers("/api/bf4/auth/**").permitAll()
                        .requestMatchers("/api/bf4/**").authenticated()
                        .anyRequest().permitAll()
                )
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessHandler((request, response, authentication) ->
                                logoutSuccess(
                                        request,
                                        response,
                                        authentication,
                                        securityErrorResponseHandler
                                )
                        )
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(securityErrorResponseHandler)
                        .accessDeniedHandler(securityErrorResponseHandler)
                )
                .addFilterAfter(rateLimitFilter, AnonymousAuthenticationFilter.class)
                .addFilterBefore(requestBodyLimitFilter, RateLimitFilter.class);

        return http.build();
    }

    private void logoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication,
            SecurityErrorResponseHandler securityErrorResponseHandler
    ) throws IOException {
        if (authentication == null) {
            securityErrorResponseHandler.commence(
                    request,
                    response,
                    new InsufficientAuthenticationException(
                            "Sessão inexistente ou expirada"
                    )
            );
            return;
        }

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}
