package com.yurepires.lazydeploy.service.monitoring;

import com.yurepires.lazydeploy.model.monitoring.ServerState;
import com.yurepires.lazydeploy.model.persistence.Server;
import com.yurepires.lazydeploy.model.persistence.ServerSubscription;
import com.yurepires.lazydeploy.model.server.ServerReference;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.model.server.ServerSnapshotProvider;
import com.yurepires.lazydeploy.service.notification.NotificationOrchestrator;
import com.yurepires.lazydeploy.service.subscription.ServerSubscriptionPersistenceService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.yurepires.lazydeploy.TestFixtures.snapshot;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerMonitorTest {

    @Test
    void shouldFetchKeeperOncePerServerAndEvaluateEverySubscription() {
        ServerSnapshotProvider snapshotProvider = mock(ServerSnapshotProvider.class);
        ServerSubscriptionPersistenceService subscriptionService = mock(
                ServerSubscriptionPersistenceService.class
        );
        MonitoringStateService monitoringStateService = mock(MonitoringStateService.class);
        NotificationOrchestrator notificationOrchestrator = mock(NotificationOrchestrator.class);

        Server server = createServer();
        ServerSubscription firstSubscription = createSubscription(server, "guid");
        ServerSubscription secondSubscription = createSubscription(server, "guid");
        ServerSnapshot serverSnapshot = snapshot("guid", "MAP_A", 50);
        ServerState currentState = createState(server.id());

        when(subscriptionService.findAllEnabled()).thenReturn(
                List.of(firstSubscription, secondSubscription)
        );
        when(snapshotProvider.providerId()).thenReturn("KEEPER");
        when(snapshotProvider.getSnapshot(any(ServerReference.class))).thenReturn(
                Optional.of(serverSnapshot)
        );
        when(monitoringStateService.observe(server, serverSnapshot))
                .thenReturn(new MonitoringObservation(null, currentState, true, true))
                .thenReturn(new MonitoringObservation(currentState, currentState, false, false));

        ServerMonitor serverMonitor = new ServerMonitor(
                snapshotProvider,
                subscriptionService,
                monitoringStateService,
                notificationOrchestrator
        );

        serverMonitor.monitor();

        verify(snapshotProvider, times(1)).getSnapshot(any());
        verify(notificationOrchestrator, times(2)).initialize(any(), any());
        verify(notificationOrchestrator, never()).process(any(), any(), any(), any(), any());

        serverMonitor.monitor();

        verify(snapshotProvider, times(2)).getSnapshot(any());
        verify(notificationOrchestrator, times(2)).process(
                any(),
                eq(serverSnapshot),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldContinueMonitoringOtherServersWhenOneFails() {
        ServerSnapshotProvider snapshotProvider = mock(ServerSnapshotProvider.class);
        ServerSubscriptionPersistenceService subscriptionService = mock(
                ServerSubscriptionPersistenceService.class
        );
        MonitoringStateService monitoringStateService = mock(MonitoringStateService.class);
        NotificationOrchestrator notificationOrchestrator = mock(NotificationOrchestrator.class);

        Server firstServer = createServer();
        Server secondServer = createServer();
        ServerSnapshot secondServerSnapshot = snapshot("guid-2", "MAP_A", 10);

        when(subscriptionService.findAllEnabled()).thenReturn(List.of(
                createSubscription(firstServer, "guid-1"),
                createSubscription(secondServer, "guid-2")
        ));
        when(snapshotProvider.providerId()).thenReturn("KEEPER");
        when(snapshotProvider.getSnapshot(any()))
                .thenThrow(new RuntimeException("offline"))
                .thenReturn(Optional.of(secondServerSnapshot));
        when(monitoringStateService.observe(secondServer, secondServerSnapshot)).thenReturn(
                new MonitoringObservation(null, createState(secondServer.id()), true, true)
        );

        ServerMonitor serverMonitor = new ServerMonitor(
                snapshotProvider,
                subscriptionService,
                monitoringStateService,
                notificationOrchestrator
        );

        serverMonitor.monitor();

        verify(notificationOrchestrator).initialize(any(), any());
    }

    private Server createServer() {
        Instant currentTime = Instant.now();
        return new Server(UUID.randomUUID(), "Server", true, currentTime, currentTime);
    }

    private ServerSubscription createSubscription(Server server, String externalGuid) {
        Instant currentTime = Instant.now();
        return new ServerSubscription(
                UUID.randomUUID(),
                UUID.randomUUID(),
                server,
                externalGuid,
                true,
                List.of(),
                List.of(),
                currentTime,
                currentTime
        );
    }

    private ServerState createState(UUID serverId) {
        Instant currentTime = Instant.now();
        return new ServerState(
                serverId,
                UUID.randomUUID(),
                "MAP_A",
                300,
                currentTime,
                currentTime
        );
    }
}
