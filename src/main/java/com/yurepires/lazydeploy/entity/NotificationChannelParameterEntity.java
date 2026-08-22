package com.yurepires.lazydeploy.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="notification_channel_parameter", uniqueConstraints=@UniqueConstraint(columnNames={"channel_configuration_id","parameter_key"}))
public class NotificationChannelParameterEntity {
    @Id
    private UUID id;

    @Column(name="channel_configuration_id", nullable=false)
    private UUID channelConfigurationId;

    @Column(name="parameter_key", nullable=false, length=128)
    private String key;

    @Column(name="parameter_value", nullable=false, columnDefinition="TEXT")
    private String value;

    @Column(name="value_type", nullable=false, length=32)
    private String valueType;

    protected NotificationChannelParameterEntity() {
    }

    public NotificationChannelParameterEntity(UUID id, UUID channelConfigurationId, String key, String value, String valueType) {
        this.id=id;
        this.channelConfigurationId=channelConfigurationId;
        this.key=key;
        this.value=value;
        this.valueType=valueType;
    }

    public UUID getId(){
        return id;
    }

    public UUID getChannelConfigurationId(){
        return channelConfigurationId;
    }

    public String getKey(){
        return key;
    }

    public String getValue(){
        return value;
    }

    public String getValueType(){
        return valueType;
    }
}
