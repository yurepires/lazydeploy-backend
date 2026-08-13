package com.yurepires.lazydeploy.notification;

import com.yurepires.lazydeploy.domain.notification.NotificationCandidate;
import com.yurepires.lazydeploy.domain.notification.NotificationChannel;
import com.yurepires.lazydeploy.domain.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.domain.notification.NotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class LoggingNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationChannel.class);

    @Override
    public String type() {
        return "LOGGING";
    }

    @Override
    public NotificationResult send(NotificationCandidate candidate, NotificationChannelConfiguration configuration) {
        log.info(
                "NOTIFICATION | server='{}' | map='{}' | players={}/{} | state={}",
                candidate.server().name(),
                candidate.server().map().label(),
                candidate.server().players().current(),
                candidate.server().players().maximum(),
                candidate.stateIdentity()
        );
        return NotificationResult.success(type(), Instant.now());
    }
}
