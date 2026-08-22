package com.yurepires.lazydeploy.service.notification;

import com.yurepires.lazydeploy.mapper.MonitoringMapper;
import com.yurepires.lazydeploy.mapper.NotificationDeliveryAttemptMapper;
import com.yurepires.lazydeploy.model.monitoring.NotificationState;
import com.yurepires.lazydeploy.model.notification.NotificationResult;
import com.yurepires.lazydeploy.model.persistence.NotificationDeliveryAttempt;
import com.yurepires.lazydeploy.repository.NotificationDeliveryAttemptRepository;
import com.yurepires.lazydeploy.repository.NotificationStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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
        for (NotificationResult result : results) {
            NotificationDeliveryAttempt deliveryAttempt = createDeliveryAttempt(
                    state,
                    result,
                    attemptedAt
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
            Instant attemptedAt
    ) {
        String status = "FAILED";
        Instant sentAt = null;
        String errorCode = "DELIVERY_FAILED";

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
                Map.of()
        );
    }

    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return null;
        }

        String sanitizedMessage = message.replaceAll("[\\r\\n]+", " ");
        if (sanitizedMessage.length() <= MAXIMUM_ERROR_MESSAGE_LENGTH) {
            return sanitizedMessage;
        }

        return sanitizedMessage.substring(0, MAXIMUM_ERROR_MESSAGE_LENGTH);
    }
}
