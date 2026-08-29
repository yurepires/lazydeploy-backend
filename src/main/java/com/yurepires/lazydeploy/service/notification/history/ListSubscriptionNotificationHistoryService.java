package com.yurepires.lazydeploy.service.notification.history;

import com.yurepires.lazydeploy.exception.SubscriptionNotFoundException;
import com.yurepires.lazydeploy.model.notification.NotificationHistoryEntry;
import com.yurepires.lazydeploy.model.notification.NotificationHistoryFilter;
import com.yurepires.lazydeploy.repository.NotificationHistoryRepository;
import com.yurepires.lazydeploy.repository.ServerSubscriptionRepository;
import com.yurepires.lazydeploy.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ListSubscriptionNotificationHistoryService {

    private final CurrentUserProvider currentUserProvider;
    private final ServerSubscriptionRepository subscriptionRepository;
    private final NotificationHistoryRepository historyRepository;
    private final NotificationHistoryQueryServiceSupport querySupport;

    public ListSubscriptionNotificationHistoryService(
            CurrentUserProvider currentUserProvider,
            ServerSubscriptionRepository subscriptionRepository,
            NotificationHistoryRepository historyRepository,
            NotificationHistoryQueryServiceSupport querySupport
    ) {
        this.currentUserProvider = currentUserProvider;
        this.subscriptionRepository = subscriptionRepository;
        this.historyRepository = historyRepository;
        this.querySupport = querySupport;
    }

    @Transactional(readOnly = true)
    public Page<NotificationHistoryEntry> list(
            UUID subscriptionId,
            NotificationHistoryFilter filter,
            Pageable pageable
    ) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        subscriptionRepository.findByIdAndUserId(subscriptionId, currentUserId)
                .orElseThrow(SubscriptionNotFoundException::new);

        NotificationHistoryFilter suppliedFilter = filter;
        if (suppliedFilter == null) {
            suppliedFilter = new NotificationHistoryFilter(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
        NotificationHistoryFilter forcedFilter = suppliedFilter.withSubscriptionId(subscriptionId);

        return historyRepository.findHistory(
                currentUserId,
                querySupport.validateFilter(forcedFilter),
                querySupport.normalizePageable(pageable)
        );
    }
}
