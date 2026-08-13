package com.yurepires.lazydeploy;

import com.yurepires.lazydeploy.config.LazyDeployProperties;
import com.yurepires.lazydeploy.domain.monitoring.NotificationState;
import com.yurepires.lazydeploy.domain.rule.EvaluationContext;
import com.yurepires.lazydeploy.notification.rule.MapInRuleEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LazyDeployApplicationTests {

    @Autowired
    private LazyDeployProperties properties;

    @Test
    void contextLoads() {
    }

    @Test
    void evaluatesMapInUsingValuesBoundFromApplicationYaml() {
        var server = properties.servers().stream()
                .filter(candidate -> candidate.id().equals("server-5"))
                .findFirst()
                .orElseThrow();
        var rule = server.rules().stream()
                .filter(candidate -> candidate.type().equals("MAP_IN"))
                .findFirst()
                .orElseThrow();
        var snapshot = TestFixtures.snapshot(server.identifiers().guid(), "XP0_Metro", 59);
        var context = new EvaluationContext(
                server,
                snapshot,
                null,
                NotificationState.pending(server.id(), "state"),
                java.time.Instant.now()
        );

        assertThat(new MapInRuleEvaluator().evaluate(rule, context).matched()).isTrue();
    }

}
