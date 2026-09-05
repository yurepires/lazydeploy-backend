package com.yurepires.lazydeploy.service.notification.history;

import com.yurepires.lazydeploy.exception.InvalidDateRangeException;
import com.yurepires.lazydeploy.exception.InvalidNotificationStatusException;
import com.yurepires.lazydeploy.exception.InvalidPaginationException;
import com.yurepires.lazydeploy.service.validation.PageRequestPolicy;
import com.yurepires.lazydeploy.model.notification.NotificationDeliveryStatus;
import com.yurepires.lazydeploy.model.notification.NotificationHistoryFilter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class NotificationHistoryQueryServiceSupport {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAXIMUM_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "attemptedAt",
            "sentAt",
            "status",
            "channelType",
            "channel"
    );

    private final PageRequestPolicy pageRequestPolicy;

    @Autowired
    public NotificationHistoryQueryServiceSupport(PageRequestPolicy pageRequestPolicy) {
        this.pageRequestPolicy = pageRequestPolicy;
    }

    public NotificationHistoryQueryServiceSupport() {
        this.pageRequestPolicy = new PageRequestPolicy();
    }

    public NotificationHistoryFilter createFilter(
            UUID subscriptionId,
            UUID serverId,
            String status,
            String channel,
            String mapId,
            Instant from,
            Instant to
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDateRangeException(from, to);
        }

        NotificationDeliveryStatus deliveryStatus = parseStatus(status);
        return new NotificationHistoryFilter(
                subscriptionId,
                serverId,
                deliveryStatus,
                normalize(channel),
                normalize(mapId),
                from,
                to
        );
    }

    public Pageable normalizePageable(Pageable pageable) {
        return normalizePageable(pageable, null, null);
    }

    public Pageable normalizePageable(
            Pageable pageable,
            Integer requestedPage,
            Integer requestedSize
    ) {
        Sort sort = Sort.unsorted();
        if (pageable != null) {
            sort = pageable.getSort();
        }
        validateSort(sort);
        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.DESC, "attemptedAt");
        }

        return pageRequestPolicy.apply(
                pageable,
                sort,
                requestedPage,
                requestedSize
        );
    }

    public NotificationHistoryFilter validateFilter(NotificationHistoryFilter filter) {
        if (filter == null) {
            return new NotificationHistoryFilter(null, null, null, null, null, null, null);
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new InvalidDateRangeException(filter.from(), filter.to());
        }
        return filter;
    }

    private NotificationDeliveryStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return NotificationDeliveryStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidNotificationStatusException(status);
        }
    }

    private void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidPaginationException(
                        "Ordenação não permitida para o campo: " + order.getProperty()
                );
            }
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
