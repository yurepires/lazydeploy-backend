package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.exception.NotificationRecipientUnavailableException;
import com.yurepires.lazydeploy.integration.mailjet.MailjetEmailClient;
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
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EmailNotificationChannel implements NotificationChannel {

    private final MailjetEmailClient mailjetEmailClient;
    private final NotificationMessageRenderer renderer;
    private final NotificationRecipientResolver recipientResolver;
    private final ExternalProviderHealthTracker healthTracker;

    public EmailNotificationChannel(
            MailjetEmailClient mailjetEmailClient,
            NotificationMessageRenderer renderer,
            NotificationRecipientResolver recipientResolver
    ) {
        this(
                mailjetEmailClient,
                renderer,
                recipientResolver,
                new ExternalProviderHealthTracker()
        );
    }

    @Autowired
    public EmailNotificationChannel(
            MailjetEmailClient mailjetEmailClient,
            NotificationMessageRenderer renderer,
            NotificationRecipientResolver recipientResolver,
            ExternalProviderHealthTracker healthTracker
    ) {
        this.mailjetEmailClient = mailjetEmailClient;
        this.renderer = renderer;
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
            recipient = recipientResolver.resolveEmail(candidate.userId());

            RenderedNotification rendered = renderer.render(candidate, type());
            mailjetEmailClient.send(recipient, rendered);
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
