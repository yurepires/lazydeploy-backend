package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.mapper.MonitoringMapper;
import com.yurepires.lazydeploy.model.monitoring.NotificationState;
import com.yurepires.lazydeploy.model.monitoring.NotificationStatus;
import com.yurepires.lazydeploy.model.monitoring.ServerState;
import com.yurepires.lazydeploy.repository.NotificationStateRepository;
import com.yurepires.lazydeploy.model.notification.DeliveryPolicy;
import com.yurepires.lazydeploy.model.notification.NotificationCandidate;
import com.yurepires.lazydeploy.model.notification.NotificationChannel;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.notification.NotificationDecision;
import com.yurepires.lazydeploy.model.notification.NotificationResult;
import com.yurepires.lazydeploy.model.rule.EvaluationContext;
import com.yurepires.lazydeploy.model.rule.RuleEvaluationResult;
import com.yurepires.lazydeploy.model.server.MonitoredServer;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.service.notification.rule.NotificationEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestrator.class);

    private final NotificationEvaluationService evaluationService;
    private final NotificationChannelRegistry channelRegistry;
    private final DeliveryPolicy deliveryPolicy;
    private final NotificationStateRepository notificationStates;
    private final NotificationPersistenceService persistence;
    private final MonitoringMapper monitoringMapper;

    public NotificationOrchestrator(
            NotificationEvaluationService evaluationService,
            NotificationChannelRegistry channelRegistry,
            DeliveryPolicy deliveryPolicy,
            NotificationStateRepository notificationStates,
            NotificationPersistenceService persistence,
            MonitoringMapper monitoringMapper
    ) {
        this.evaluationService = evaluationService;
        this.channelRegistry = channelRegistry;
        this.deliveryPolicy = deliveryPolicy;
        this.notificationStates = notificationStates;
        this.persistence = persistence;
        this.monitoringMapper = monitoringMapper;
    }

    public void initialize(UUID subscriptionId, UUID roundInstanceId) {
        notificationStates.findBySubscriptionIdAndRoundInstanceId(subscriptionId, roundInstanceId)
                .orElseGet(() -> notificationStates.save(
                        monitoringMapper.toEntity(NotificationState.pending(subscriptionId, roundInstanceId))
                ));
    }

    public void process(
            MonitoredServer configuration,
            ServerSnapshot currentSnapshot,
            ServerState previousState,
            ServerState currentState,
            Instant evaluatedAt
    ) {
        UUID subscriptionId = UUID.fromString(configuration.id());
        NotificationState notificationState = notificationStates
                .findBySubscriptionIdAndRoundInstanceId(subscriptionId, currentState.roundInstanceId())
                .map(monitoringMapper::toDomain)
                .orElseGet(() -> monitoringMapper.toDomain(notificationStates.save(
                        monitoringMapper.toEntity(NotificationState.pending(
                                subscriptionId,
                                currentState.roundInstanceId()
                        ))
                )));

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
                notificationState.deduplicationKey(),
                decision,
                evaluatedAt,
                candidateAttributes(configuration),
                configuration.userId()
        );

        List<NotificationResult> results = deliver(configuration, candidate);
        persistence.record(
                notificationState,
                results,
                evaluatedAt,
                deliveryPolicy.isSuccessful(results),
                configuration,
                currentSnapshot
        );
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
                if (result.success()) {
                    log.info(
                            "Notificação enviada | channel={} | subscriptionId={} | sentAt={}",
                            result.channelType(),
                            server.id(),
                            result.sentAt()
                    );
                } else {
                    log.warn("Falha no canal {} para subscriptionId={}: {}", result.channelType(), server.id(), result.errorMessage());
                }
            } catch (RuntimeException exception) {
                log.error("Erro no canal {} para subscriptionId={}", configuration.type(), server.id(), exception);
                NotificationResult failure = NotificationResult.failure(configuration.type(), exception.getMessage());
                results.add(failure);
            }
        }
        return results;
    }

    private void logDecision(MonitoredServer server, ServerSnapshot snapshot, NotificationDecision decision) {
        for (RuleEvaluationResult result : decision.results()) {
            log.info(
                    "RULE EVALUATION | subscriptionId={} | rule={} | matched={} | reason={} | metadata={}",
                    server.id(), result.ruleType(), result.matched(), result.reason(), result.metadata()
            );
        }
        String serverDisplayName = server.displayName();
        if (serverDisplayName == null) {
            serverDisplayName = snapshot.serverGuid();
        }

        log.info(
                "NOTIFICATION DECISION | server='{}' | serverId={} | approved={}",
                serverDisplayName,
                server.serverId(),
                decision.approved()
        );
    }

    private Map<String, Object> candidateAttributes(MonitoredServer configuration) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (configuration.displayName() != null && !configuration.displayName().isBlank()) {
            attributes.put("displayName", configuration.displayName());
        }
        return attributes;
    }
}
