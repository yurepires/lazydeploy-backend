package com.yurepires.lazydeploy.domain.notification;

public interface NotificationMessageRenderer {
    RenderedNotification render(NotificationCandidate candidate, String channelType);
}
