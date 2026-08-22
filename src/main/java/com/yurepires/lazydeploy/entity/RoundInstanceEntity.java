package com.yurepires.lazydeploy.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="round_instance")
public class RoundInstanceEntity {

    @Id
    private UUID id;

    @Column(name="server_id", nullable=false)
    private UUID serverId;

    @Column(name="map_id", nullable=false)
    private String mapId;

    @Column(name="detected_at", nullable=false)
    private Instant detectedAt;

    @Column(name="first_observed_round_time", nullable=false)
    private long firstObservedRoundTime;

    @Column(name="ended_at")
    private Instant endedAt;

    protected RoundInstanceEntity() {
    }

    public RoundInstanceEntity(
            UUID id,
            UUID serverId,
            String mapId,
            Instant detectedAt,
            long firstObservedRoundTime,
            Instant endedAt
    ) {
        this.id=id;
        this.serverId=serverId;
        this.mapId=mapId;
        this.detectedAt=detectedAt;
        this.firstObservedRoundTime=firstObservedRoundTime;
        this.endedAt=endedAt;
    }

    public UUID getId(){
        return id;
    }

    public UUID getServerId(){
        return serverId;
    }

    public String getMapId(){
        return mapId;
    }

    public Instant getDetectedAt(){
        return detectedAt;
    }

    public long getFirstObservedRoundTime(){
        return firstObservedRoundTime;
    }

    public Instant getEndedAt(){
        return endedAt;
    }
}
