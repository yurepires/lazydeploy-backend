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

    @Column(name="subscription_id")
    private UUID subscriptionId;

    /**
     * Keeps ownership available when the subscription is later removed.
     * This is intentionally not exposed by the history API.
     */
    @Column(name="owner_user_id")
    private UUID ownerUserId;

    @Column(name="round_instance_id", nullable=false)
    private UUID roundInstanceId;

    @Column(name="server_id")
    private UUID serverId;

    @Column(name="server_display_name", length=255)
    private String serverDisplayName;

    @Column(name="map_id", length=255)
    private String mapId;

    @Column(name="map_display_name", length=255)
    private String mapDisplayName;

    @Column(name="player_count")
    private Integer playerCount;

    @Column(name="max_players")
    private Integer maxPlayers;

    @Column(name="game_mode", length=128)
    private String gameMode;

    @Column(name="recipient_snapshot", length=320)
    private String recipientSnapshot;

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
        this(
                id,
                subscriptionId,
                null,
                roundInstanceId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                channelType,
                status,
                attemptedAt,
                sentAt,
                errorCode,
                errorMessage,
                metadata
        );
    }

    public NotificationDeliveryAttemptEntity(
            UUID id,
            UUID subscriptionId,
            UUID ownerUserId,
            UUID roundInstanceId,
            UUID serverId,
            String serverDisplayName,
            String mapId,
            String mapDisplayName,
            Integer playerCount,
            Integer maxPlayers,
            String gameMode,
            String channelType,
            String status,
            Instant attemptedAt,
            Instant sentAt,
            String errorCode,
            String errorMessage,
            Map<String, Object> metadata
    ) {
        this(
                id,
                subscriptionId,
                ownerUserId,
                roundInstanceId,
                serverId,
                serverDisplayName,
                mapId,
                mapDisplayName,
                playerCount,
                maxPlayers,
                gameMode,
                null,
                channelType,
                status,
                attemptedAt,
                sentAt,
                errorCode,
                errorMessage,
                metadata
        );
    }

    public NotificationDeliveryAttemptEntity(
            UUID id,
            UUID subscriptionId,
            UUID ownerUserId,
            UUID roundInstanceId,
            UUID serverId,
            String serverDisplayName,
            String mapId,
            String mapDisplayName,
            Integer playerCount,
            Integer maxPlayers,
            String gameMode,
            String recipientSnapshot,
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
        this.ownerUserId=ownerUserId;
        this.roundInstanceId=roundInstanceId;
        this.serverId=serverId;
        this.serverDisplayName=serverDisplayName;
        this.mapId=mapId;
        this.mapDisplayName=mapDisplayName;
        this.playerCount=playerCount;
        this.maxPlayers=maxPlayers;
        this.gameMode=gameMode;
        this.recipientSnapshot=recipientSnapshot;
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

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public UUID getRoundInstanceId(){
        return roundInstanceId;
    }

    public UUID getServerId() {
        return serverId;
    }

    public String getServerDisplayName() {
        return serverDisplayName;
    }

    public String getMapId() {
        return mapId;
    }

    public String getMapDisplayName() {
        return mapDisplayName;
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

    public String getRecipientSnapshot() {
        return recipientSnapshot;
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
