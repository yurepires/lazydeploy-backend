package com.yurepires.lazydeploy.service.notification.history;

import com.yurepires.lazydeploy.model.notification.NotificationHistoryEntry;
import com.yurepires.lazydeploy.model.notification.NotificationHistoryFilter;
import com.yurepires.lazydeploy.repository.NotificationHistoryRepository;
import com.yurepires.lazydeploy.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ListNotificationHistoryService {

    private final CurrentUserProvider currentUserProvider;
    private final NotificationHistoryRepository historyRepository;
    private final NotificationHistoryQueryServiceSupport querySupport;

    public ListNotificationHistoryService(
            CurrentUserProvider currentUserProvider,
            NotificationHistoryRepository historyRepository,
            NotificationHistoryQueryServiceSupport querySupport
    ) {
        this.currentUserProvider = currentUserProvider;
        this.historyRepository = historyRepository;
        this.querySupport = querySupport;
    }

    @Transactional(readOnly = true)
    public Page<NotificationHistoryEntry> list(
            NotificationHistoryFilter filter,
            Pageable pageable
    ) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        NotificationHistoryFilter validatedFilter = querySupport.validateFilter(filter);
        return historyRepository.findHistory(
                currentUserId,
                validatedFilter,
                querySupport.normalizePageable(pageable)
        );
    }

}
