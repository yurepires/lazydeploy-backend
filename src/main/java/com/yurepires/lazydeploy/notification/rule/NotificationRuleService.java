package com.yurepires.lazydeploy.notification.rule;

import com.yurepires.lazydeploy.config.Bf4Properties;
import com.yurepires.lazydeploy.monitoring.event.MapChangedEvent;
import org.springframework.stereotype.Service;

@Service
public class NotificationRuleService {

    public NotificationDecision evaluate(
            Bf4Properties.MonitoredServer server,
            MapChangedEvent event
    ) {
        if (!server.enabled()) {
            return rejected(NotificationDecisionReason.SERVER_DISABLED);
        }

        if (!server.favoriteMaps().contains(event.currentMap())) {
            return rejected(NotificationDecisionReason.MAP_NOT_FAVORITE);
        }

        if (event.numPlayers() < server.minPlayers()) {
            return rejected(NotificationDecisionReason.MIN_PLAYERS_NOT_REACHED);
        }

        return new NotificationDecision(true, NotificationDecisionReason.ALL_RULES_MATCHED);
    }

    private NotificationDecision rejected(NotificationDecisionReason reason) {
        return new NotificationDecision(false, reason);
    }
}
