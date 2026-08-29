package com.yurepires.lazydeploy.service.notification.history;

import com.yurepires.lazydeploy.exception.NotificationHistoryNotFoundException;
import com.yurepires.lazydeploy.model.notification.NotificationHistoryEntry;
import com.yurepires.lazydeploy.repository.NotificationHistoryRepository;
import com.yurepires.lazydeploy.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetNotificationHistoryService {

    private final CurrentUserProvider currentUserProvider;
    private final NotificationHistoryRepository historyRepository;

    public GetNotificationHistoryService(
            CurrentUserProvider currentUserProvider,
            NotificationHistoryRepository historyRepository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public NotificationHistoryEntry get(UUID notificationId) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return historyRepository.findByIdAndUserId(notificationId, currentUserId)
                .orElseThrow(NotificationHistoryNotFoundException::new);
    }
}
