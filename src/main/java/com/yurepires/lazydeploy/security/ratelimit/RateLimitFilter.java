package com.yurepires.lazydeploy.security.ratelimit;

import com.yurepires.lazydeploy.config.RateLimitProperties;
import com.yurepires.lazydeploy.dto.request.LoginRequest;
import com.yurepires.lazydeploy.exception.RateLimitExceededException;
import com.yurepires.lazydeploy.service.observability.LogSanitizer;
import com.yurepires.lazydeploy.service.observability.SecurityEventLogger;
import com.yurepires.lazydeploy.service.observability.SecurityEventOutcome;
import com.yurepires.lazydeploy.service.observability.SecurityEventType;
import com.yurepires.lazydeploy.service.observability.SecurityMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Aplica todas as políticas de rate limiting em um único ponto antes dos
 * controllers. As políticas são avaliadas apenas para os endpoints definidos.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String SERVER_SEARCH_PATH = "/api/bf4/servers/search";
    private static final String SUBSCRIPTION_CONFIGURE_PATH =
            "/api/bf4/subscriptions/configure";

    private final RateLimitProperties properties;
    private final RateLimitService rateLimitService;
    private final RateLimitKeyResolver keyResolver;
    private final SecurityMetrics securityMetrics;
    private final SecurityEventLogger securityEventLogger;
    private final RateLimitResponseWriter responseWriter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RateLimitProperties properties,
            RateLimitService rateLimitService,
            RateLimitKeyResolver keyResolver,
            SecurityMetrics securityMetrics,
            RateLimitResponseWriter responseWriter,
            ObjectMapper objectMapper
    ) {
        this(
                properties,
                rateLimitService,
                keyResolver,
                securityMetrics,
                new SecurityEventLogger(new LogSanitizer()),
                responseWriter,
                objectMapper
        );
    }

    public RateLimitFilter(
            RateLimitProperties properties,
            RateLimitService rateLimitService,
            RateLimitKeyResolver keyResolver,
            SecurityMetrics securityMetrics,
            SecurityEventLogger securityEventLogger,
            RateLimitResponseWriter responseWriter,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.rateLimitService = rateLimitService;
        this.keyResolver = keyResolver;
        this.securityMetrics = securityMetrics;
        this.securityEventLogger = securityEventLogger;
        this.responseWriter = responseWriter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = keyResolver.resolveClientIp(request);

        if (isPost(request, LOGIN_PATH)) {
            if (!allow(
                    RateLimitPolicyNames.LOGIN_IP,
                    clientIp,
                    properties.login().ip(),
                    request,
                    response
            )) {
                securityMetrics.recordLoginAttempt("rate_limited");
                return;
            }

            CachedBodyHttpServletRequest cachedRequest;
            try {
                cachedRequest = CachedBodyHttpServletRequest.from(request);
            } catch (IOException exception) {
                filterChain.doFilter(request, response);
                return;
            }
            String normalizedEmail = extractLoginEmail(cachedRequest);
            if (normalizedEmail != null && !allow(
                    RateLimitPolicyNames.LOGIN_IDENTITY,
                    normalizedEmail,
                    properties.login().identity(),
                    request,
                    response
            )) {
                securityMetrics.recordLoginAttempt("rate_limited");
                return;
            }

            filterChain.doFilter(cachedRequest, response);
            return;
        }

        if (isPost(request, REGISTER_PATH)) {
            allowOrReturn(
                    RateLimitPolicyNames.REGISTER_IP,
                    clientIp,
                    properties.register(),
                    request,
                    response,
                    filterChain
            );
            return;
        }

        if (isGet(request, SERVER_SEARCH_PATH)) {
            if (!allowAuthenticatedUser(
                    RateLimitPolicyNames.SERVER_SEARCH_USER,
                    properties.serverSearch().user(),
                    request,
                    response
            )) {
                return;
            }
            allowOrReturn(
                    RateLimitPolicyNames.SERVER_SEARCH_IP,
                    clientIp,
                    properties.serverSearch().ip(),
                    request,
                    response,
                    filterChain
            );
            return;
        }

        if (isPost(request, SUBSCRIPTION_CONFIGURE_PATH)) {
            if (!allowAuthenticatedUser(
                    RateLimitPolicyNames.SUBSCRIPTION_CONFIGURE_USER,
                    properties.subscriptionConfigure().user(),
                    request,
                    response
            )) {
                return;
            }
            allowOrReturn(
                    RateLimitPolicyNames.SUBSCRIPTION_CONFIGURE_IP,
                    clientIp,
                    properties.subscriptionConfigure().ip(),
                    request,
                    response,
                    filterChain
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void allowOrReturn(
            String policyName,
            String key,
            RateLimitProperties.Policy policy,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {
        if (!allow(policyName, key, policy, request, response)) {
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean allowAuthenticatedUser(
            String policyName,
            RateLimitProperties.Policy policy,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String userKey = keyResolver.resolveAuthenticatedUser();
        if (userKey == null) {
            return true;
        }
        return allow(policyName, userKey, policy, request, response);
    }

    private boolean allow(
            String policyName,
            String key,
            RateLimitProperties.Policy policy,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        RateLimitDecision decision = rateLimitService.tryConsume(policyName, key, policy);
        String outcome;
        if (decision.allowed()) {
            outcome = "allowed";
        } else {
            outcome = "blocked";
        }
        securityMetrics.recordRateLimitRequest(policyName, outcome);
        if (decision.allowed()) {
            return true;
        }

        responseWriter.write(
                request,
                response,
                new RateLimitExceededException(decision.retryAfterSeconds())
        );
        securityEventLogger.log(
                rateLimitEventType(policyName),
                SecurityEventOutcome.REJECTED,
                "RATE_LIMIT",
                request.getRequestURI()
        );
        return false;
    }

    private SecurityEventType rateLimitEventType(String policyName) {
        if (RateLimitPolicyNames.LOGIN_IP.equals(policyName)
                || RateLimitPolicyNames.LOGIN_IDENTITY.equals(policyName)) {
            return SecurityEventType.AUTH_RATE_LIMITED;
        }
        if (RateLimitPolicyNames.REGISTER_IP.equals(policyName)) {
            return SecurityEventType.REGISTER_RATE_LIMITED;
        }
        return SecurityEventType.RATE_LIMIT_REJECTED;
    }

    private String extractLoginEmail(CachedBodyHttpServletRequest request) {
        try {
            LoginRequest loginRequest = objectMapper.readValue(
                    request.body(),
                    LoginRequest.class
            );
            if (loginRequest.email() == null || loginRequest.email().isBlank()) {
                return null;
            }
            return keyResolver.normalizeLoginEmail(loginRequest.email());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean isPost(HttpServletRequest request, String path) {
        return HttpMethod.POST.matches(request.getMethod())
                && path.equals(requestPath(request));
    }

    private boolean isGet(HttpServletRequest request, String path) {
        return HttpMethod.GET.matches(request.getMethod())
                && path.equals(requestPath(request));
    }

    private String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath == null || contextPath.isEmpty()) {
            return requestUri;
        }
        if (requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
