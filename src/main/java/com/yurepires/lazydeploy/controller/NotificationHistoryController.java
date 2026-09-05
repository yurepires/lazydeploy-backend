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
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
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
        Pageable validatedPageable = querySupport.normalizePageable(pageable, page, size);
        Page<NotificationHistoryResponse> pageResponse = listHistoryService.list(filter, validatedPageable)
                .map(NotificationHistoryResponse::from);
        return PageResponse.from(pageResponse);
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
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
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
        Pageable validatedPageable = querySupport.normalizePageable(pageable, page, size);
        Page<NotificationHistoryResponse> pageResponse = listSubscriptionHistoryService
                .list(subscriptionId, filter, validatedPageable)
                .map(NotificationHistoryResponse::from);
        return PageResponse.from(pageResponse);
    }

    private String selectChannel(String channel, String channelType) {
        if (channel != null && !channel.isBlank()) {
            return channel;
        }
        return channelType;
    }
}
