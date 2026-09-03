package com.yurepires.lazydeploy.service.subscription;

import com.yurepires.lazydeploy.dto.response.CurrentServerStatusResponse;
import com.yurepires.lazydeploy.dto.response.SubscriptionResponse;
import com.yurepires.lazydeploy.entity.ServerStateEntity;
import com.yurepires.lazydeploy.exception.SubscriptionNotFoundException;
import com.yurepires.lazydeploy.mapper.CurrentServerStatusMapper;
import com.yurepires.lazydeploy.mapper.MonitoringMapper;
import com.yurepires.lazydeploy.model.monitoring.ServerState;
import com.yurepires.lazydeploy.model.persistence.ServerSubscription;
import com.yurepires.lazydeploy.repository.ServerStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consulta subscriptions pertencentes ao usuário com o último status
 * persistido de cada servidor.
 */
@Service
public class SubscriptionQueryService {

    private final ServerSubscriptionPersistenceService subscriptionPersistenceService;
    private final ServerStateRepository serverStateRepository;
    private final MonitoringMapper monitoringMapper;
    private final CurrentServerStatusMapper currentServerStatusMapper;

    public SubscriptionQueryService(
            ServerSubscriptionPersistenceService subscriptionPersistenceService,
            ServerStateRepository serverStateRepository,
            MonitoringMapper monitoringMapper,
            CurrentServerStatusMapper currentServerStatusMapper
    ) {
        this.subscriptionPersistenceService = subscriptionPersistenceService;
        this.serverStateRepository = serverStateRepository;
        this.monitoringMapper = monitoringMapper;
        this.currentServerStatusMapper = currentServerStatusMapper;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> list(UUID userId) {
        List<ServerSubscription> subscriptions = subscriptionPersistenceService
                .findAllByUserId(userId);
        if (subscriptions.isEmpty()) {
            return List.of();
        }

        Map<UUID, ServerState> statesByServerId = loadStates(subscriptions);
        Map<String, String> mapDisplayNames = loadMapDisplayNamesIfNecessary(
                statesByServerId
        );

        return subscriptions.stream()
                .map(subscription -> toResponse(subscription, statesByServerId, mapDisplayNames))
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse get(UUID userId, UUID subscriptionId) {
        ServerSubscription subscription = subscriptionPersistenceService
                .findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(SubscriptionNotFoundException::new);
        Map<UUID, ServerState> statesByServerId = loadStates(List.of(subscription));
        Map<String, String> mapDisplayNames = loadMapDisplayNamesIfNecessary(
                statesByServerId
        );
        return toResponse(subscription, statesByServerId, mapDisplayNames);
    }

    private SubscriptionResponse toResponse(
            ServerSubscription subscription,
            Map<UUID, ServerState> statesByServerId,
            Map<String, String> mapDisplayNames
    ) {
        ServerState state = statesByServerId.get(subscription.server().id());
        CurrentServerStatusResponse currentStatus = currentServerStatusMapper
                .toResponse(state, mapDisplayNames);
        return SubscriptionResponse.from(subscription, currentStatus);
    }

    private Map<UUID, ServerState> loadStates(List<ServerSubscription> subscriptions) {
        Collection<UUID> serverIds = subscriptions.stream()
                .map(subscription -> subscription.server().id())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (serverIds.isEmpty()) {
            return Map.of();
        }

        List<ServerStateEntity> stateEntities =
                serverStateRepository.findAllByServerIdIn(serverIds);
        if (stateEntities == null || stateEntities.isEmpty()) {
            return Map.of();
        }

        return stateEntities
                .stream()
                .map(monitoringMapper::toDomain)
                .collect(Collectors.toMap(
                        ServerState::serverId,
                        Function.identity(),
                        (firstState, secondState) -> secondState
                ));
    }

    private Map<String, String> loadMapDisplayNamesIfNecessary(
            Map<UUID, ServerState> statesByServerId
    ) {
        if (statesByServerId.isEmpty()) {
            return Map.of();
        }

        return currentServerStatusMapper.createMapDisplayNameIndex();
    }
}
