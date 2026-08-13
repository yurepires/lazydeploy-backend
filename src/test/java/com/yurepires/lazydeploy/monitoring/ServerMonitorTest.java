package com.yurepires.lazydeploy.monitoring;

import com.yurepires.lazydeploy.battlefield.BfListSnapshotService;
import com.yurepires.lazydeploy.battlefield.DefaultServerLocator;
import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import com.yurepires.lazydeploy.domain.server.ServerCatalogSnapshot;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;
import com.yurepires.lazydeploy.notification.NotificationOrchestrator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.yurepires.lazydeploy.TestFixtures.monitored;
import static com.yurepires.lazydeploy.TestFixtures.snapshot;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerMonitorTest {

    @Test
    void shouldInitializeAnyServerWithoutSendingNotificationThenEvaluateNormally() {
        BfListSnapshotService snapshots = mock(BfListSnapshotService.class);
        NotificationOrchestrator orchestrator = mock(NotificationOrchestrator.class);
        MonitoredServer configured = monitored("any-server", "guid", List.of(), List.of());
        ServerSnapshot server = snapshot("guid", "MAP_A", 10);
        ServerCatalogSnapshot catalog = ServerCatalogSnapshot.from(List.of(server), Instant.now());
        when(snapshots.fetchSnapshot()).thenReturn(Optional.of(catalog));
        ServerMonitor monitor = new ServerMonitor(
                snapshots,
                properties(configured),
                new DefaultServerLocator(),
                new MapRotationStateIdentityStrategy(),
                orchestrator
        );

        monitor.monitor();

        verify(orchestrator).initialize(eq(configured), any());
        verify(orchestrator, never()).process(any(), any(), any(), any(), any());

        monitor.monitor();

        verify(orchestrator).process(eq(configured), eq(server), any(), any(), any());
    }

    private LazyDeployProperties properties(MonitoredServer server) {
        return new LazyDeployProperties(
                "https://api.bflist.io/v2/bf4",
                new LazyDeployProperties.Api(100),
                new LazyDeployProperties.Monitoring(Duration.ofSeconds(30)),
                List.of(server),
                new LazyDeployProperties.Channels(new LazyDeployProperties.Email("from@example.com"))
        );
    }
}
