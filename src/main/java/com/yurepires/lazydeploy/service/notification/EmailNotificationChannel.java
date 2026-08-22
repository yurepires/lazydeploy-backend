package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.model.notification.NotificationCandidate;
import com.yurepires.lazydeploy.model.notification.NotificationChannel;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.notification.NotificationMessageRenderer;
import com.yurepires.lazydeploy.model.notification.NotificationResult;
import com.yurepires.lazydeploy.model.notification.RenderedNotification;
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

    public EmailNotificationChannel(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            NotificationMessageRenderer renderer,
            LazyDeployProperties properties
    ) {
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

            Object recipientValue = configuration.parameters().get("recipient");
            if (recipientValue == null) {
                recipientValue = configuration.parameters().get("to");
            }
            List<String> recipients = recipients(recipientValue);
            if (recipients.isEmpty()) {
                return NotificationResult.failure(type(), "Destinatário EMAIL não configurado");
            }

            RenderedNotification rendered = renderer.render(candidate, type());
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.emailFrom());
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
