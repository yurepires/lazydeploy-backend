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
        Object serverName = candidate.attributes().getOrDefault("displayName", candidate.server().serverGuid());
        String mapName = candidate.server().map().displayName() == null
                ? candidate.server().map().normalizedId()
                : candidate.server().map().displayName();
        log.info("NOTIFICATION | server='{}' | map='{}' | players={}/{} | round={}",
                serverName, mapName, candidate.server().players().current(),
                candidate.server().players().maximum(), candidate.stateIdentity());
        return NotificationResult.success(type(), Instant.now());
    }
}
