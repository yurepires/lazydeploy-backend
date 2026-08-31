package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.dto.request.CreateChannelRequest;
import com.yurepires.lazydeploy.dto.request.CreateRuleRequest;
import com.yurepires.lazydeploy.dto.request.CreateSubscriptionRequest;
import com.yurepires.lazydeploy.dto.request.ConfigureSubscriptionRequest;
import com.yurepires.lazydeploy.dto.request.PatchChannelRequest;
import com.yurepires.lazydeploy.dto.request.PatchRuleRequest;
import com.yurepires.lazydeploy.dto.request.PatchSubscriptionRequest;
import com.yurepires.lazydeploy.dto.request.UpdateChannelRequest;
import com.yurepires.lazydeploy.dto.request.UpdateRuleRequest;
import com.yurepires.lazydeploy.dto.request.UpdateSubscriptionRequest;
import com.yurepires.lazydeploy.dto.response.ChannelResponse;
import com.yurepires.lazydeploy.dto.response.ConfiguredSubscriptionResponse;
import com.yurepires.lazydeploy.dto.response.RuleResponse;
import com.yurepires.lazydeploy.dto.response.SubscriptionResponse;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.security.CurrentUserProvider;
import com.yurepires.lazydeploy.service.subscription.SubscriptionApplicationService;
import com.yurepires.lazydeploy.service.subscription.ConfigureSubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final ConfigureSubscriptionService configureSubscriptionService;
    private final CurrentUserProvider currentUserProvider;

    public SubscriptionController(
            SubscriptionApplicationService subscriptionService,
            ConfigureSubscriptionService configureSubscriptionService,
            CurrentUserProvider currentUserProvider
    ) {
        this.subscriptionService = subscriptionService;
        this.configureSubscriptionService = configureSubscriptionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/configure")
    @ResponseStatus(HttpStatus.CREATED)
    public ConfiguredSubscriptionResponse configure(
            @Valid @RequestBody ConfigureSubscriptionRequest request
    ) {
        return ConfiguredSubscriptionResponse.from(
                configureSubscriptionService.configure(request)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(@Valid @RequestBody CreateSubscriptionRequest request) {
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

    @PutMapping("/{id}")
    public SubscriptionResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSubscriptionRequest request) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return SubscriptionResponse.from(subscriptionService.update(currentUserId, id, request.enabled()));
    }

    @PatchMapping("/{id}")
    public SubscriptionResponse patch(@PathVariable UUID id, @Valid @RequestBody PatchSubscriptionRequest request) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return SubscriptionResponse.from(subscriptionService.update(currentUserId, id, request.enabled()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        subscriptionService.delete(currentUserId, id);
    }

    @GetMapping("/{id}/rules")
    public List<RuleResponse> rules(@PathVariable UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return subscriptionService.listRules(currentUserId, id).stream()
                .map(RuleResponse::from)
                .toList();
    }

    @GetMapping("/{id}/rules/{ruleId}")
    public RuleResponse getRule(@PathVariable UUID id, @PathVariable UUID ruleId) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return RuleResponse.from(subscriptionService.getRule(currentUserId, id, ruleId));
    }

    @PostMapping("/{id}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public RuleResponse addRule(@PathVariable UUID id, @Valid @RequestBody CreateRuleRequest request) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        NotificationRuleDefinition rule = subscriptionService.createRule(currentUserId, id, request);
        return RuleResponse.from(rule);
    }

    @PutMapping("/{id}/rules/{ruleId}")
    public RuleResponse updateRule(@PathVariable UUID id, @PathVariable UUID ruleId, @Valid @RequestBody UpdateRuleRequest request) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        subscriptionService.updateRule(currentUserId, id, ruleId, request);
        return RuleResponse.from(subscriptionService.getRule(currentUserId, id, ruleId));
    }

    @PatchMapping("/{id}/rules/{ruleId}")
    public RuleResponse patchRule(@PathVariable UUID id, @PathVariable UUID ruleId, @Valid @RequestBody PatchRuleRequest request) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        subscriptionService.patchRule(currentUserId, id, ruleId, request);
        return RuleResponse.from(subscriptionService.getRule(currentUserId, id, ruleId));
    }

    @DeleteMapping("/{id}/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable UUID id, @PathVariable UUID ruleId) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        subscriptionService.deleteRule(currentUserId, id, ruleId);
    }

    @PostMapping("/{id}/channels")
    @ResponseStatus(HttpStatus.CREATED)
    public ChannelResponse addChannel(@PathVariable UUID id, @Valid @RequestBody CreateChannelRequest request) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        NotificationChannelConfiguration channel = subscriptionService.createChannel(currentUserId, id, request);
        return ChannelResponse.from(channel);
    }

    @GetMapping("/{id}/channels")
    public List<ChannelResponse> channels(@PathVariable UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return subscriptionService.listChannels(currentUserId, id).stream()
                .map(ChannelResponse::from)
                .toList();
    }

    @GetMapping("/{id}/channels/{channelId}")
    public ChannelResponse getChannel(@PathVariable UUID id, @PathVariable UUID channelId) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return ChannelResponse.from(subscriptionService.getChannel(currentUserId, id, channelId));
    }

    @PutMapping("/{id}/channels/{channelId}")
    public ChannelResponse updateChannel(@PathVariable UUID id, @PathVariable UUID channelId, @Valid @RequestBody UpdateChannelRequest request) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        subscriptionService.updateChannel(currentUserId, id, channelId, request);
        return ChannelResponse.from(subscriptionService.getChannel(currentUserId, id, channelId));
    }

    @PatchMapping("/{id}/channels/{channelId}")
    public ChannelResponse patchChannel(@PathVariable UUID id, @PathVariable UUID channelId, @Valid @RequestBody PatchChannelRequest request) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        subscriptionService.patchChannel(currentUserId, id, channelId, request);
        return ChannelResponse.from(subscriptionService.getChannel(currentUserId, id, channelId));
    }

    @DeleteMapping("/{id}/channels/{channelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChannel(@PathVariable UUID id, @PathVariable UUID channelId) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        subscriptionService.deleteChannel(currentUserId, id, channelId);
    }

}
