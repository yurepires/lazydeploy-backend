package com.yurepires.lazydeploy.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "server_identifier", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "identifier_type", "identifier_value"}))
public class ServerIdentifierEntity {

    @Id
    private UUID id;

    @Column(name = "server_id", nullable = false)
    private UUID serverId;

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(name = "identifier_type", nullable = false, length = 64)
    private String type;

    @Column(name = "identifier_value", nullable = false)
    private String value;

    protected ServerIdentifierEntity() {
    }

    public ServerIdentifierEntity(UUID id, UUID serverId, String provider, String type, String value) {
        this.id = id;
        this.serverId = serverId;
        this.provider = provider;
        this.type = type;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public UUID getServerId() {
        return serverId;
    }

    public String getProvider() {
        return provider;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
