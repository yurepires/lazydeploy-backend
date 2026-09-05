package com.yurepires.lazydeploy.service.provider;

import com.yurepires.lazydeploy.exception.ExternalProviderException;
import com.yurepires.lazydeploy.exception.ExternalProviderFailureCategory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalProviderFailureClassifierTest {

    @Test
    void shouldClassifyTimeoutAsRetryable() {
        ExternalProviderException exception = ExternalProviderFailureClassifier.classify(
                "KEEPER",
                new TimeoutException("response timed out")
        );

        assertThat(exception.category())
                .isEqualTo(ExternalProviderFailureCategory.TIMEOUT);
        assertThat(exception.retryable()).isTrue();
        assertThat(ExternalProviderFailureClassifier.outcome(exception.category()))
                .isEqualTo("timeout");
    }

    @Test
    void shouldClassifyConnectionFailureAsRetryable() {
        ExternalProviderException exception = ExternalProviderFailureClassifier.classify(
                "KEEPER",
                new ConnectException("connection refused")
        );

        assertThat(exception.category())
                .isEqualTo(ExternalProviderFailureCategory.CONNECTION_ERROR);
        assertThat(exception.retryable()).isTrue();
    }

    @Test
    void shouldClassifyRateLimitWithoutRetry() {
        WebClientResponseException responseException = WebClientResponseException.create(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too many requests",
                HttpHeaders.EMPTY,
                new byte[0],
                null
        );

        ExternalProviderException exception = ExternalProviderFailureClassifier.classify(
                "KEEPER",
                responseException
        );

        assertThat(exception.category())
                .isEqualTo(ExternalProviderFailureCategory.RATE_LIMITED_BY_PROVIDER);
        assertThat(exception.retryable()).isFalse();
    }
}
