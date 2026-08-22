package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.exception.InvalidRequestException;
import com.yurepires.lazydeploy.model.notification.NotificationCandidate;
import com.yurepires.lazydeploy.model.notification.NotificationMessageRenderer;
import com.yurepires.lazydeploy.model.notification.RenderedNotification;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationMessageRenderer implements NotificationMessageRenderer {
    @Override
    public RenderedNotification render(NotificationCandidate candidate, String channelType) {
        if (!"EMAIL".equalsIgnoreCase(channelType)) {
            throw new InvalidRequestException("Renderer não suporta o canal " + channelType);
        }
        String serverName = String.valueOf(candidate.attributes().getOrDefault("displayName", candidate.server().serverGuid()));
        String mapName = candidate.server().map().displayName();
        if (mapName == null) {
            mapName = candidate.server().map().normalizedId();
        }
        String subject = "LazyDeploy: condições atendidas em " + serverName;
        String body = """
                Servidor: %s
                Mapa: %s
                Jogadores: %d/%d
                Modo: %s
                """.formatted(
                serverName, mapName, candidate.server().players().current(),
                candidate.server().players().maximum(), candidate.server().gameMode()
        );
        return new RenderedNotification(subject, body);
    }
}
