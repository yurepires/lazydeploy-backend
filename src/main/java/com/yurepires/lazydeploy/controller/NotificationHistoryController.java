package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.dto.response.NotificationHistoryDetailResponse;
import com.yurepires.lazydeploy.dto.response.NotificationHistoryResponse;
import com.yurepires.lazydeploy.dto.response.PageResponse;
import com.yurepires.lazydeploy.model.notification.NotificationHistoryFilter;
import com.yurepires.lazydeploy.service.notification.history.GetNotificationHistoryService;
import com.yurepires.lazydeploy.service.notification.history.ListNotificationHistoryService;
import com.yurepires.lazydeploy.service.notification.history.ListSubscriptionNotificationHistoryService;
import com.yurepires.lazydeploy.service.notification.history.NotificationHistoryQueryServiceSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/bf4")
public class NotificationHistoryController {

    private final ListNotificationHistoryService listHistoryService;
    private final ListSubscriptionNotificationHistoryService listSubscriptionHistoryService;
    private final GetNotificationHistoryService getHistoryService;
    private final NotificationHistoryQueryServiceSupport querySupport;

    public NotificationHistoryController(
            ListNotificationHistoryService listHistoryService,
            ListSubscriptionNotificationHistoryService listSubscriptionHistoryService,
            GetNotificationHistoryService getHistoryService,
            NotificationHistoryQueryServiceSupport querySupport
    ) {
        this.listHistoryService = listHistoryService;
        this.listSubscriptionHistoryService = listSubscriptionHistoryService;
        this.getHistoryService = getHistoryService;
        this.querySupport = querySupport;
    }

    @GetMapping("/notifications")
    public PageResponse<NotificationHistoryResponse> list(
            @RequestParam(required = false) UUID subscriptionId,
            @RequestParam(required = false) UUID serverId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "channel") String channel,
            @RequestParam(required = false, name = "channelType") String channelType,
            @RequestParam(required = false) String mapId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(
                    size = NotificationHistoryQueryServiceSupport.DEFAULT_PAGE_SIZE,
                    sort = "attemptedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        NotificationHistoryFilter filter = querySupport.createFilter(
                subscriptionId,
                serverId,
                status,
                selectChannel(channel, channelType),
                mapId,
                from,
                to
        );
        Page<NotificationHistoryResponse> page = listHistoryService.list(filter, pageable)
                .map(NotificationHistoryResponse::from);
        return PageResponse.from(page);
    }

    @GetMapping("/notifications/{notificationId}")
    public NotificationHistoryDetailResponse get(@PathVariable UUID notificationId) {
        return NotificationHistoryDetailResponse.from(getHistoryService.get(notificationId));
    }

    @GetMapping("/subscriptions/{subscriptionId}/notifications")
    public PageResponse<NotificationHistoryResponse> listForSubscription(
            @PathVariable UUID subscriptionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "channel") String channel,
            @RequestParam(required = false, name = "channelType") String channelType,
            @RequestParam(required = false) String mapId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(
                    size = NotificationHistoryQueryServiceSupport.DEFAULT_PAGE_SIZE,
                    sort = "attemptedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        NotificationHistoryFilter filter = querySupport.createFilter(
                null,
                null,
                status,
                selectChannel(channel, channelType),
                mapId,
                from,
                to
        );
        Page<NotificationHistoryResponse> page = listSubscriptionHistoryService
                .list(subscriptionId, filter, pageable)
                .map(NotificationHistoryResponse::from);
        return PageResponse.from(page);
    }

    private String selectChannel(String channel, String channelType) {
        if (channel != null && !channel.isBlank()) {
            return channel;
        }
        return channelType;
    }
}
