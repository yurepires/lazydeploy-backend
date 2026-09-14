package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.exception.NotificationRecipientUnavailableException;
import com.yurepires.lazydeploy.integration.mailjet.MailjetEmailClient;
import com.yurepires.lazydeploy.model.notification.NotificationCandidate;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.notification.NotificationDecision;
import com.yurepires.lazydeploy.model.notification.NotificationRecipientResolver;
import com.yurepires.lazydeploy.model.notification.NotificationResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.yurepires.lazydeploy.TestFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationChannelTest {

    @Test
    void shouldRenderAndSendEmailUsingAccountOwnerEmail() {
        MailjetEmailClient mailjetEmailClient = mock(MailjetEmailClient.class);
        NotificationRecipientResolver recipientResolver = mock(NotificationRecipientResolver.class);
        UUID userId = UUID.randomUUID();
        when(recipientResolver.resolveEmail(userId)).thenReturn("owner@example.com");
        EmailNotificationChannel channel = new EmailNotificationChannel(
                mailjetEmailClient,
                new EmailNotificationMessageRenderer(),
                recipientResolver
        );
        NotificationCandidate candidate = new NotificationCandidate(
                "server",
                snapshot("guid", "MAP_A", 50),
                "state",
                new NotificationDecision(true, List.of(), Instant.now()),
                Instant.now(),
                Map.of(),
                userId
        );

        NotificationResult result = channel.send(
                candidate,
                new NotificationChannelConfiguration(
                        "EMAIL", true, Map.of("recipient", "attacker@example.com")
                )
        );

        assertThat(result.success()).isTrue();
        verify(mailjetEmailClient).send(eq("owner@example.com"), any());
        assertThat(result.recipientSnapshot()).isEqualTo("owner@example.com");
    }

    @Test
    void shouldReturnFailureWhenAccountOwnerEmailIsUnavailable() {
        MailjetEmailClient mailjetEmailClient = mock(MailjetEmailClient.class);
        NotificationRecipientResolver recipientResolver = mock(NotificationRecipientResolver.class);
        UUID userId = UUID.randomUUID();
        when(recipientResolver.resolveEmail(userId))
                .thenThrow(new NotificationRecipientUnavailableException());
        EmailNotificationChannel channel = new EmailNotificationChannel(
                mailjetEmailClient,
                new EmailNotificationMessageRenderer(),
                recipientResolver
        );

        NotificationResult result = channel.send(
                new NotificationCandidate(
                        "server",
                        snapshot("guid", "MAP_A", 50),
                        "state",
                        new NotificationDecision(true, List.of(), Instant.now()),
                        Instant.now(),
                        Map.of(),
                        userId
                ),
                new NotificationChannelConfiguration("EMAIL", true, Map.of())
        );

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("endereço de e-mail");
        assertThat(result.recipientSnapshot()).isNull();
    }
}
