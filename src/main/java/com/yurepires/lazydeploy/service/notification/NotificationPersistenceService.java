package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.mapper.MonitoringMapper;
import com.yurepires.lazydeploy.mapper.NotificationDeliveryAttemptMapper;
import com.yurepires.lazydeploy.model.monitoring.NotificationState;
import com.yurepires.lazydeploy.model.notification.NotificationResult;
import com.yurepires.lazydeploy.model.persistence.NotificationDeliveryAttempt;
import com.yurepires.lazydeploy.model.server.MapSnapshot;
import com.yurepires.lazydeploy.model.server.MonitoredServer;
import com.yurepires.lazydeploy.model.server.PlayerSnapshot;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import com.yurepires.lazydeploy.repository.NotificationDeliveryAttemptRepository;
import com.yurepires.lazydeploy.repository.NotificationStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationPersistenceService {

    private static final int MAXIMUM_ERROR_MESSAGE_LENGTH = 1000;

    private final NotificationStateRepository notificationStateRepository;
    private final NotificationDeliveryAttemptRepository deliveryAttemptRepository;
    private final MonitoringMapper monitoringMapper;
    private final NotificationDeliveryAttemptMapper deliveryAttemptMapper;

    public NotificationPersistenceService(
            NotificationStateRepository notificationStateRepository,
            NotificationDeliveryAttemptRepository deliveryAttemptRepository,
            MonitoringMapper monitoringMapper,
            NotificationDeliveryAttemptMapper deliveryAttemptMapper
    ) {
        this.notificationStateRepository = notificationStateRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.monitoringMapper = monitoringMapper;
        this.deliveryAttemptMapper = deliveryAttemptMapper;
    }

    @Transactional
    public NotificationState record(
            NotificationState state,
            List<NotificationResult> results,
            Instant attemptedAt,
            boolean successful
    ) {
        return record(state, results, attemptedAt, successful, null, null);
    }

    @Transactional
    public NotificationState record(
            NotificationState state,
            List<NotificationResult> results,
            Instant attemptedAt,
            boolean successful,
            MonitoredServer configuration,
            ServerSnapshot snapshot
    ) {
        for (NotificationResult result : results) {
            NotificationDeliveryAttempt deliveryAttempt = createDeliveryAttempt(
                    state,
                    result,
                    attemptedAt,
                    configuration,
                    snapshot
            );
            deliveryAttemptRepository.save(deliveryAttemptMapper.toEntity(deliveryAttempt));
        }

        NotificationState updatedState = state.attemptedAt(attemptedAt);
        if (successful) {
            updatedState = updatedState.sentAt(attemptedAt);
        }

        return monitoringMapper.toDomain(
                notificationStateRepository.save(monitoringMapper.toEntity(updatedState))
        );
    }

    private NotificationDeliveryAttempt createDeliveryAttempt(
            NotificationState state,
            NotificationResult result,
            Instant attemptedAt,
            MonitoredServer configuration,
            ServerSnapshot snapshot
    ) {
        String status = "FAILED";
        Instant sentAt = null;
        String errorCode = errorCodeFor(result.errorMessage());
        if (result.success()) {
            status = "SUCCESS";
            sentAt = result.sentAt();
            errorCode = null;
        }

        return new NotificationDeliveryAttempt(
                UUID.randomUUID(),
                state.subscriptionId(),
                state.roundInstanceId(),
                result.channelType(),
                status,
                attemptedAt,
                sentAt,
                errorCode,
                sanitizeErrorMessage(result.errorMessage()),
                Map.of(),
                ownerUserId(configuration),
                serverId(configuration),
                serverDisplayName(configuration, snapshot),
                mapId(snapshot),
                mapDisplayName(snapshot),
                playerCount(snapshot),
                maxPlayers(snapshot),
                gameMode(snapshot),
                result.recipientSnapshot()
        );
    }

    private String errorCodeFor(String message) {
        if (message == null) {
            return "DELIVERY_FAILED";
        }

        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        if (normalizedMessage.contains("destinatário")
                || normalizedMessage.contains("destinatario")
                || normalizedMessage.contains("e-mail da conta")) {
            return "NOTIFICATION_RECIPIENT_UNAVAILABLE";
        }
        if (normalizedMessage.contains("timeout")
                || normalizedMessage.contains("timed out")) {
            return "DELIVERY_TIMEOUT";
        }
        if (normalizedMessage.contains("authentication")
                || normalizedMessage.contains("smtp")) {
            return "MAIL_SEND_FAILED";
        }
        if (normalizedMessage.contains("connect")
                || normalizedMessage.contains("unavailable")) {
            return "CHANNEL_UNAVAILABLE";
        }

        return "DELIVERY_FAILED";
    }

    private UUID ownerUserId(MonitoredServer configuration) {
        if (configuration == null) {
            return null;
        }
        return configuration.userId();
    }

    private UUID serverId(MonitoredServer configuration) {
        if (configuration == null) {
            return null;
        }
        return configuration.serverId();
    }

    private String serverDisplayName(MonitoredServer configuration, ServerSnapshot snapshot) {
        if (configuration != null
                && configuration.displayName() != null
                && !configuration.displayName().isBlank()) {
            return configuration.displayName();
        }
        if (snapshot == null) {
            return null;
        }
        return snapshot.serverGuid();
    }

    private String mapId(ServerSnapshot snapshot) {
        if (snapshot == null || snapshot.map() == null) {
            return null;
        }

        MapSnapshot map = snapshot.map();
        if (map.normalizedId() != null && !map.normalizedId().isBlank()) {
            return map.normalizedId();
        }
        return map.externalId();
    }

    private String mapDisplayName(ServerSnapshot snapshot) {
        if (snapshot == null || snapshot.map() == null) {
            return null;
        }
        return snapshot.map().displayName();
    }

    private Integer playerCount(ServerSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        PlayerSnapshot players = snapshot.players();
        if (players == null) {
            return null;
        }
        return players.current();
    }

    private Integer maxPlayers(ServerSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        PlayerSnapshot players = snapshot.players();
        if (players == null) {
            return null;
        }
        return players.maximum();
    }

    private String gameMode(ServerSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return snapshot.gameMode();
    }

    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return null;
        }

        String sanitizedMessage = message.replaceAll("[\\r\\n]+", " ");
        sanitizedMessage = sanitizedMessage.replaceAll(
                "(?i)(password|passwd|secret|token|authorization)\\s*[:=]\\s*\\S+",
                "$1=[REDACTED]"
        );

        String normalizedMessage = sanitizedMessage.toLowerCase(Locale.ROOT);
        if (normalizedMessage.contains("smtp")
                || normalizedMessage.contains("authentication failed")
                || normalizedMessage.contains("mail server connection failed")) {
            return "Falha ao conectar ou autenticar no canal de e-mail";
        }
        if (normalizedMessage.contains("timed out")
                || normalizedMessage.contains("timeout")) {
            return "Tempo limite excedido no canal de entrega";
        }

        if (sanitizedMessage.length() <= MAXIMUM_ERROR_MESSAGE_LENGTH) {
            return sanitizedMessage;
        }

        return sanitizedMessage.substring(0, MAXIMUM_ERROR_MESSAGE_LENGTH);
    }
}
