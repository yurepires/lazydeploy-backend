package com.yurepires.lazydeploy.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingNotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);

    public void notify(NotificationCandidate candidate) {
        log.info(
                """

                ========================================
                LAZYDEPLOY NOTIFICATION
                ========================================
                Server: {}
                Map: {}
                Players: {}/{}
                Game mode: {}
                Status: notification approved
                ========================================
                """,
                candidate.serverName(),
                candidate.mapLabel(),
                candidate.players(),
                candidate.maxPlayers(),
                candidate.gameType()
        );
    }
}
