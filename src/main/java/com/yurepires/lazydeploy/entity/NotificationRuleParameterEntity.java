package com.yurepires.lazydeploy.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "notification_rule_parameter", uniqueConstraints = @UniqueConstraint(columnNames={"rule_id","parameter_key"}))
public class NotificationRuleParameterEntity {

    @Id
    private UUID id;

    @Column(name="rule_id", nullable=false)
    private UUID ruleId;

    @Column(name="parameter_key", nullable=false, length=128)
    private String key;

    @Column(name="parameter_value", nullable=false, columnDefinition="TEXT")
    private String value;

    @Column(name="value_type", nullable=false, length=32)
    private String valueType;

    protected NotificationRuleParameterEntity() {
    }

    public NotificationRuleParameterEntity(UUID id, UUID ruleId, String key, String value, String valueType) {
        this.id=id;
        this.ruleId=ruleId;
        this.key=key;
        this.value=value;
        this.valueType=valueType;
    }

    public UUID getId(){
        return id;
    }

    public UUID getRuleId(){
        return ruleId;
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
