package com.yurepires.lazydeploy.mapper;

import com.yurepires.lazydeploy.entity.NotificationChannelConfigurationEntity;
import com.yurepires.lazydeploy.entity.NotificationChannelParameterEntity;
import com.yurepires.lazydeploy.entity.NotificationRuleEntity;
import com.yurepires.lazydeploy.entity.NotificationRuleParameterEntity;
import com.yurepires.lazydeploy.entity.ServerSubscriptionEntity;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.persistence.Server;
import com.yurepires.lazydeploy.model.persistence.ServerSubscription;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(config = LazyDeployMapperConfig.class)
public interface ServerSubscriptionMapper {

    @Mapping(target = "serverId", source = "server.id")
    ServerSubscriptionEntity toEntity(ServerSubscription subscription);

    @Mapping(target = "id", source = "subscriptionEntity.id")
    @Mapping(target = "userId", source = "subscriptionEntity.userId")
    @Mapping(target = "server", source = "server")
    @Mapping(target = "externalGuid", source = "externalGuid")
    @Mapping(target = "enabled", source = "subscriptionEntity.enabled")
    @Mapping(target = "rules", source = "rules")
    @Mapping(target = "channels", source = "channels")
    @Mapping(target = "createdAt", source = "subscriptionEntity.createdAt")
    @Mapping(target = "updatedAt", source = "subscriptionEntity.updatedAt")
    ServerSubscription toDomain(
            ServerSubscriptionEntity subscriptionEntity,
            Server server,
            String externalGuid,
            List<NotificationRuleDefinition> rules,
            List<NotificationChannelConfiguration> channels
    );

    @Mapping(target = "id", source = "definition.id")
    @Mapping(target = "subscriptionId", source = "subscriptionId")
    @Mapping(target = "type", source = "definition.type")
    @Mapping(target = "enabled", source = "definition.enabled")
    @Mapping(target = "createdAt", source = "currentTime")
    @Mapping(target = "updatedAt", source = "currentTime")
    NotificationRuleEntity toEntity(NotificationRuleDefinition definition, UUID subscriptionId, Instant currentTime);

    @Mapping(target = "id", source = "ruleEntity.id")
    @Mapping(target = "type", source = "ruleEntity.type")
    @Mapping(target = "enabled", source = "ruleEntity.enabled")
    @Mapping(target = "parameters", source = "parameters")
    NotificationRuleDefinition toDomain(NotificationRuleEntity ruleEntity, Map<String, Object> parameters);

    @Mapping(target = "id", source = "configuration.id")
    @Mapping(target = "subscriptionId", source = "subscriptionId")
    @Mapping(target = "type", source = "configuration.type")
    @Mapping(target = "enabled", source = "configuration.enabled")
    NotificationChannelConfigurationEntity toEntity(NotificationChannelConfiguration configuration, UUID subscriptionId);

    @Mapping(target = "id", source = "channelEntity.id")
    @Mapping(target = "type", source = "channelEntity.type")
    @Mapping(target = "enabled", source = "channelEntity.enabled")
    @Mapping(target = "parameters", source = "parameters")
    NotificationChannelConfiguration toDomain(NotificationChannelConfigurationEntity channelEntity, Map<String, Object> parameters);

    @Mapping(target = "id", source = "parameterId")
    @Mapping(target = "ruleId", source = "ruleId")
    @Mapping(target = "key", source = "key")
    @Mapping(target = "value", source = "encodedValue.value")
    @Mapping(target = "valueType", source = "encodedValue.type")
    NotificationRuleParameterEntity toRuleParameterEntity(
            UUID parameterId,
            UUID ruleId,
            String key,
            EncodedParameterValue encodedValue
    );

    @Mapping(target = "id", source = "parameterId")
    @Mapping(target = "channelConfigurationId", source = "channelConfigurationId")
    @Mapping(target = "key", source = "key")
    @Mapping(target = "value", source = "encodedValue.value")
    @Mapping(target = "valueType", source = "encodedValue.type")
    NotificationChannelParameterEntity toChannelParameterEntity(
            UUID parameterId,
            UUID channelConfigurationId,
            String key,
            EncodedParameterValue encodedValue
    );

}
