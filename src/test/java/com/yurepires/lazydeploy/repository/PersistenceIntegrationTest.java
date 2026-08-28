package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.dto.request.NotificationChannelRequest;
import com.yurepires.lazydeploy.dto.request.NotificationRuleRequest;
import com.yurepires.lazydeploy.dto.request.SubscriptionCreationRequest;
import com.yurepires.lazydeploy.entity.NotificationDeliveryAttemptEntity;
import com.yurepires.lazydeploy.entity.NotificationStateEntity;
import com.yurepires.lazydeploy.entity.RoundInstanceEntity;
import com.yurepires.lazydeploy.entity.ServerStateEntity;
import com.yurepires.lazydeploy.entity.UserEntity;
import com.yurepires.lazydeploy.mapper.MonitoringMapper;
import com.yurepires.lazydeploy.mapper.NotificationDeliveryAttemptMapper;
import com.yurepires.lazydeploy.mapper.UserMapper;
import com.yurepires.lazydeploy.model.monitoring.NotificationState;
import com.yurepires.lazydeploy.model.monitoring.NotificationStatus;
import com.yurepires.lazydeploy.model.monitoring.RoundInstance;
import com.yurepires.lazydeploy.model.monitoring.ServerState;
import com.yurepires.lazydeploy.model.persistence.NotificationDeliveryAttempt;
import com.yurepires.lazydeploy.model.persistence.User;
import com.yurepires.lazydeploy.service.subscription.SubscriptionApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PersistenceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionApplicationService subscriptionService;

    @Autowired
    private RoundInstanceRepository roundInstanceRepository;

    @Autowired
    private ServerStateRepository serverStateRepository;

    @Autowired
    private NotificationStateRepository notificationStateRepository;

    @Autowired
    private NotificationDeliveryAttemptRepository deliveryAttemptRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MonitoringMapper monitoringMapper;

    @Autowired
    private NotificationDeliveryAttemptMapper deliveryAttemptMapper;

    @Test
    void shouldReuseGlobalServerAndKeepRulesPerUserSubscription() {
        User firstUser = saveUser("first@example.test");
        User secondUser = saveUser("second@example.test");
        String serverGuid = UUID.randomUUID().toString();
        SubscriptionCreationRequest firstRequest = createRequest(
                serverGuid,
                20,
                List.of("MAP_A")
        );
        SubscriptionCreationRequest secondRequest = createRequest(
                serverGuid,
                50,
                List.of("MAP_B")
        );

        var firstSubscription = subscriptionService.create(firstUser.id(), firstRequest);
        var secondSubscription = subscriptionService.create(secondUser.id(), secondRequest);
        assertThatThrownBy(() -> subscriptionService.create(firstUser.id(), firstRequest))
                .isInstanceOf(com.yurepires.lazydeploy.exception.SubscriptionAlreadyExistsException.class);

        assertThat(firstSubscription.server().id()).isEqualTo(secondSubscription.server().id());
        assertThat(firstSubscription.id()).isNotEqualTo(secondSubscription.id());
        assertThat(firstSubscription.rules()).isNotEqualTo(secondSubscription.rules());
    }

    @Test
    void shouldRestoreRoundServerAndNotificationState() {
        UUID serverId = UUID.randomUUID();
        Instant currentTime = Instant.now();

        RoundInstance detectedRound = RoundInstance.detected(
                serverId,
                "MAP_A",
                currentTime,
                10
        );
        RoundInstanceEntity roundEntity = roundInstanceRepository.save(
                monitoringMapper.toEntity(detectedRound)
        );
        RoundInstance savedRound = monitoringMapper.toDomain(roundEntity);

        ServerState serverState = new ServerState(
                serverId,
                savedRound.id(),
                "MAP_A",
                20,
                currentTime,
                currentTime.plusSeconds(10)
        );
        serverStateRepository.save(monitoringMapper.toEntity(serverState));

        UUID subscriptionId = UUID.randomUUID();
        NotificationState sentState = NotificationState
                .pending(subscriptionId, savedRound.id())
                .attemptedAt(currentTime)
                .sentAt(currentTime);
        NotificationStateEntity savedStateEntity = notificationStateRepository.save(
                monitoringMapper.toEntity(sentState)
        );

        NotificationDeliveryAttempt deliveryAttempt = createSuccessfulAttempt(
                subscriptionId,
                savedRound.id(),
                currentTime
        );
        NotificationDeliveryAttemptEntity attemptEntity = deliveryAttemptMapper.toEntity(
                deliveryAttempt
        );
        deliveryAttemptRepository.save(attemptEntity);

        assertThat(serverStateRepository.findById(serverId))
                .get()
                .extracting(ServerStateEntity::getRoundInstanceId)
                .isEqualTo(savedRound.id());
        assertThat(notificationStateRepository.findBySubscriptionIdAndRoundInstanceId(
                subscriptionId,
                savedRound.id()
        )).get().satisfies(restoredState -> {
            assertThat(restoredState.getId()).isEqualTo(savedStateEntity.getId());
            assertThat(restoredState.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(restoredState.getRoundInstanceId()).isEqualTo(savedRound.id());
        });
    }

    private User saveUser(String email) {
        Instant currentTime = Instant.now();
        UserEntity user = new UserEntity(
                null,
                email,
                "{bcrypt}$2a$10$test-only-password-hash",
                true,
                currentTime,
                currentTime
        );
        UserEntity savedEntity = userRepository.save(user);
        return userMapper.toDomain(savedEntity);
    }

    private NotificationDeliveryAttempt createSuccessfulAttempt(
            UUID subscriptionId,
            UUID roundInstanceId,
            Instant attemptedAt
    ) {
        return new NotificationDeliveryAttempt(
                UUID.randomUUID(),
                subscriptionId,
                roundInstanceId,
                "EMAIL",
                "SUCCESS",
                attemptedAt,
                attemptedAt,
                null,
                null,
                Map.of()
        );
    }

    private SubscriptionCreationRequest createRequest(
            String serverGuid,
            int minimumPlayers,
            List<String> maps
    ) {
        List<NotificationRuleRequest> rules = List.of(
                new NotificationRuleRequest("MAP_IN", true, Map.of("values", maps)),
                new NotificationRuleRequest(
                        "PLAYER_COUNT_AT_LEAST",
                        true,
                        Map.of("value", minimumPlayers)
                )
        );
        List<NotificationChannelRequest> channels = List.of(
                new NotificationChannelRequest(
                        "EMAIL",
                        true,
                        Map.of("recipient", "notify@example.test")
                )
        );

        return new SubscriptionCreationRequest(
                serverGuid,
                "Shared server",
                rules,
                channels
        );
    }
}
