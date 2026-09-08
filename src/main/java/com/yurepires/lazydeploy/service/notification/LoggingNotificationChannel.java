package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.model.notification.NotificationCandidate;
import com.yurepires.lazydeploy.model.notification.NotificationChannel;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.notification.NotificationResult;
import com.yurepires.lazydeploy.service.observability.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class LoggingNotificationChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationChannel.class);
    private final LogSanitizer logSanitizer;

    public LoggingNotificationChannel() {
        this(new LogSanitizer());
    }

    public LoggingNotificationChannel(LogSanitizer logSanitizer) {
        this.logSanitizer = logSanitizer;
    }

    @Override
    public String type() {
        return "LOGGING";
    }

    @Override
    public NotificationResult send(NotificationCandidate candidate, NotificationChannelConfiguration configuration) {
        String mapName = candidate.server().map().displayName();
        if (mapName == null) {
            mapName = candidate.server().map().normalizedId();
        }
        log.info(
                "NOTIFICATION | channel={} | outcome=success | map={} | players={}/{}",
                type(),
                logSanitizer.sanitize(mapName),
                candidate.server().players().current(),
                candidate.server().players().maximum()
        );
        return NotificationResult.success(type(), Instant.now());
    }
}
