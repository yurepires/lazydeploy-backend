package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.entity.NotificationDeliveryAttemptEntity;
import com.yurepires.lazydeploy.entity.NotificationStateEntity;
import com.yurepires.lazydeploy.mapper.MonitoringMapper;
import com.yurepires.lazydeploy.mapper.NotificationDeliveryAttemptMapper;
import com.yurepires.lazydeploy.model.monitoring.ServerState;
import com.yurepires.lazydeploy.model.notification.NotificationCandidate;
import com.yurepires.lazydeploy.model.notification.NotificationChannel;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.notification.NotificationResult;
import com.yurepires.lazydeploy.model.persistence.NotificationDeliveryAttempt;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.model.server.MonitoredServer;
import com.yurepires.lazydeploy.repository.NotificationDeliveryAttemptRepository;
import com.yurepires.lazydeploy.repository.NotificationStateRepository;
import com.yurepires.lazydeploy.service.notification.rule.MapInRuleEvaluator;
import com.yurepires.lazydeploy.service.notification.rule.NotificationEvaluationService;
import com.yurepires.lazydeploy.service.notification.rule.PlayerCountAtLeastRuleEvaluator;
import com.yurepires.lazydeploy.service.notification.rule.RuleEvaluatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.yurepires.lazydeploy.TestFixtures.monitored;
import static com.yurepires.lazydeploy.TestFixtures.rule;
import static com.yurepires.lazydeploy.TestFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationOrchestratorTest {

    private final Map<String, NotificationStateEntity> states = new HashMap<>();
    private final List<NotificationDeliveryAttempt> attempts = new ArrayList<>();
    private final MonitoringMapper monitoringMapper = Mappers.getMapper(MonitoringMapper.class);
    private final NotificationDeliveryAttemptMapper deliveryAttemptMapper = Mappers.getMapper(
            NotificationDeliveryAttemptMapper.class
    );

    private NotificationStateRepository stateRepository;
    private NotificationDeliveryAttemptRepository attemptRepository;

    @BeforeEach
    void setUpRepositories() {
        states.clear();
        attempts.clear();
        stateRepository = createStateRepository();
        attemptRepository = createAttemptRepository();
    }

    @Test
    void shouldNotifyWhenPreviouslyFalseRuleBecomesTrueAndDeduplicateState() {
        CapturingChannel channel = new CapturingChannel("TEST", true);
        NotificationOrchestrator orchestrator = createOrchestrator(channel);
        MonitoredServer server = createServer(List.of(createChannel("TEST")));
        Instant currentTime = Instant.now();
        UUID roundInstanceId = UUID.randomUUID();

        orchestrator.initialize(UUID.fromString(server.id()), roundInstanceId);
        orchestrator.process(
                server,
                snapshot("guid", "MAP_A", 20),
                null,
                createState(roundInstanceId, currentTime),
                currentTime
        );
        orchestrator.process(
                server,
                snapshot("guid", "MAP_A", 50),
                null,
                createState(roundInstanceId, currentTime),
                currentTime.plusSeconds(1)
        );
        orchestrator.process(
                server,
                snapshot("guid", "MAP_A", 55),
                null,
                createState(roundInstanceId, currentTime),
                currentTime.plusSeconds(2)
        );

        assertThat(channel.candidates).hasSize(1);
    }

    @Test
    void shouldAllowNotificationForNewStateIdentity() {
        CapturingChannel channel = new CapturingChannel("TEST", true);
        NotificationOrchestrator orchestrator = createOrchestrator(channel);
        MonitoredServer server = createServer(List.of(createChannel("TEST")));
        Instant currentTime = Instant.now();
        UUID firstRoundId = UUID.randomUUID();
        UUID secondRoundId = UUID.randomUUID();

        orchestrator.initialize(UUID.fromString(server.id()), firstRoundId);
        orchestrator.process(
                server,
                snapshot("guid", "MAP_A", 50),
                null,
                createState(firstRoundId, currentTime),
                currentTime
        );
        orchestrator.process(
                server,
                snapshot("guid", "MAP_A", 50),
                null,
                createState(secondRoundId, currentTime.plusSeconds(1)),
                currentTime.plusSeconds(1)
        );

        assertThat(channel.candidates).hasSize(2);
    }

    @Test
    void shouldContinueWhenOneChannelFails() {
        CapturingChannel failingChannel = new CapturingChannel("FAIL", false);
        CapturingChannel successfulChannel = new CapturingChannel("SUCCESS", true);
        NotificationOrchestrator orchestrator = createOrchestrator(
                failingChannel,
                successfulChannel
        );
        MonitoredServer server = createServer(List.of(
                createChannel("FAIL"),
                createChannel("SUCCESS")
        ));
        UUID roundInstanceId = UUID.randomUUID();
        Instant currentTime = Instant.now();

        orchestrator.initialize(UUID.fromString(server.id()), roundInstanceId);
        orchestrator.process(
                server,
                snapshot("guid", "MAP_A", 50),
                null,
                createState(roundInstanceId, currentTime),
                currentTime
        );

        assertThat(failingChannel.candidates).hasSize(1);
        assertThat(successfulChannel.candidates).hasSize(1);
        assertThat(attempts)
                .extracting(NotificationDeliveryAttempt::status)
                .containsExactly("FAILED", "SUCCESS");
    }

    @Test
    void shouldNotifyWhenOptionalDisplayNameIsMissing() {
        CapturingChannel channel = new CapturingChannel("TEST", true);
        NotificationOrchestrator orchestrator = createOrchestrator(channel);
        MonitoredServer server = createServer(List.of(createChannel("TEST")));
        MonitoredServer serverWithoutDisplayName = new MonitoredServer(
                server.id(),
                server.identifiers(),
                null,
                server.enabled(),
                server.rules(),
                server.notificationChannels()
        );
        UUID roundInstanceId = UUID.randomUUID();
        Instant currentTime = Instant.now();

        orchestrator.initialize(UUID.fromString(serverWithoutDisplayName.id()), roundInstanceId);
        orchestrator.process(
                serverWithoutDisplayName,
                snapshot("guid", "MAP_A", 50),
                null,
                createState(roundInstanceId, currentTime),
                currentTime
        );

        assertThat(channel.candidates).hasSize(1);
        assertThat(channel.candidates.getFirst().attributes()).isEmpty();
    }

    private NotificationOrchestrator createOrchestrator(NotificationChannel... channels) {
        NotificationEvaluationService evaluationService = new NotificationEvaluationService(
                new RuleEvaluatorRegistry(List.of(
                        new MapInRuleEvaluator(),
                        new PlayerCountAtLeastRuleEvaluator()
                ))
        );
        NotificationPersistenceService persistenceService = new NotificationPersistenceService(
                stateRepository,
                attemptRepository,
                monitoringMapper,
                deliveryAttemptMapper
        );

        return new NotificationOrchestrator(
                evaluationService,
                new NotificationChannelRegistry(List.of(channels)),
                new AnySuccessDeliveryPolicy(),
                stateRepository,
                persistenceService,
                monitoringMapper
        );
    }

    private NotificationStateRepository createStateRepository() {
        NotificationStateRepository repository = mock(NotificationStateRepository.class);
        when(repository.findBySubscriptionIdAndRoundInstanceId(any(), any())).thenAnswer(invocation -> {
            UUID subscriptionId = invocation.getArgument(0);
            UUID roundInstanceId = invocation.getArgument(1);
            return Optional.ofNullable(states.get(createStateKey(subscriptionId, roundInstanceId)));
        });
        when(repository.save(any(NotificationStateEntity.class))).thenAnswer(invocation -> {
            NotificationStateEntity stateEntity = invocation.getArgument(0);
            states.put(
                    createStateKey(
                            stateEntity.getSubscriptionId(),
                            stateEntity.getRoundInstanceId()
                    ),
                    stateEntity
            );
            return stateEntity;
        });
        return repository;
    }

    private NotificationDeliveryAttemptRepository createAttemptRepository() {
        NotificationDeliveryAttemptRepository repository = mock(
                NotificationDeliveryAttemptRepository.class
        );
        when(repository.save(any(NotificationDeliveryAttemptEntity.class))).thenAnswer(invocation -> {
            NotificationDeliveryAttemptEntity attemptEntity = invocation.getArgument(0);
            attempts.add(deliveryAttemptMapper.toDomain(attemptEntity));
            return attemptEntity;
        });
        return repository;
    }

    private String createStateKey(UUID subscriptionId, UUID roundInstanceId) {
        return subscriptionId + ":" + roundInstanceId;
    }

    private MonitoredServer createServer(List<NotificationChannelConfiguration> channels) {
        List<NotificationRuleDefinition> rules = List.of(
                rule("MAP_IN", Map.of("values", List.of("MAP_A"))),
                rule("PLAYER_COUNT_AT_LEAST", Map.of("value", 40))
        );
        return monitored(UUID.randomUUID().toString(), "guid", rules, channels);
    }

    private NotificationChannelConfiguration createChannel(String type) {
        return new NotificationChannelConfiguration(type, true, Map.of());
    }

    private ServerState createState(UUID roundInstanceId, Instant observedAt) {
        return new ServerState(
                UUID.randomUUID(),
                roundInstanceId,
                "MAP_A",
                300,
                observedAt,
                observedAt
        );
    }

    private static final class CapturingChannel implements NotificationChannel {

        private final String type;
        private final boolean succeeds;
        private final List<NotificationCandidate> candidates = new ArrayList<>();

        private CapturingChannel(String type, boolean succeeds) {
            this.type = type;
            this.succeeds = succeeds;
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public NotificationResult send(
                NotificationCandidate candidate,
                NotificationChannelConfiguration configuration
        ) {
            candidates.add(candidate);

            if (succeeds) {
                return NotificationResult.success(type, Instant.now());
            }

            return NotificationResult.failure(type, "failure");
        }
    }
}
