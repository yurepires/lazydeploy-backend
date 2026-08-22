package com.yurepires.lazydeploy.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="server_state")
public class ServerStateEntity {

    @Id
    @Column(name="server_id")
    private UUID serverId;

    @Column(name="round_instance_id", nullable=false)
    private UUID roundInstanceId;

    @Column(name="map_id", nullable=false)
    private String mapId;

    @Column(name="previous_round_time_seconds", nullable=false)
    private long previousRoundTimeSeconds;

    @Column(name="round_detected_at", nullable=false)
    private Instant roundDetectedAt;

    @Column(name="last_observed_at", nullable=false)
    private Instant lastObservedAt;

    protected ServerStateEntity() {
    }

    public ServerStateEntity(
            UUID serverId,
            UUID roundInstanceId,
            String mapId,
            long previousRoundTimeSeconds,
            Instant roundDetectedAt,
            Instant lastObservedAt
    ) {
        this.serverId=serverId;
        this.roundInstanceId=roundInstanceId;
        this.mapId=mapId;
        this.previousRoundTimeSeconds=previousRoundTimeSeconds;
        this.roundDetectedAt=roundDetectedAt;
        this.lastObservedAt=lastObservedAt;
    }

    public UUID getServerId(){
        return serverId;
    }

    public UUID getRoundInstanceId(){
        return roundInstanceId;
    }

    public String getMapId(){
        return mapId;
    }

    public long getPreviousRoundTimeSeconds(){
        return previousRoundTimeSeconds;
    }

    public Instant getRoundDetectedAt(){
        return roundDetectedAt;
    }

    public Instant getLastObservedAt(){
        return lastObservedAt;
    }
}
