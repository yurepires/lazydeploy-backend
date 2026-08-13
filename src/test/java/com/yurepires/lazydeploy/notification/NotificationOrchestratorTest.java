package com.yurepires.lazydeploy.notification;

import com.yurepires.lazydeploy.domain.monitoring.ServerState;
import com.yurepires.lazydeploy.domain.notification.NotificationCandidate;
import com.yurepires.lazydeploy.domain.notification.NotificationChannel;
import com.yurepires.lazydeploy.domain.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.domain.notification.NotificationResult;
import com.yurepires.lazydeploy.domain.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;
import com.yurepires.lazydeploy.notification.rule.MapInRuleEvaluator;
import com.yurepires.lazydeploy.notification.rule.NotificationEvaluationService;
import com.yurepires.lazydeploy.notification.rule.PlayerCountAtLeastRuleEvaluator;
import com.yurepires.lazydeploy.notification.rule.RuleEvaluatorRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.yurepires.lazydeploy.TestFixtures.monitored;
import static com.yurepires.lazydeploy.TestFixtures.rule;
import static com.yurepires.lazydeploy.TestFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationOrchestratorTest {

    @Test
    void shouldNotifyWhenPreviouslyFalseRuleBecomesTrueAndDeduplicateState() {
        CapturingChannel channel = new CapturingChannel("TEST", true);
        NotificationOrchestrator orchestrator = orchestrator(channel);
        MonitoredServer server = server(List.of(channel("TEST")));
        Instant now = Instant.now();

        orchestrator.initialize(server, "state-1");
        orchestrator.process(server, snapshot("guid", "MAP_A", 20), null, "state-1", now);
        orchestrator.process(server, snapshot("guid", "MAP_A", 50), null, "state-1", now.plusSeconds(1));
        orchestrator.process(server, snapshot("guid", "MAP_A", 55), null, "state-1", now.plusSeconds(2));

        assertThat(channel.candidates).hasSize(1);
    }

    @Test
    void shouldAllowNotificationForNewStateIdentity() {
        CapturingChannel channel = new CapturingChannel("TEST", true);
        NotificationOrchestrator orchestrator = orchestrator(channel);
        MonitoredServer server = server(List.of(channel("TEST")));
        Instant now = Instant.now();

        orchestrator.initialize(server, "state-1");
        orchestrator.process(server, snapshot("guid", "MAP_A", 50), null, "state-1", now);
        orchestrator.process(server, snapshot("guid", "MAP_A", 50), null, "state-2", now.plusSeconds(1));

        assertThat(channel.candidates).hasSize(2);
    }

    @Test
    void shouldContinueWhenOneChannelFails() {
        CapturingChannel failing = new CapturingChannel("FAIL", false);
        CapturingChannel succeeding = new CapturingChannel("SUCCESS", true);
        NotificationOrchestrator orchestrator = orchestrator(failing, succeeding);
        MonitoredServer server = server(List.of(channel("FAIL"), channel("SUCCESS")));

        orchestrator.initialize(server, "state");
        orchestrator.process(server, snapshot("guid", "MAP_A", 50), null, "state", Instant.now());

        assertThat(failing.candidates).hasSize(1);
        assertThat(succeeding.candidates).hasSize(1);
    }

    @Test
    void shouldNotifyWhenOptionalDisplayNameIsMissing() {
        CapturingChannel channel = new CapturingChannel("TEST", true);
        NotificationOrchestrator orchestrator = orchestrator(channel);
        MonitoredServer base = server(List.of(channel("TEST")));
        MonitoredServer withoutDisplayName = new MonitoredServer(
                base.id(),
                base.identifiers(),
                null,
                base.enabled(),
                base.rules(),
                base.notificationChannels()
        );

        orchestrator.initialize(withoutDisplayName, "state");
        orchestrator.process(
                withoutDisplayName,
                snapshot("guid", "MAP_A", 50),
                null,
                "state",
                Instant.now()
        );

        assertThat(channel.candidates).hasSize(1);
        assertThat(channel.candidates.getFirst().attributes()).isEmpty();
    }

    private NotificationOrchestrator orchestrator(NotificationChannel... channels) {
        NotificationEvaluationService evaluation = new NotificationEvaluationService(
                new RuleEvaluatorRegistry(List.of(
                        new MapInRuleEvaluator(), new PlayerCountAtLeastRuleEvaluator()
                ))
        );
        return new NotificationOrchestrator(
                evaluation,
                new NotificationChannelRegistry(List.of(channels)),
                new AnySuccessDeliveryPolicy()
        );
    }

    private MonitoredServer server(List<NotificationChannelConfiguration> channels) {
        List<NotificationRuleDefinition> rules = List.of(
                rule("MAP_IN", Map.of("values", List.of("MAP_A"))),
                rule("PLAYER_COUNT_AT_LEAST", Map.of("value", 40))
        );
        return monitored("server", "guid", rules, channels);
    }

    private NotificationChannelConfiguration channel(String type) {
        return new NotificationChannelConfiguration(type, true, Map.of());
    }

    private static final class CapturingChannel implements NotificationChannel {
        private final String type;
        private final boolean succeeds;
        private final List<NotificationCandidate> candidates = new ArrayList<>();

        private CapturingChannel(String type, boolean succeeds) {
            this.type = type;
            this.succeeds = succeeds;
        }

        public String type() { return type; }

        public NotificationResult send(
                NotificationCandidate candidate,
                NotificationChannelConfiguration configuration
        ) {
            candidates.add(candidate);
            return succeeds
                    ? NotificationResult.success(type, Instant.now())
                    : NotificationResult.failure(type, "failure");
        }
    }
}
