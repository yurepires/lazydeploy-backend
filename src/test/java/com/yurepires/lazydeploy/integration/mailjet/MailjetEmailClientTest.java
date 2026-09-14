package com.yurepires.lazydeploy.integration.mailjet;

import com.yurepires.lazydeploy.config.MailjetProperties;
import com.yurepires.lazydeploy.model.notification.RenderedNotification;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailjetEmailClientTest {

    @Test
    void shouldSendMessageUsingMailjetBasicAuthentication() {
        AtomicReference<ClientRequest> requestReference = new AtomicReference<>();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    requestReference.set(request);
                    return Mono.just(successResponse());
                })
                .build();
        MailjetEmailClient client = new MailjetEmailClient(webClient, properties());

        client.send(
                "recipient@example.com",
                new RenderedNotification("Subject", "Body")
        );

        ClientRequest request = requestReference.get();
        assertThat(request).isNotNull();
        assertThat(request.url().getPath()).isEqualTo("/v3.1/send");
        assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION))
                .startsWith("Basic ");
        assertThat(request.headers().getFirst(HttpHeaders.CONTENT_TYPE))
                .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void shouldReportAuthenticationFailureReturnedByMailjet() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.UNAUTHORIZED).build()
                ))
                .build();
        MailjetEmailClient client = new MailjetEmailClient(webClient, properties());

        assertThatThrownBy(() -> client.send(
                "recipient@example.com",
                new RenderedNotification("Subject", "Body")
        ))
                .isInstanceOf(MailjetDeliveryException.class)
                .hasMessage("MAILJET_AUTHENTICATION_FAILED");
    }

    @Test
    void shouldRejectAnUnconfiguredClientBeforeCallingMailjet() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(successResponse()))
                .build();
        MailjetProperties properties = new MailjetProperties(
                "https://api.mailjet.com",
                "",
                "",
                "",
                "LazyDeploy",
                3_000,
                10_000,
                1_048_576
        );
        MailjetEmailClient client = new MailjetEmailClient(webClient, properties);

        assertThatThrownBy(() -> client.send(
                "recipient@example.com",
                new RenderedNotification("Subject", "Body")
        ))
                .isInstanceOf(MailjetDeliveryException.class)
                .hasMessage("MAILJET_NOT_CONFIGURED");
    }

    private ClientResponse successResponse() {
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"Messages\":[{\"Status\":\"success\"}]}")
                .build();
    }

    private MailjetProperties properties() {
        return new MailjetProperties(
                "https://api.mailjet.com",
                "public-key",
                "private-key",
                "from@example.com",
                "LazyDeploy",
                3_000,
                10_000,
                1_048_576
        );
    }
}
