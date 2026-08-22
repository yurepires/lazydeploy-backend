package com.yurepires.lazydeploy.model.notification;

public interface NotificationMessageRenderer {
    RenderedNotification render(NotificationCandidate candidate, String channelType);
}
