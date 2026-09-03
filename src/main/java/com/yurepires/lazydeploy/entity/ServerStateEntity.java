package com.yurepires.lazydeploy.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "server_state")
public class ServerStateEntity {

    @Id
    @Column(name = "server_id")
    private UUID serverId;

    @Column(name = "round_instance_id", nullable = false)
    private UUID roundInstanceId;

    @Column(name = "map_id", nullable = false)
    private String mapId;

    @Column(name = "previous_round_time_seconds", nullable = false)
    private long previousRoundTimeSeconds;

    @Column(name = "round_detected_at", nullable = false)
    private Instant roundDetectedAt;

    @Column(name = "last_observed_at", nullable = false)
    private Instant lastObservedAt;

    @Column(name="player_count")
    private Integer playerCount;

    @Column(name="max_players")
    private Integer maxPlayers;

    @Column(name="game_mode")
    private String gameMode;

    protected ServerStateEntity() {
    }

    public ServerStateEntity(
            UUID serverId,
            UUID roundInstanceId,
            String mapId,
            long previousRoundTimeSeconds,
            Instant roundDetectedAt,
            Instant lastObservedAt,
            Integer playerCount,
            Integer maxPlayers,
            String gameMode
    ) {
        this.serverId = serverId;
        this.roundInstanceId = roundInstanceId;
        this.mapId = mapId;
        this.previousRoundTimeSeconds = previousRoundTimeSeconds;
        this.roundDetectedAt = roundDetectedAt;
        this.lastObservedAt = lastObservedAt;
        this.playerCount = playerCount;
        this.maxPlayers = maxPlayers;
        this.gameMode = gameMode;
    }

    public ServerStateEntity(
            UUID serverId,
            UUID roundInstanceId,
            String mapId,
            long previousRoundTimeSeconds,
            Instant roundDetectedAt,
            Instant lastObservedAt
    ) {
        this(
                serverId,
                roundInstanceId,
                mapId,
                previousRoundTimeSeconds,
                roundDetectedAt,
                lastObservedAt,
                null,
                null,
                null
        );
    }

    public UUID getServerId() {
        return serverId;
    }

    public UUID getRoundInstanceId() {
        return roundInstanceId;
    }

    public String getMapId() {
        return mapId;
    }

    public long getPreviousRoundTimeSeconds() {
        return previousRoundTimeSeconds;
    }

    public Instant getRoundDetectedAt() {
        return roundDetectedAt;
    }

    public Instant getLastObservedAt() {
        return lastObservedAt;
    }

    public Integer getPlayerCount() {
        return playerCount;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public String getGameMode() {
        return gameMode;
    }
}
