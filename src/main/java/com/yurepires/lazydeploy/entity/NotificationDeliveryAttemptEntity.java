package com.yurepires.lazydeploy.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name="notification_delivery_attempt")
public class NotificationDeliveryAttemptEntity {

    @Id
    private UUID id;

    @Column(name="subscription_id", nullable=false)
    private UUID subscriptionId;

    @Column(name="round_instance_id", nullable=false)
    private UUID roundInstanceId;

    @Column(name="channel_type", nullable=false, length=64)
    private String channelType;

    @Column(nullable=false, length=32)
    private String status;

    @Column(name="attempted_at", nullable=false)
    private Instant attemptedAt;

    @Column(name="sent_at")
    private Instant sentAt;

    @Column(name="error_code", length=128)
    private String errorCode;

    @Column(name="error_message", length=1000)
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition="jsonb")
    private Map<String,Object> metadata;

    protected NotificationDeliveryAttemptEntity() {
    }

    public NotificationDeliveryAttemptEntity(
            UUID id,
            UUID subscriptionId,
            UUID roundInstanceId,
            String channelType,
            String status,
            Instant attemptedAt,
            Instant sentAt,
            String errorCode,
            String errorMessage,
            Map<String, Object> metadata
    ) {
        this.id=id;
        this.subscriptionId=subscriptionId;
        this.roundInstanceId=roundInstanceId;
        this.channelType=channelType;
        this.status=status;
        this.attemptedAt=attemptedAt;
        this.sentAt=sentAt;
        this.errorCode=errorCode;
        this.errorMessage=errorMessage;
        this.metadata=metadata;
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

    public String getChannelType(){
        return channelType;
    }

    public String getStatus(){
        return status;
    }

    public Instant getAttemptedAt(){
        return attemptedAt;
    }

    public Instant getSentAt(){
        return sentAt;
    }

    public String getErrorCode(){
        return errorCode;
    }

    public String getErrorMessage(){
        return errorMessage;
    }

    public Map<String,Object> getMetadata(){
        return metadata;
    }
}
