package com.yurepires.lazydeploy.security.request;

import com.yurepires.lazydeploy.config.BusinessLimitProperties;
import com.yurepires.lazydeploy.exception.RequestTooLargeException;
import com.yurepires.lazydeploy.service.observability.BusinessLimitMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/** Rejeita corpos JSON grandes antes do binding e das validações de domínio. */
public class RequestBodyLimitFilter extends OncePerRequestFilter {

    private final BusinessLimitProperties properties;
    private final BusinessLimitMetrics metrics;
    private final RequestBodyLimitResponseWriter responseWriter;

    public RequestBodyLimitFilter(
            BusinessLimitProperties properties,
            BusinessLimitMetrics metrics,
            RequestBodyLimitResponseWriter responseWriter
    ) {
        this.properties = properties;
        this.metrics = metrics;
        this.responseWriter = responseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isJsonApiRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        long contentLength = request.getContentLengthLong();
        if (contentLength > properties.maxRequestBodyBytes()) {
            reject(request, response);
            return;
        }

        RequestBodyLimitHttpServletRequest limitedRequest =
                new RequestBodyLimitHttpServletRequest(
                        request,
                        properties.maxRequestBodyBytes()
                );

        try {
            filterChain.doFilter(limitedRequest, response);
        } catch (RequestTooLargeException exception) {
            reject(request, response);
        }
    }

    private boolean isJsonApiRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null
                || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            return false;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()
                && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }

        return requestUri.startsWith("/api/")
                && (HttpMethod.POST.matches(request.getMethod())
                || HttpMethod.PUT.matches(request.getMethod())
                || HttpMethod.PATCH.matches(request.getMethod()));
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        metrics.recordRejection("request_body");
        responseWriter.write(request, response);
    }
}
