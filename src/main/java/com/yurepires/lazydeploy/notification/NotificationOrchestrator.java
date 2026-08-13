package com.yurepires.lazydeploy.notification;

import com.yurepires.lazydeploy.domain.monitoring.NotificationState;
import com.yurepires.lazydeploy.domain.monitoring.NotificationStatus;
import com.yurepires.lazydeploy.domain.monitoring.ServerState;
import com.yurepires.lazydeploy.domain.notification.DeliveryPolicy;
import com.yurepires.lazydeploy.domain.notification.NotificationCandidate;
import com.yurepires.lazydeploy.domain.notification.NotificationChannel;
import com.yurepires.lazydeploy.domain.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.domain.notification.NotificationDecision;
import com.yurepires.lazydeploy.domain.notification.NotificationResult;
import com.yurepires.lazydeploy.domain.rule.EvaluationContext;
import com.yurepires.lazydeploy.domain.rule.RuleEvaluationResult;
import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;
import com.yurepires.lazydeploy.notification.rule.NotificationEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestrator.class);

    private final NotificationEvaluationService evaluationService;
    private final NotificationChannelRegistry channelRegistry;
    private final DeliveryPolicy deliveryPolicy;
    private final Map<String, NotificationState> notificationStates = new ConcurrentHashMap<>();

    public NotificationOrchestrator(NotificationEvaluationService evaluationService, NotificationChannelRegistry channelRegistry, DeliveryPolicy deliveryPolicy) {
        this.evaluationService = evaluationService;
        this.channelRegistry = channelRegistry;
        this.deliveryPolicy = deliveryPolicy;
    }

    public void initialize(MonitoredServer server, String stateIdentity) {
        notificationStates.put(server.id(), NotificationState.pending(server.id(), stateIdentity));
    }

    public void process(
            MonitoredServer configuration,
            ServerSnapshot currentSnapshot,
            ServerState previousState,
            String stateIdentity,
            Instant evaluatedAt
    ) {
        NotificationState notificationState = notificationStates.compute(
                configuration.id(),
                (serverId, existing) -> existing == null || !existing.stateIdentity().equals(stateIdentity) ? NotificationState.pending(serverId, stateIdentity) : existing
        );

        EvaluationContext context = new EvaluationContext(
                configuration,
                currentSnapshot,
                previousState,
                notificationState,
                evaluatedAt
        );
        NotificationDecision decision = evaluationService.evaluate(context);
        logDecision(configuration, currentSnapshot, decision);

        if (!decision.approved() || notificationState.status() == NotificationStatus.SENT) {
            return;
        }

        NotificationCandidate candidate = new NotificationCandidate(
                configuration.id(),
                currentSnapshot,
                stateIdentity,
                decision,
                evaluatedAt,
                Map.of()
        );

        List<NotificationResult> results = deliver(configuration, candidate);
        NotificationState attempted = notificationState.attemptedAt(evaluatedAt);
        notificationStates.put(configuration.id(), deliveryPolicy.isSuccessful(results) ? attempted.sentAt(evaluatedAt) : attempted);
    }

    private List<NotificationResult> deliver(MonitoredServer server, NotificationCandidate candidate) {
        List<NotificationResult> results = new ArrayList<>();
        for (NotificationChannelConfiguration configuration : server.notificationChannels()) {
            if (!configuration.enabled()) {
                continue;
            }
            try {
                NotificationChannel channel = channelRegistry.resolve(configuration.type());
                NotificationResult result = channel.send(candidate, configuration);
                results.add(result);
                if (!result.success()) {
                    log.warn("Falha no canal {} para serverId={}: {}", result.channelType(), server.id(), result.errorMessage());
                }
            } catch (RuntimeException exception) {
                log.error("Erro no canal {} para serverId={}", configuration.type(), server.id(), exception);
                results.add(NotificationResult.failure(
                        configuration.type(), exception.getMessage()
                ));
            }
        }
        return results;
    }

    private void logDecision(MonitoredServer server, ServerSnapshot snapshot, NotificationDecision decision) {
        for (RuleEvaluationResult result : decision.results()) {
            log.info(
                    "RULE EVALUATION | serverId={} | rule={} | matched={} | reason={} | metadata={}",
                    server.id(), result.ruleType(), result.matched(), result.reason(), result.metadata()
            );
        }
        log.info(
                "NOTIFICATION DECISION | server='{}' | serverId={} | approved={}",
                snapshot.name(), server.id(), decision.approved()
        );
    }
}
