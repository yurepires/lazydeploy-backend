package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.model.notification.NotificationHistoryEntry;
import com.yurepires.lazydeploy.model.notification.NotificationHistoryFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface NotificationHistoryRepository {

    Page<NotificationHistoryEntry> findHistory(
            UUID userId,
            NotificationHistoryFilter filter,
            Pageable pageable
    );

    Optional<NotificationHistoryEntry> findByIdAndUserId(UUID notificationId, UUID userId);
}
