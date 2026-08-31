package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.exception.NotificationRecipientUnavailableException;
import com.yurepires.lazydeploy.model.notification.NotificationCandidate;
import com.yurepires.lazydeploy.model.notification.NotificationChannel;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.notification.NotificationMessageRenderer;
import com.yurepires.lazydeploy.model.notification.NotificationRecipientResolver;
import com.yurepires.lazydeploy.model.notification.NotificationResult;
import com.yurepires.lazydeploy.model.notification.RenderedNotification;
import com.yurepires.lazydeploy.service.observability.ExternalProviderHealthTracker;
import com.yurepires.lazydeploy.service.observability.MailHealthIndicator;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ExternalProviderHealthTracker healthTracker;

    public EmailNotificationChannel(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            NotificationMessageRenderer renderer,
            LazyDeployProperties properties,
            NotificationRecipientResolver recipientResolver
    ) {
        this(
                mailSenderProvider,
                renderer,
                properties,
                recipientResolver,
                new ExternalProviderHealthTracker()
        );
    }

    @Autowired
    public EmailNotificationChannel(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            NotificationMessageRenderer renderer,
            LazyDeployProperties properties,
            NotificationRecipientResolver recipientResolver,
            ExternalProviderHealthTracker healthTracker
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.renderer = renderer;
        this.properties = properties;
        this.recipientResolver = recipientResolver;
        this.healthTracker = healthTracker;
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
                healthTracker.recordFailure(MailHealthIndicator.PROVIDER_ID, "NOT_CONFIGURED");
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
            healthTracker.recordSuccess(MailHealthIndicator.PROVIDER_ID);
            return NotificationResult.success(type(), Instant.now(), recipient);
        } catch (RuntimeException exception) {
            String category = failureCategory(exception);
            healthTracker.recordFailure(MailHealthIndicator.PROVIDER_ID, category);
            return NotificationResult.failure(type(), exception.getMessage(), recipient);
        }
    }

    private String failureCategory(RuntimeException exception) {
        if (exception instanceof NotificationRecipientUnavailableException) {
            return "RECIPIENT_UNAVAILABLE";
        }
        return "DELIVERY_FAILED";
    }
}
