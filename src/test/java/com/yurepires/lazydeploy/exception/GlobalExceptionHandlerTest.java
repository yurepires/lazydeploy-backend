package com.yurepires.lazydeploy.exception;

import com.yurepires.lazydeploy.dto.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldReturnStructuredNotFoundResponse() {
        MockHttpServletRequest request = createRequest("/api/bf4/subscriptions/unknown");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(
                new SubscriptionNotFoundException(),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("SUBSCRIPTION_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Inscrição não encontrada");
        assertThat(response.getBody().path())
                .isEqualTo("/api/bf4/subscriptions/unknown");
    }

    @Test
    void shouldReturnStructuredInvalidRequestResponse() {
        MockHttpServletRequest request = createRequest("/api/bf4/servers/search");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidRequest(
                new InvalidRequestException("O texto de busca deve ser informado"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().message())
                .isEqualTo("O texto de busca deve ser informado");
    }

    private MockHttpServletRequest createRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
