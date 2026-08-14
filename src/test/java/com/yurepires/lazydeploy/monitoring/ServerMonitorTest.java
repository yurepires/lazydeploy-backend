package com.yurepires.lazydeploy.monitoring;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import com.yurepires.lazydeploy.domain.server.ServerReference;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;
import com.yurepires.lazydeploy.domain.server.ServerSnapshotProvider;
import com.yurepires.lazydeploy.notification.NotificationOrchestrator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.yurepires.lazydeploy.TestFixtures.monitored;
import static com.yurepires.lazydeploy.TestFixtures.snapshot;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ServerMonitorTest {
    @Test
    void shouldInitializeWithoutNotificationThenEvaluateContinuously() {
        ServerSnapshotProvider provider = mock(ServerSnapshotProvider.class);
        NotificationOrchestrator orchestrator = mock(NotificationOrchestrator.class);
        MonitoredServer configured = monitored("any-server", "guid", List.of(), List.of());
        ServerSnapshot server = snapshot("guid", "MAP_A", 10);
        when(provider.providerId()).thenReturn("TEST");
        when(provider.getSnapshot(any(ServerReference.class))).thenReturn(Optional.of(server));
        ServerMonitor monitor = new ServerMonitor(
                provider, properties(configured), new DefaultRoundTransitionDetector(30, Duration.ofMinutes(5)), orchestrator
        );

        monitor.monitor();
        verify(orchestrator).initialize(eq("guid"), any());
        verify(orchestrator, never()).process(any(), any(), any(), any(), any());

        monitor.monitor();
        verify(orchestrator).process(eq(configured), eq(server), any(), any(), any());
    }

    @Test
    void shouldContinueMonitoringWhenOneProviderCallFails() {
        ServerSnapshotProvider provider = mock(ServerSnapshotProvider.class);
        NotificationOrchestrator orchestrator = mock(NotificationOrchestrator.class);
        MonitoredServer one = monitored("one", "guid-1", List.of(), List.of());
        MonitoredServer two = monitored("two", "guid-2", List.of(), List.of());
        when(provider.providerId()).thenReturn("TEST");
        when(provider.getSnapshot(any())).thenThrow(new RuntimeException("offline"))
                .thenReturn(Optional.of(snapshot("guid-2", "MAP_A", 10)));
        ServerMonitor monitor = new ServerMonitor(
                provider, properties(one, two), new DefaultRoundTransitionDetector(30, Duration.ofMinutes(5)), orchestrator
        );

        monitor.monitor();

        verify(orchestrator).initialize(eq("guid-2"), any());
    }

    private LazyDeployProperties properties(MonitoredServer... servers) {
        return new LazyDeployProperties(
                "https://api.bflist.io/v2/bf4", new LazyDeployProperties.Api(100),
                new LazyDeployProperties.Monitoring(Duration.ofSeconds(30), 30, Duration.ofMinutes(5)),
                new LazyDeployProperties.Providers(
                        new LazyDeployProperties.GameTools("https://api.gametools.network"),
                        new LazyDeployProperties.BattlelogKeeper("https://keeper.battlelog.com")
                ),
                List.of(servers),
                new LazyDeployProperties.Channels(new LazyDeployProperties.Email("from@example.com"))
        );
    }
}
