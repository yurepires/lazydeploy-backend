package com.yurepires.lazydeploy.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "notification_channel_configuration",
        uniqueConstraints = @UniqueConstraint(columnNames = {"subscription_id", "type"})
)
public class NotificationChannelConfigurationEntity {

    @Id
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationChannelConfigurationEntity() {
    }

    public NotificationChannelConfigurationEntity(
            UUID id,
            UUID subscriptionId,
            String type,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.type = type;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public NotificationChannelConfigurationEntity(
            UUID id,
            UUID subscriptionId,
            String type,
            boolean enabled
    ) {
        this(id, subscriptionId, type, enabled, Instant.now(), Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public String getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
