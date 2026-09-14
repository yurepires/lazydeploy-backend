package com.yurepires.lazydeploy.integration.mailjet;

import com.yurepires.lazydeploy.config.MailjetProperties;
import com.yurepires.lazydeploy.integration.mailjet.dto.MailjetEmailAddress;
import com.yurepires.lazydeploy.integration.mailjet.dto.MailjetMessage;
import com.yurepires.lazydeploy.integration.mailjet.dto.MailjetSendRequest;
import com.yurepires.lazydeploy.integration.mailjet.dto.MailjetSendResponse;
import com.yurepires.lazydeploy.model.notification.RenderedNotification;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

/** Cliente da Send API v3.1 do Mailjet. */
@Component
public class MailjetEmailClient {

    private final WebClient webClient;
    private final MailjetProperties properties;

    public MailjetEmailClient(
            @Qualifier("mailjetWebClient") WebClient webClient,
            MailjetProperties properties
    ) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public void send(String recipient, RenderedNotification notification) {
        if (!properties.isConfigured()) {
            throw new MailjetDeliveryException("MAILJET_NOT_CONFIGURED");
        }

        MailjetSendRequest request = createRequest(recipient, notification);

        try {
            MailjetSendResponse response = webClient.post()
                    .uri("/v3.1/send")
                    .headers(headers -> headers.setBasicAuth(
                            properties.apiKey(),
                            properties.apiSecret()
                    ))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MailjetSendResponse.class)
                    .block(Duration.ofMillis(properties.responseTimeoutMs()));

            validateResponse(response);
        } catch (MailjetDeliveryException exception) {
            throw exception;
        } catch (WebClientResponseException exception) {
            throw new MailjetDeliveryException(
                    errorCodeForStatus(exception.getStatusCode().value()),
                    exception
            );
        } catch (RuntimeException exception) {
            throw new MailjetDeliveryException("MAILJET_REQUEST_FAILED", exception);
        }
    }

    private MailjetSendRequest createRequest(
            String recipient,
            RenderedNotification notification
    ) {
        MailjetEmailAddress sender = new MailjetEmailAddress(
                properties.fromEmail(),
                properties.fromName()
        );
        MailjetEmailAddress destination = new MailjetEmailAddress(recipient, null);
        MailjetMessage message = new MailjetMessage(
                sender,
                List.of(destination),
                notification.subject(),
                notification.body()
        );
        return new MailjetSendRequest(List.of(message));
    }

    private void validateResponse(MailjetSendResponse response) {
        if (response == null
                || response.messages() == null
                || response.messages().isEmpty()) {
            throw new MailjetDeliveryException("MAILJET_EMPTY_RESPONSE");
        }

        boolean everyMessageSucceeded = response.messages().stream()
                .allMatch(message -> message != null
                        && "success".equalsIgnoreCase(message.status()));
        if (!everyMessageSucceeded) {
            throw new MailjetDeliveryException("MAILJET_MESSAGE_REJECTED");
        }
    }

    private String errorCodeForStatus(int status) {
        if (status == 401) {
            return "MAILJET_AUTHENTICATION_FAILED";
        }
        if (status == 403) {
            return "MAILJET_SENDER_NOT_AUTHORIZED";
        }
        if (status == 429) {
            return "MAILJET_RATE_LIMITED";
        }
        if (status >= 500) {
            return "MAILJET_PROVIDER_UNAVAILABLE";
        }
        return "MAILJET_REQUEST_REJECTED";
    }
}
