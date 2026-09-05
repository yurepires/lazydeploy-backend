package com.yurepires.lazydeploy.service.provider;

import com.yurepires.lazydeploy.exception.ExternalProviderException;
import com.yurepires.lazydeploy.exception.ExternalProviderFailureCategory;
import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

/** Traduz exceções de clients HTTP em categorias estáveis da aplicação. */
public final class ExternalProviderFailureClassifier {

    private ExternalProviderFailureClassifier() {
    }

    public static ExternalProviderException classify(
            String providerId,
            Throwable failure
    ) {
        if (failure instanceof ExternalProviderException providerException) {
            return providerException;
        }

        ExternalProviderFailureCategory category = categoryOf(failure);
        return new ExternalProviderException(
                providerId,
                category,
                isRetryable(category),
                failure
        );
    }

    public static String outcome(ExternalProviderFailureCategory category) {
        if (category == null) {
            return "other_error";
        }

        return switch (category) {
            case TIMEOUT -> "timeout";
            case CONNECTION_ERROR -> "connection_error";
            case CLIENT_ERROR -> "client_error";
            case SERVER_ERROR -> "server_error";
            case INVALID_RESPONSE -> "invalid_response";
            case RATE_LIMITED_BY_PROVIDER -> "rate_limited";
            case CONCURRENCY_LIMIT_REACHED -> "concurrency_rejected";
            case UNKNOWN -> "other_error";
        };
    }

    private static ExternalProviderFailureCategory categoryOf(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof WebClientResponseException responseException) {
                return responseCategory(responseException);
            }
            if (isTimeout(current)) {
                return ExternalProviderFailureCategory.TIMEOUT;
            }
            if (isConnectionFailure(current)) {
                return ExternalProviderFailureCategory.CONNECTION_ERROR;
            }
            if (current instanceof DataBufferLimitException
                    || isDecodingFailure(current)) {
                return ExternalProviderFailureCategory.INVALID_RESPONSE;
            }
            current = current.getCause();
        }

        return ExternalProviderFailureCategory.UNKNOWN;
    }

    private static ExternalProviderFailureCategory responseCategory(
            WebClientResponseException exception
    ) {
        int status = exception.getStatusCode().value();
        if (status == 429) {
            return ExternalProviderFailureCategory.RATE_LIMITED_BY_PROVIDER;
        }
        if (exception.getStatusCode().is4xxClientError()) {
            return ExternalProviderFailureCategory.CLIENT_ERROR;
        }
        if (exception.getStatusCode().is5xxServerError()) {
            return ExternalProviderFailureCategory.SERVER_ERROR;
        }
        return ExternalProviderFailureCategory.UNKNOWN;
    }

    private static boolean isTimeout(Throwable failure) {
        if (failure instanceof TimeoutException || failure instanceof ReadTimeoutException) {
            return true;
        }

        String className = failure.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (className.contains("timeout") || className.contains("timeoutexception")) {
            return true;
        }

        String message = failure.getMessage();
        if (message == null) {
            return false;
        }

        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return normalizedMessage.contains("timeout")
                || normalizedMessage.contains("timed out");
    }

    private static boolean isConnectionFailure(Throwable failure) {
        return failure instanceof WebClientRequestException
                || failure instanceof ConnectException
                || failure instanceof UnknownHostException
                || failure instanceof SocketException;
    }

    private static boolean isDecodingFailure(Throwable failure) {
        String className = failure.getClass().getName().toLowerCase(Locale.ROOT);
        return className.contains("decodingexception")
                || className.contains("jsonprocessingexception")
                || className.contains("codecexception");
    }

    private static boolean isRetryable(ExternalProviderFailureCategory category) {
        return category == ExternalProviderFailureCategory.TIMEOUT
                || category == ExternalProviderFailureCategory.CONNECTION_ERROR
                || category == ExternalProviderFailureCategory.SERVER_ERROR;
    }
}
