package com.yurepires.lazydeploy.monitoring;

import com.yurepires.lazydeploy.battlefield.Bf4ServerSnapshot;
import com.yurepires.lazydeploy.battlefield.BfListSnapshotService;
import com.yurepires.lazydeploy.config.Bf4Properties;
import com.yurepires.lazydeploy.dto.Bf4ServerResponse;
import com.yurepires.lazydeploy.monitoring.event.MapChangedEvent;
import com.yurepires.lazydeploy.notification.LoggingNotificationService;
import com.yurepires.lazydeploy.notification.NotificationCandidate;
import com.yurepires.lazydeploy.notification.rule.NotificationDecision;
import com.yurepires.lazydeploy.notification.rule.NotificationDecisionReason;
import com.yurepires.lazydeploy.notification.rule.NotificationRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServerMonitorTest {

    private BfListSnapshotService snapshotService;
    private NotificationRuleService ruleService;
    private LoggingNotificationService notificationService;

    @BeforeEach
    void setUp() {
        snapshotService = mock(BfListSnapshotService.class);
        ruleService = mock(NotificationRuleService.class);
        notificationService = mock(LoggingNotificationService.class);
        when(ruleService.evaluate(any(), any())).thenReturn(
                new NotificationDecision(false, NotificationDecisionReason.MAP_NOT_FAVORITE)
        );
    }

    @Test
    void shouldNotCreateEventOnFirstServerState() {
        Bf4Properties.MonitoredServer target = target("guid-1", true);
        ServerMonitor monitor = monitor(List.of(target));
        when(snapshotService.fetchSnapshot()).thenReturn(Optional.of(snapshot(
                server("guid-1", "MP_Prison", "Operation Locker")
        )));

        monitor.monitor();

        verifyNoInteractions(ruleService, notificationService);
    }

    @Test
    void shouldNotCreateEventWhenMapDoesNotChange() {
        Bf4Properties.MonitoredServer target = target("guid-1", true);
        ServerMonitor monitor = monitor(List.of(target));
        Bf4ServerSnapshot snapshot = snapshot(server("guid-1", "MP_Prison", "Operation Locker"));
        when(snapshotService.fetchSnapshot()).thenReturn(Optional.of(snapshot));

        monitor.monitor();
        monitor.monitor();

        verifyNoInteractions(ruleService, notificationService);
    }

    @Test
    void shouldCreateEventWhenMapChanges() {
        Bf4Properties.MonitoredServer target = target("guid-1", true);
        ServerMonitor monitor = monitor(List.of(target));
        when(snapshotService.fetchSnapshot()).thenReturn(
                Optional.of(snapshot(server("guid-1", "MP_Prison", "Operation Locker"))),
                Optional.of(snapshot(server("guid-1", "MP_Siege", "Siege of Shanghai")))
        );

        monitor.monitor();
        monitor.monitor();

        ArgumentCaptor<MapChangedEvent> eventCaptor = ArgumentCaptor.forClass(MapChangedEvent.class);
        verify(ruleService).evaluate(eq(target), eventCaptor.capture());
        assertThat(eventCaptor.getValue().previousMap()).isEqualTo("MP_Prison");
        assertThat(eventCaptor.getValue().currentMap()).isEqualTo("MP_Siege");
        verify(notificationService, never()).notify(any());
    }

    @Test
    void shouldSendLoggingNotificationWhenRulesApprove() {
        Bf4Properties.MonitoredServer target = target("guid-1", true);
        ServerMonitor monitor = monitor(List.of(target));
        when(snapshotService.fetchSnapshot()).thenReturn(
                Optional.of(snapshot(server("guid-1", "MP_Prison", "Operation Locker"))),
                Optional.of(snapshot(server("guid-1", "MP_Siege", "Siege of Shanghai")))
        );
        when(ruleService.evaluate(any(), any())).thenReturn(
                new NotificationDecision(true, NotificationDecisionReason.ALL_RULES_MATCHED)
        );

        monitor.monitor();
        monitor.monitor();

        ArgumentCaptor<NotificationCandidate> candidateCaptor =
                ArgumentCaptor.forClass(NotificationCandidate.class);
        verify(notificationService).notify(candidateCaptor.capture());
        assertThat(candidateCaptor.getValue().serverGuid()).isEqualTo("guid-1");
        assertThat(candidateCaptor.getValue().map()).isEqualTo("MP_Siege");
    }

    @Test
    void shouldKeepIndependentStateForMultipleServers() {
        Bf4Properties.MonitoredServer firstTarget = target("guid-1", true);
        Bf4Properties.MonitoredServer secondTarget = target("guid-2", true);
        ServerMonitor monitor = monitor(List.of(firstTarget, secondTarget));

        when(snapshotService.fetchSnapshot()).thenReturn(
                Optional.of(snapshot(
                        server("guid-1", "MP_Prison", "Operation Locker"),
                        server("guid-2", "MP_Siege", "Siege of Shanghai")
                )),
                Optional.of(snapshot(
                        server("guid-1", "MP_Abandoned", "Zavod 311"),
                        server("guid-2", "MP_Siege", "Siege of Shanghai")
                )),
                Optional.of(snapshot(
                        server("guid-1", "MP_Abandoned", "Zavod 311"),
                        server("guid-2", "MP_Prison", "Operation Locker")
                ))
        );

        monitor.monitor();
        monitor.monitor();
        monitor.monitor();

        ArgumentCaptor<MapChangedEvent> eventCaptor = ArgumentCaptor.forClass(MapChangedEvent.class);
        verify(ruleService, times(2)).evaluate(any(), eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(MapChangedEvent::serverGuid)
                .containsExactly("guid-1", "guid-2");
    }

    private ServerMonitor monitor(List<Bf4Properties.MonitoredServer> targets) {
        Bf4Properties properties = new Bf4Properties(
                "https://api.bflist.io/v2/bf4",
                new Bf4Properties.Monitoring(Duration.ofSeconds(30)),
                new Bf4Properties.Api(100),
                targets
        );
        return new ServerMonitor(snapshotService, properties, ruleService, notificationService);
    }

    private Bf4Properties.MonitoredServer target(String guid, boolean enabled) {
        return new Bf4Properties.MonitoredServer(
                guid,
                "10.0.0.1",
                25226,
                "Server " + guid,
                enabled,
                40,
                Set.of("MP_Prison", "MP_Siege", "MP_Abandoned")
        );
    }

    private Bf4ServerSnapshot snapshot(Bf4ServerResponse... servers) {
        return Bf4ServerSnapshot.from(List.of(servers), Instant.parse("2026-08-11T12:00:00Z"));
    }

    private Bf4ServerResponse server(String guid, String map, String mapLabel) {
        return new Bf4ServerResponse(
                guid,
                "10.0.0.1",
                25226,
                "Server " + guid,
                55,
                64,
                map,
                mapLabel,
                "CONQUEST",
                0,
                1,
                300
        );
    }
}
