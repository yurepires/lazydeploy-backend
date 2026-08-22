package com.yurepires.lazydeploy.entity;

import com.yurepires.lazydeploy.model.monitoring.NotificationStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="notification_state", uniqueConstraints=@UniqueConstraint(columnNames={"subscription_id","round_instance_id"}))
public class NotificationStateEntity {

    @Id
    private UUID id;

    @Column(name="subscription_id", nullable=false)
    private UUID subscriptionId;

    @Column(name="round_instance_id", nullable=false)
    private UUID roundInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private NotificationStatus status;

    @Column(name="last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name="notified_at")
    private Instant notifiedAt;

    protected NotificationStateEntity() {
    }

    public NotificationStateEntity(
            UUID id,
            UUID subscriptionId,
            UUID roundInstanceId,
            NotificationStatus status,
            Instant lastAttemptAt,
            Instant notifiedAt
    ) {
        this.id=id;
        this.subscriptionId=subscriptionId;
        this.roundInstanceId=roundInstanceId;
        this.status=status;
        this.lastAttemptAt=lastAttemptAt;
        this.notifiedAt=notifiedAt;
    }

    public UUID getId(){
        return id;
    }

    public UUID getSubscriptionId(){
        return subscriptionId;
    }

    public UUID getRoundInstanceId(){
        return roundInstanceId;
    }

    public NotificationStatus getStatus(){
        return status;
    }

    public Instant getLastAttemptAt(){
        return lastAttemptAt;
    }

    public Instant getNotifiedAt(){
        return notifiedAt;
    }
}
