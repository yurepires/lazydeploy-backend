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
        String subject = "LazyDeploy | Prepare-se para a batalha em " + serverName;
        String body = """
                Mapa: %s
                Servidor: %s
                Jogadores: %d/%d
                Modo: %s

                O servidor atingiu as condições configuradas na sua assinatura, então este pode ser um bom momento para entrar na partida.

                Boa jogatina!

                LazyDeploy
                """.formatted(
                mapName, serverName, candidate.server().players().current(),
                candidate.server().players().maximum(), candidate.server().gameMode()
        );
        return new RenderedNotification(subject, body);
    }
}
