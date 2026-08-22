package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.model.notification.DeliveryPolicy;
import com.yurepires.lazydeploy.model.notification.NotificationResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnySuccessDeliveryPolicy implements DeliveryPolicy {
    @Override
    public boolean isSuccessful(List<NotificationResult> results) {
        return results.stream().anyMatch(NotificationResult::success);
    }
}
