package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.dto.request.NotificationRuleRequest;
import com.yurepires.lazydeploy.dto.request.SubscriptionCreationRequest;
import com.yurepires.lazydeploy.dto.response.SubscriptionResponse;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.security.CurrentUserProvider;
import com.yurepires.lazydeploy.service.subscription.SubscriptionApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bf4/subscriptions")
public class SubscriptionController {

    private final SubscriptionApplicationService subscriptionService;
    private final CurrentUserProvider currentUserProvider;

    public SubscriptionController(
            SubscriptionApplicationService subscriptionService,
            CurrentUserProvider currentUserProvider
    ) {
        this.subscriptionService = subscriptionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(
            @Valid @RequestBody SubscriptionCreationRequest request
    ) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return SubscriptionResponse.from(subscriptionService.create(currentUserId, request));
    }

    @GetMapping
    public List<SubscriptionResponse> list() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return subscriptionService.list(currentUserId)
                .stream()
                .map(SubscriptionResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public SubscriptionResponse get(@PathVariable UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return SubscriptionResponse.from(subscriptionService.get(currentUserId, id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        subscriptionService.delete(currentUserId, id);
    }

    @GetMapping("/{id}/rules")
    public List<NotificationRuleDefinition> rules(@PathVariable UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return subscriptionService.get(currentUserId, id).rules();
    }

    @PostMapping("/{id}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public List<NotificationRuleDefinition> addRule(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationRuleRequest request
    ) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return subscriptionService.addRule(currentUserId, id, request).rules();
    }

    @PutMapping("/{id}/rules/{ruleId}")
    public List<NotificationRuleDefinition> updateRule(
            @PathVariable UUID id,
            @PathVariable UUID ruleId,
            @Valid @RequestBody NotificationRuleRequest request
    ) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return subscriptionService.updateRule(currentUserId, id, ruleId, request).rules();
    }

    @DeleteMapping("/{id}/rules/{ruleId}")
    public List<NotificationRuleDefinition> deleteRule(
            @PathVariable UUID id,
            @PathVariable UUID ruleId
    ) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return subscriptionService.deleteRule(currentUserId, id, ruleId).rules();
    }
}
