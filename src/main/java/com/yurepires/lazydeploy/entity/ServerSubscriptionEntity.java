package com.yurepires.lazydeploy.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "server_subscription", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "server_id"}))
public class ServerSubscriptionEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "server_id", nullable = false)
    private UUID serverId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ServerSubscriptionEntity() {
    }

    public ServerSubscriptionEntity(UUID id, UUID userId, UUID serverId, boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.serverId = serverId;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getServerId() {
        return serverId;
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
