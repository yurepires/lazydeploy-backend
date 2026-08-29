package com.yurepires.lazydeploy.model.notification;

import java.util.UUID;

public interface NotificationRecipientResolver {

    String resolveEmail(UUID userId);
}
