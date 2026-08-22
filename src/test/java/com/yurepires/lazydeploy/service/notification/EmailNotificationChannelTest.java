package com.yurepires.lazydeploy.service.notification;


import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.config.MonitoringProperties;
import com.yurepires.lazydeploy.model.notification.NotificationCandidate;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.notification.NotificationDecision;
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

import static com.yurepires.lazydeploy.TestFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailNotificationChannelTest {

    @Test
    void shouldRenderAndSendEmailUsingConfiguredRecipient() {
        JavaMailSender sender = mock(JavaMailSender.class);
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("mailSender", sender);
        EmailNotificationChannel channel = new EmailNotificationChannel(
                factory.getBeanProvider(JavaMailSender.class),
                new EmailNotificationMessageRenderer(),
                properties()
        );
        NotificationCandidate candidate = new NotificationCandidate(
                "server",
                snapshot("guid", "MAP_A", 50),
                "state",
                new NotificationDecision(true, List.of(), Instant.now()),
                Instant.now(),
                Map.of()
        );

        NotificationResult result = channel.send(
                candidate,
                new NotificationChannelConfiguration(
                        "EMAIL", true, Map.of("to", "recipient@example.com")
                )
        );

        assertThat(result.success()).isTrue();
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("recipient@example.com");
        assertThat(message.getValue().getFrom()).isEqualTo("from@example.com");
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
