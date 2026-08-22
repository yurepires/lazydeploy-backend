package com.yurepires.lazydeploy.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_rule")
public class NotificationRuleEntity {

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

    protected NotificationRuleEntity() {
    }

    public NotificationRuleEntity(UUID id, UUID subscriptionId, String type, boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id=id;
        this.subscriptionId=subscriptionId;
        this.type=type;
        this.enabled=enabled;
        this.createdAt=createdAt;
        this.updatedAt=updatedAt;
    }

    public UUID getId(){
        return id;
    }

    public UUID getSubscriptionId(){
        return subscriptionId;
    }

    public String getType(){
        return type;
    }

    public boolean isEnabled(){
        return enabled;
    }

    public Instant getCreatedAt(){
        return createdAt;
    }

    public Instant getUpdatedAt(){
        return updatedAt;
    }
}
