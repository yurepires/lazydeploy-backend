package com.yurepires.lazydeploy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "battlefield_map")
public class BattlefieldMapEntity {

    @Id
    @Column(length = 128)
    private String id;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled;

    @Column(length = 128)
    private String expansion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BattlefieldMapEntity() {
    }

    public BattlefieldMapEntity(
            String id,
            String displayName,
            boolean enabled,
            String expansion,
            Map<String, Object> metadata,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.displayName = displayName;
        this.enabled = enabled;
        this.expansion = expansion;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public BattlefieldMapEntity(
            String id,
            String displayName,
            boolean enabled,
            String expansion,
            Map<String, Object> metadata
    ) {
        this(
                id,
                displayName,
                enabled,
                expansion,
                metadata,
                Instant.now(),
                Instant.now()
        );
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getExpansion() {
        return expansion;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
