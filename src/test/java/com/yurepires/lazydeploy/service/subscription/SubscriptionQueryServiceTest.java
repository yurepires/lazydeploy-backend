package com.yurepires.lazydeploy.service.subscription;

import com.yurepires.lazydeploy.dto.response.SubscriptionResponse;
import com.yurepires.lazydeploy.entity.ServerStateEntity;
import com.yurepires.lazydeploy.mapper.CurrentServerStatusMapper;
import com.yurepires.lazydeploy.mapper.MonitoringMapper;
import com.yurepires.lazydeploy.model.monitoring.ServerState;
import com.yurepires.lazydeploy.model.persistence.Server;
import com.yurepires.lazydeploy.model.persistence.ServerSubscription;
import com.yurepires.lazydeploy.repository.ServerStateRepository;
import com.yurepires.lazydeploy.service.map.MapCatalogService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionQueryServiceTest {

    private final ServerSubscriptionPersistenceService subscriptions = mock(
            ServerSubscriptionPersistenceService.class
    );
    private final ServerStateRepository states = mock(ServerStateRepository.class);
    private final MonitoringMapper monitoringMapper = mock(MonitoringMapper.class);
    private final MapCatalogService mapCatalogService = mock(MapCatalogService.class);
    private final SubscriptionQueryService service = new SubscriptionQueryService(
            subscriptions,
            states,
            monitoringMapper,
            new CurrentServerStatusMapper(mapCatalogService)
    );

    @Test
    void shouldBatchLoadStateAndReuseItForSubscriptionsSharingServer() {
        UUID userId = UUID.randomUUID();
        UUID serverId = UUID.randomUUID();
        ServerSubscription first = subscription(userId, serverId);
        ServerSubscription second = subscription(userId, serverId);
        ServerStateEntity entity = stateEntity(serverId);
        ServerState state = state(serverId);

        when(subscriptions.findAllByUserId(userId)).thenReturn(List.of(first, second));
        when(states.findAllByServerIdIn(anyCollection())).thenReturn(List.of(entity));
        when(monitoringMapper.toDomain(entity)).thenReturn(state);
        when(mapCatalogService.listAvailableMaps()).thenReturn(List.of());

        List<SubscriptionResponse> responses = service.list(userId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).currentStatus().map().id()).isEqualTo("MP_Prison");
        assertThat(responses.get(1).currentStatus().map().id()).isEqualTo("MP_Prison");
        verify(states, times(1)).findAllByServerIdIn(anyCollection());
        verify(states).findAllByServerIdIn(org.mockito.ArgumentMatchers.argThat(ids ->
                ids.size() == 1 && ids.contains(serverId)
        ));
    }

    @Test
    void shouldReturnUnavailableStatusWhenServerHasNotBeenObserved() {
        UUID userId = UUID.randomUUID();
        UUID serverId = UUID.randomUUID();
        when(subscriptions.findAllByUserId(userId)).thenReturn(List.of(subscription(userId, serverId)));
        when(states.findAllByServerIdIn(anyCollection())).thenReturn(List.of());
        when(mapCatalogService.listAvailableMaps()).thenReturn(List.of());

        SubscriptionResponse response = service.list(userId).get(0);

        assertThat(response.currentStatus().available()).isFalse();
        assertThat(response.currentStatus().availabilityReason()).isEqualTo("NOT_OBSERVED_YET");
    }

    private ServerSubscription subscription(UUID userId, UUID serverId) {
        Instant now = Instant.parse("2026-09-02T23:45:12Z");
        return new ServerSubscription(
                UUID.randomUUID(),
                userId,
                new Server(serverId, "Example server", true, now, now),
                UUID.randomUUID().toString(),
                true,
                List.of(),
                List.of(),
                now,
                now
        );
    }

    private ServerStateEntity stateEntity(UUID serverId) {
        Instant now = Instant.parse("2026-09-02T23:45:12Z");
        return new ServerStateEntity(
                serverId,
                UUID.randomUUID(),
                "MP_Prison",
                120,
                now,
                now,
                54,
                64,
                "ConquestLarge0"
        );
    }

    private ServerState state(UUID serverId) {
        Instant now = Instant.parse("2026-09-02T23:45:12Z");
        return new ServerState(
                serverId,
                UUID.randomUUID(),
                "MP_Prison",
                120,
                now,
                now,
                54,
                64,
                "ConquestLarge0"
        );
    }
}
