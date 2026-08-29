package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.config.MonitoringProperties;
import com.yurepires.lazydeploy.exception.NotificationRecipientUnavailableException;
import com.yurepires.lazydeploy.model.notification.NotificationCandidate;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.notification.NotificationDecision;
import com.yurepires.lazydeploy.model.notification.NotificationRecipientResolver;
import com.yurepires.lazydeploy.model.notification.NotificationResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.yurepires.lazydeploy.TestFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationChannelTest {

    @Test
    void shouldRenderAndSendEmailUsingAccountOwnerEmail() {
        JavaMailSender sender = mock(JavaMailSender.class);
        NotificationRecipientResolver recipientResolver = mock(NotificationRecipientResolver.class);
        UUID userId = UUID.randomUUID();
        when(recipientResolver.resolveEmail(userId)).thenReturn("owner@example.com");
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("mailSender", sender);
        EmailNotificationChannel channel = new EmailNotificationChannel(
                factory.getBeanProvider(JavaMailSender.class),
                new EmailNotificationMessageRenderer(),
                properties(),
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
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("owner@example.com");
        assertThat(message.getValue().getFrom()).isEqualTo("from@example.com");
        assertThat(result.recipientSnapshot()).isEqualTo("owner@example.com");
    }

    @Test
    void shouldReturnFailureWhenAccountOwnerEmailIsUnavailable() {
        JavaMailSender sender = mock(JavaMailSender.class);
        NotificationRecipientResolver recipientResolver = mock(NotificationRecipientResolver.class);
        UUID userId = UUID.randomUUID();
        when(recipientResolver.resolveEmail(userId))
                .thenThrow(new NotificationRecipientUnavailableException());
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("mailSender", sender);
        EmailNotificationChannel channel = new EmailNotificationChannel(
                factory.getBeanProvider(JavaMailSender.class),
                new EmailNotificationMessageRenderer(),
                properties(),
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

    private LazyDeployProperties properties() {
        return new LazyDeployProperties(
                new MonitoringProperties(
                        Duration.ofSeconds(30),
                        30,
                        Duration.ofMinutes(5)
                ),
                "https://api.gametools.network",
                "https://keeper.battlelog.com",
                "from@example.com"
        );
    }
}
