package com.yurepires.lazydeploy.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="notification_channel_configuration")
public class NotificationChannelConfigurationEntity {

    @Id
    private UUID id;

    @Column(name="subscription_id", nullable=false)
    private UUID subscriptionId;

    @Column(nullable=false, length=64)
    private String type;

    @Column(nullable=false)
    private boolean enabled;

    protected NotificationChannelConfigurationEntity() {
    }

    public NotificationChannelConfigurationEntity(UUID id, UUID subscriptionId, String type, boolean enabled) {
        this.id=id;
        this.subscriptionId=subscriptionId;
        this.type=type;
        this.enabled=enabled;
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
}
