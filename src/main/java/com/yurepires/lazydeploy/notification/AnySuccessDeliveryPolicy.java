package com.yurepires.lazydeploy.notification;

import com.yurepires.lazydeploy.domain.notification.DeliveryPolicy;
import com.yurepires.lazydeploy.domain.notification.NotificationResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnySuccessDeliveryPolicy implements DeliveryPolicy {
    @Override
    public boolean isSuccessful(List<NotificationResult> results) {
        return results.stream().anyMatch(NotificationResult::success);
    }
}
