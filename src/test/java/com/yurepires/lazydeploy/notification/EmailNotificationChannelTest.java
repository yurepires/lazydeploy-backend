package com.yurepires.lazydeploy.notification;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.domain.notification.NotificationCandidate;
import com.yurepires.lazydeploy.domain.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.domain.notification.NotificationDecision;
import com.yurepires.lazydeploy.domain.notification.NotificationResult;
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
                "https://api.bflist.io/v2/bf4",
                new LazyDeployProperties.Api(100),
                new LazyDeployProperties.Monitoring(Duration.ofSeconds(30), 30, Duration.ofMinutes(5)),
                new LazyDeployProperties.Providers(
                        new LazyDeployProperties.GameTools("https://api.gametools.network"),
                        new LazyDeployProperties.BattlelogKeeper("https://keeper.battlelog.com")
                ),
                List.of(),
                new LazyDeployProperties.Channels(
                        new LazyDeployProperties.Email("from@example.com")
                )
        );
    }
}
