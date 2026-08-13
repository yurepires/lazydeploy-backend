package com.yurepires.lazydeploy.notification;

import com.yurepires.lazydeploy.domain.notification.NotificationCandidate;
import com.yurepires.lazydeploy.domain.notification.NotificationMessageRenderer;
import com.yurepires.lazydeploy.domain.notification.RenderedNotification;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationMessageRenderer implements NotificationMessageRenderer {

    @Override
    public RenderedNotification render(NotificationCandidate candidate, String channelType) {
        if (!"EMAIL".equalsIgnoreCase(channelType)) {
            throw new IllegalArgumentException("Renderer não suporta o canal " + channelType);
        }

        String subject = "LazyDeploy: condições atendidas em " + candidate.server().name();
        String body = """
                Servidor: %s
                Mapa: %s
                Jogadores: %d/%d
                Modo: %s
                """.formatted(
                candidate.server().name(),
                candidate.server().map().label(),
                candidate.server().players().current(),
                candidate.server().players().maximum(),
                candidate.server().gameMode()
        );
        return new RenderedNotification(subject, body);
    }
}
