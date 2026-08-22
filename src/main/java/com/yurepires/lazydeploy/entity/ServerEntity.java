package com.yurepires.lazydeploy.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "server")
public class ServerEntity {

    @Id
    private UUID id;

    @Column(name = "display_name")
    private String displayName;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ServerEntity() {
    }

    public ServerEntity(UUID id, String displayName, boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.displayName = displayName;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }
    public String getDisplayName() {
        return displayName;
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
