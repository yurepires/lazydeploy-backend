package com.yurepires.lazydeploy.notification;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.domain.notification.NotificationCandidate;
import com.yurepires.lazydeploy.domain.notification.NotificationChannel;
import com.yurepires.lazydeploy.domain.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.domain.notification.NotificationMessageRenderer;
import com.yurepires.lazydeploy.domain.notification.NotificationResult;
import com.yurepires.lazydeploy.domain.notification.RenderedNotification;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Component
public class EmailNotificationChannel implements NotificationChannel {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final NotificationMessageRenderer renderer;
    private final LazyDeployProperties properties;

    public EmailNotificationChannel(ObjectProvider<JavaMailSender> mailSenderProvider, NotificationMessageRenderer renderer, LazyDeployProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.renderer = renderer;
        this.properties = properties;
    }

    @Override
    public String type() {
        return "EMAIL";
    }

    @Override
    public NotificationResult send(NotificationCandidate candidate, NotificationChannelConfiguration configuration) {
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                return NotificationResult.failure(type(), "SMTP não configurado");
            }

            List<String> recipients = recipients(configuration.parameters().get("to"));
            if (recipients.isEmpty()) {
                return NotificationResult.failure(type(), "Destinatário EMAIL não configurado");
            }

            RenderedNotification rendered = renderer.render(candidate, type());
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.channels().email().from());
            message.setTo(recipients.toArray(String[]::new));
            message.setSubject(rendered.subject());
            message.setText(rendered.body());
            mailSender.send(message);
            return NotificationResult.success(type(), Instant.now());
        } catch (RuntimeException exception) {
            return NotificationResult.failure(type(), exception.getMessage());
        }
    }

    private List<String> recipients(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).filter(s -> !s.isBlank()).toList();
        }
        if (value instanceof java.util.Map<?, ?> indexedValues) {
            return indexedValues.values().stream()
                    .map(String::valueOf)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        if (value instanceof String string && !string.isBlank()) {
            return List.of(string);
        }
        return List.of();
    }
}
