package com.yurepires.lazydeploy.model.notification;

import java.util.List;

public interface DeliveryPolicy {
    boolean isSuccessful(List<NotificationResult> results);
}
