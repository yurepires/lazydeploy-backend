package com.yurepires.lazydeploy.notification.rule;

import com.yurepires.lazydeploy.domain.monitoring.NotificationState;
import com.yurepires.lazydeploy.domain.notification.NotificationDecision;
import com.yurepires.lazydeploy.domain.rule.EvaluationContext;
import com.yurepires.lazydeploy.domain.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.domain.rule.RuleEvaluationResult;
import com.yurepires.lazydeploy.domain.rule.RuleEvaluator;
import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.yurepires.lazydeploy.TestFixtures.monitored;
import static com.yurepires.lazydeploy.TestFixtures.rule;
import static com.yurepires.lazydeploy.TestFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationEvaluationServiceTest {

    @Test
    void shouldApproveWhenAllConfiguredRulesMatch() {
        NotificationEvaluationService service = service(
                new MapInRuleEvaluator(), new PlayerCountAtLeastRuleEvaluator()
        );
        MonitoredServer server = monitored("one", "guid", List.of(
                rule("MAP_IN", Map.of("values", List.of("MAP_A"))),
                rule("PLAYER_COUNT_AT_LEAST", Map.of("value", 40))
        ), List.of());

        NotificationDecision decision = service.evaluate(context(server, "MAP_A", 50));

        assertThat(decision.approved()).isTrue();
        assertThat(decision.results()).allMatch(RuleEvaluationResult::matched);
    }

    @Test
    void shouldRejectWhenAnyRequiredRuleDoesNotMatch() {
        NotificationEvaluationService service = service(
                new MapInRuleEvaluator(), new PlayerCountAtLeastRuleEvaluator()
        );
        MonitoredServer server = monitored("one", "guid", List.of(
                rule("MAP_IN", Map.of("values", List.of("MAP_B"))),
                rule("PLAYER_COUNT_AT_LEAST", Map.of("value", 40))
        ), List.of());

        NotificationDecision decision = service.evaluate(context(server, "MAP_A", 50));

        assertThat(decision.approved()).isFalse();
        assertThat(decision.results()).extracting(RuleEvaluationResult::reason)
                .contains("MAP_NOT_MATCHED");
    }

    @Test
    void shouldAllowAddingEvaluatorWithoutChangingEvaluationService() {
        RuleEvaluator custom = new RuleEvaluator() {
            public boolean supports(NotificationRuleDefinition rule) { return "CUSTOM".equals(rule.type()); }
            public RuleEvaluationResult evaluate(NotificationRuleDefinition rule, EvaluationContext context) {
                return new RuleEvaluationResult(true, rule.type(), "CUSTOM_MATCH", Map.of());
            }
        };
        MonitoredServer server = monitored(
                "different", "guid", List.of(rule("CUSTOM", Map.of())), List.of()
        );

        assertThat(service(custom).evaluate(context(server, "ANY_MAP", 1)).approved()).isTrue();
    }

    @Test
    void shouldFailClearlyWhenNoEvaluatorExists() {
        RuleEvaluatorRegistry registry = new RuleEvaluatorRegistry(List.of());
        assertThatThrownBy(() -> registry.resolve(rule("UNKNOWN", Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN");
    }

    private NotificationEvaluationService service(RuleEvaluator... evaluators) {
        return new NotificationEvaluationService(new RuleEvaluatorRegistry(List.of(evaluators)));
    }

    private EvaluationContext context(MonitoredServer server, String map, int players) {
        Instant now = Instant.now();
        return new EvaluationContext(
                server,
                snapshot("guid", map, players),
                null,
                NotificationState.pending(server.identifiers().guid(), java.util.UUID.randomUUID()),
                now
        );
    }
}
