package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.model.notification.NotificationCandidate;
import com.yurepires.lazydeploy.model.notification.NotificationChannel;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.notification.NotificationMessageRenderer;
import com.yurepires.lazydeploy.model.notification.NotificationRecipientResolver;
import com.yurepires.lazydeploy.model.notification.NotificationResult;
import com.yurepires.lazydeploy.model.notification.RenderedNotification;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EmailNotificationChannel implements NotificationChannel {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final NotificationMessageRenderer renderer;
    private final LazyDeployProperties properties;
    private final NotificationRecipientResolver recipientResolver;

    public EmailNotificationChannel(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            NotificationMessageRenderer renderer,
            LazyDeployProperties properties,
            NotificationRecipientResolver recipientResolver
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.renderer = renderer;
        this.properties = properties;
        this.recipientResolver = recipientResolver;
    }

    @Override
    public String type() {
        return "EMAIL";
    }

    @Override
    public NotificationResult send(NotificationCandidate candidate, NotificationChannelConfiguration configuration) {
        String recipient = null;
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                return NotificationResult.failure(type(), "SMTP não configurado");
            }

            recipient = recipientResolver.resolveEmail(candidate.userId());

            RenderedNotification rendered = renderer.render(candidate, type());
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.emailFrom());
            message.setTo(recipient);
            message.setSubject(rendered.subject());
            message.setText(rendered.body());
            mailSender.send(message);
            return NotificationResult.success(type(), Instant.now(), recipient);
        } catch (RuntimeException exception) {
            return NotificationResult.failure(type(), exception.getMessage(), recipient);
        }
    }
}
