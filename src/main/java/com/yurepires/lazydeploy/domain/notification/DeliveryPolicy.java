package com.yurepires.lazydeploy.domain.notification;

import java.util.List;

public interface DeliveryPolicy {
    boolean isSuccessful(List<NotificationResult> results);
}
