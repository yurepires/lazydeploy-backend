package com.yurepires.lazydeploy.service.subscription;

import com.yurepires.lazydeploy.entity.NotificationChannelConfigurationEntity;
import com.yurepires.lazydeploy.entity.NotificationDeliveryAttemptEntity;
import com.yurepires.lazydeploy.entity.NotificationRuleEntity;
import com.yurepires.lazydeploy.entity.ServerEntity;
import com.yurepires.lazydeploy.entity.ServerIdentifierEntity;
import com.yurepires.lazydeploy.entity.ServerSubscriptionEntity;
import com.yurepires.lazydeploy.exception.NotificationRuleNotFoundException;
import com.yurepires.lazydeploy.exception.PersistenceMappingException;
import com.yurepires.lazydeploy.mapper.EncodedParameterValue;
import com.yurepires.lazydeploy.mapper.ParameterValueMapper;
import com.yurepires.lazydeploy.mapper.ServerMapper;
import com.yurepires.lazydeploy.mapper.ServerSubscriptionMapper;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.persistence.ExternalIdentifierTypes;
import com.yurepires.lazydeploy.model.persistence.Server;
import com.yurepires.lazydeploy.model.persistence.ServerSubscription;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.repository.NotificationChannelConfigurationRepository;
import com.yurepires.lazydeploy.repository.NotificationChannelParameterRepository;
import com.yurepires.lazydeploy.repository.NotificationDeliveryAttemptRepository;
import com.yurepires.lazydeploy.repository.NotificationRuleParameterRepository;
import com.yurepires.lazydeploy.repository.NotificationRuleRepository;
import com.yurepires.lazydeploy.repository.NotificationStateRepository;
import com.yurepires.lazydeploy.repository.ServerIdentifierRepository;
import com.yurepires.lazydeploy.repository.ServerRepository;
import com.yurepires.lazydeploy.repository.ServerSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServerSubscriptionPersistenceService {

    private final ServerSubscriptionRepository subscriptionRepository;
    private final ServerRepository serverRepository;
    private final ServerIdentifierRepository identifierRepository;
    private final NotificationRuleRepository ruleRepository;
    private final NotificationRuleParameterRepository ruleParameterRepository;
    private final NotificationChannelConfigurationRepository channelRepository;
    private final NotificationChannelParameterRepository channelParameterRepository;
    private final NotificationStateRepository notificationStateRepository;
    private final NotificationDeliveryAttemptRepository deliveryAttemptRepository;
    private final ServerSubscriptionMapper subscriptionMapper;
    private final ServerMapper serverMapper;
    private final ParameterValueMapper parameterValueMapper;

    public ServerSubscriptionPersistenceService(
            ServerSubscriptionRepository subscriptionRepository,
            ServerRepository serverRepository,
            ServerIdentifierRepository identifierRepository,
            NotificationRuleRepository ruleRepository,
            NotificationRuleParameterRepository ruleParameterRepository,
            NotificationChannelConfigurationRepository channelRepository,
            NotificationChannelParameterRepository channelParameterRepository,
            NotificationStateRepository notificationStateRepository,
            ServerSubscriptionMapper subscriptionMapper,
            ServerMapper serverMapper,
            ParameterValueMapper parameterValueMapper
    ) {
        this(
                subscriptionRepository,
                serverRepository,
                identifierRepository,
                ruleRepository,
                ruleParameterRepository,
                channelRepository,
                channelParameterRepository,
                notificationStateRepository,
                null,
                subscriptionMapper,
                serverMapper,
                parameterValueMapper
        );
    }

    @Autowired
    public ServerSubscriptionPersistenceService(
            ServerSubscriptionRepository subscriptionRepository,
            ServerRepository serverRepository,
            ServerIdentifierRepository identifierRepository,
            NotificationRuleRepository ruleRepository,
            NotificationRuleParameterRepository ruleParameterRepository,
            NotificationChannelConfigurationRepository channelRepository,
            NotificationChannelParameterRepository channelParameterRepository,
            NotificationStateRepository notificationStateRepository,
            NotificationDeliveryAttemptRepository deliveryAttemptRepository,
            ServerSubscriptionMapper subscriptionMapper,
            ServerMapper serverMapper,
            ParameterValueMapper parameterValueMapper
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.serverRepository = serverRepository;
        this.identifierRepository = identifierRepository;
        this.ruleRepository = ruleRepository;
        this.ruleParameterRepository = ruleParameterRepository;
        this.channelRepository = channelRepository;
        this.channelParameterRepository = channelParameterRepository;
        this.notificationStateRepository = notificationStateRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.subscriptionMapper = subscriptionMapper;
        this.serverMapper = serverMapper;
        this.parameterValueMapper = parameterValueMapper;
    }

    @Transactional(readOnly = true)
    public List<ServerSubscription> findAllEnabled() {
        return subscriptionRepository.findAllByEnabledTrue()
                .stream()
                .map(this::mapSubscriptionToDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServerSubscription> findAllByUserId(UUID userId) {
        return subscriptionRepository.findAllByUserId(userId)
                .stream()
                .map(this::mapSubscriptionToDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ServerSubscription> findByIdAndUserId(UUID subscriptionId, UUID userId) {
        return subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .map(this::mapSubscriptionToDomain);
    }

    @Transactional(readOnly = true)
    public Optional<ServerSubscription> findByUserIdAndServerId(UUID userId, UUID serverId) {
        return subscriptionRepository.findByUserIdAndServerId(userId, serverId)
                .map(this::mapSubscriptionToDomain);
    }

    @Transactional(readOnly = true)
    public boolean existsByUserIdAndServerId(UUID userId, UUID serverId) {
        return subscriptionRepository.existsByUserIdAndServerId(userId, serverId);
    }

    @Transactional
    public ServerSubscription save(ServerSubscription subscription) {
        ServerSubscriptionEntity subscriptionEntity = subscriptionMapper.toEntity(subscription);
        ServerSubscriptionEntity savedEntity = subscriptionRepository.saveAndFlush(subscriptionEntity);

        replaceRules(savedEntity.getId(), subscription.rules());
        replaceChannels(savedEntity.getId(), subscription.channels());

        return mapSubscriptionToDomain(savedEntity);
    }

    @Transactional
    public ServerSubscription saveSubscription(ServerSubscription subscription) {
        ServerSubscriptionEntity subscriptionEntity = subscriptionMapper.toEntity(subscription);
        ServerSubscriptionEntity savedEntity = subscriptionRepository.saveAndFlush(subscriptionEntity);
        return mapSubscriptionToDomain(savedEntity);
    }

    @Transactional
    public ServerSubscription saveRules(
            ServerSubscription currentSubscription,
            List<NotificationRuleDefinition> rules
    ) {
        ServerSubscriptionEntity subscriptionEntity = subscriptionMapper.toEntity(currentSubscription);
        ServerSubscriptionEntity savedEntity = subscriptionRepository.saveAndFlush(subscriptionEntity);
        replaceRules(savedEntity.getId(), rules);

        return mapSubscriptionToDomain(savedEntity);
    }

    @Transactional
    public ServerSubscription saveRule(
            ServerSubscription currentSubscription,
            NotificationRuleDefinition ruleDefinition
    ) {
        NotificationRuleEntity existingRule = ruleRepository.findById(ruleDefinition.id())
                .orElseThrow(NotificationRuleNotFoundException::new);

        if (!currentSubscription.id().equals(existingRule.getSubscriptionId())) {
            throw new NotificationRuleNotFoundException();
        }

        Instant currentTime = Instant.now();
        NotificationRuleEntity ruleEntity = subscriptionMapper.toEntity(
                ruleDefinition,
                currentSubscription.id(),
                existingRule.getCreatedAt(),
                currentTime
        );

        ruleParameterRepository.deleteAllByRuleIdIn(List.of(ruleDefinition.id()));
        ruleRepository.saveAndFlush(ruleEntity);
        saveRuleParameters(ruleEntity.getId(), ruleDefinition.parameters());
        subscriptionRepository.updateUpdatedAt(currentSubscription.id(), currentTime);

        ServerSubscriptionEntity subscriptionEntity = subscriptionRepository
                .findById(currentSubscription.id())
                .orElseThrow(() -> new PersistenceMappingException(
                        "Inscrição não foi encontrada: " + currentSubscription.id()
                ));
        return mapSubscriptionToDomain(subscriptionEntity);
    }

    @Transactional
    public ServerSubscription saveChannels(
            ServerSubscription currentSubscription,
            List<NotificationChannelConfiguration> channels
    ) {
        ServerSubscriptionEntity subscriptionEntity = subscriptionMapper.toEntity(currentSubscription);
        ServerSubscriptionEntity savedEntity = subscriptionRepository.saveAndFlush(subscriptionEntity);
        replaceChannels(savedEntity.getId(), channels);

        return mapSubscriptionToDomain(savedEntity);
    }

    @Transactional
    public void delete(UUID subscriptionId, UUID userId) {
        Optional<ServerSubscriptionEntity> subscription = subscriptionRepository
                .findByIdAndUserId(subscriptionId, userId);
        if (subscription.isEmpty()) {
            return;
        }

        deleteExistingRules(subscriptionId);
        deleteExistingChannels(subscriptionId);
        preserveDeliveryAttemptOwnership(subscriptionId, userId);
        notificationStateRepository.deleteAllBySubscriptionId(subscriptionId);
        subscriptionRepository.delete(subscription.get());
    }

    private void preserveDeliveryAttemptOwnership(UUID subscriptionId, UUID userId) {
        if (deliveryAttemptRepository == null) {
            return;
        }

        List<NotificationDeliveryAttemptEntity> attempts = deliveryAttemptRepository
                .findAllBySubscriptionId(subscriptionId);
        if (attempts.isEmpty()) {
            return;
        }

        List<NotificationDeliveryAttemptEntity> ownedAttempts = attempts.stream()
                .map(attempt -> new NotificationDeliveryAttemptEntity(
                        attempt.getId(),
                        attempt.getSubscriptionId(),
                        userId,
                        attempt.getRoundInstanceId(),
                        attempt.getServerId(),
                        attempt.getServerDisplayName(),
                        attempt.getMapId(),
                        attempt.getMapDisplayName(),
                        attempt.getPlayerCount(),
                        attempt.getMaxPlayers(),
                        attempt.getGameMode(),
                        attempt.getRecipientSnapshot(),
                        attempt.getChannelType(),
                        attempt.getStatus(),
                        attempt.getAttemptedAt(),
                        attempt.getSentAt(),
                        attempt.getErrorCode(),
                        attempt.getErrorMessage(),
                        attempt.getMetadata()
                ))
                .toList();

        deliveryAttemptRepository.saveAll(ownedAttempts);
    }

    private void replaceRules(
            UUID subscriptionId,
            List<NotificationRuleDefinition> ruleDefinitions
    ) {
        deleteExistingRules(subscriptionId);
        Instant currentTime = Instant.now();

        for (NotificationRuleDefinition ruleDefinition : ruleDefinitions) {
            NotificationRuleDefinition definitionWithId = ensureRuleId(ruleDefinition);
            Instant createdAt = definitionWithId.createdAt();
            if (createdAt == null) {
                createdAt = currentTime;
            }

            NotificationRuleEntity ruleEntity = subscriptionMapper.toEntity(
                    definitionWithId,
                    subscriptionId,
                    createdAt,
                    currentTime
            );
            ruleRepository.save(ruleEntity);
            saveRuleParameters(ruleEntity.getId(), definitionWithId.parameters());
        }
    }

    private NotificationRuleDefinition ensureRuleId(NotificationRuleDefinition definition) {
        if (definition.id() != null) {
            return definition;
        }

        return new NotificationRuleDefinition(
                UUID.randomUUID(),
                definition.type(),
                definition.enabled(),
                definition.parameters(),
                definition.createdAt(),
                definition.updatedAt()
        );
    }

    private void deleteExistingRules(UUID subscriptionId) {
        List<NotificationRuleEntity> existingRules = ruleRepository
                .findAllBySubscriptionId(subscriptionId);

        if (!existingRules.isEmpty()) {
            List<UUID> ruleIds = existingRules.stream()
                    .map(NotificationRuleEntity::getId)
                    .toList();
            ruleParameterRepository.deleteAllByRuleIdIn(ruleIds);
        }

        ruleRepository.deleteAllBySubscriptionId(subscriptionId);
    }

    private void saveRuleParameters(UUID ruleId, Map<String, Object> parameters) {
        for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
            EncodedParameterValue encodedValue = parameterValueMapper.encode(parameter.getValue());
            ruleParameterRepository.save(subscriptionMapper.toRuleParameterEntity(
                    UUID.randomUUID(),
                    ruleId,
                    parameter.getKey(),
                    encodedValue
            ));
        }
    }

    private void replaceChannels(
            UUID subscriptionId,
            List<NotificationChannelConfiguration> channelConfigurations
    ) {
        deleteExistingChannels(subscriptionId);
        Instant currentTime = Instant.now();

        for (NotificationChannelConfiguration channelConfiguration : channelConfigurations) {
            NotificationChannelConfiguration configurationWithId = ensureChannelId(
                    channelConfiguration
            );
            NotificationChannelConfigurationEntity channelEntity = subscriptionMapper.toEntity(
                    configurationWithId,
                    subscriptionId,
                    createdAtFor(configurationWithId, currentTime),
                    currentTime
            );
            channelRepository.save(channelEntity);
            saveChannelParameters(channelEntity.getId(), configurationWithId.parameters());
        }
    }

    private Instant createdAtFor(
            NotificationChannelConfiguration configuration,
            Instant fallback
    ) {
        if (configuration.createdAt() == null) {
            return fallback;
        }

        return configuration.createdAt();
    }

    private NotificationChannelConfiguration ensureChannelId(
            NotificationChannelConfiguration configuration
    ) {
        if (configuration.id() != null) {
            return configuration;
        }

        return new NotificationChannelConfiguration(
                UUID.randomUUID(),
                configuration.type(),
                configuration.enabled(),
                configuration.parameters(),
                configuration.createdAt(),
                configuration.updatedAt()
        );
    }

    private void deleteExistingChannels(UUID subscriptionId) {
        List<NotificationChannelConfigurationEntity> existingChannels = channelRepository
                .findAllBySubscriptionId(subscriptionId);

        if (!existingChannels.isEmpty()) {
            List<UUID> channelIds = existingChannels.stream()
                    .map(NotificationChannelConfigurationEntity::getId)
                    .toList();
            channelParameterRepository.deleteAllByChannelConfigurationIdIn(channelIds);
        }

        channelRepository.deleteAllBySubscriptionId(subscriptionId);
    }

    private void saveChannelParameters(UUID channelId, Map<String, Object> parameters) {
        for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
            EncodedParameterValue encodedValue = parameterValueMapper.encode(parameter.getValue());
            channelParameterRepository.save(subscriptionMapper.toChannelParameterEntity(
                    UUID.randomUUID(),
                    channelId,
                    parameter.getKey(),
                    encodedValue
            ));
        }
    }

    private ServerSubscription mapSubscriptionToDomain(
            ServerSubscriptionEntity subscriptionEntity
    ) {
        Server server = findServer(subscriptionEntity.getServerId());
        String externalGuid = findServerGuid(subscriptionEntity.getServerId());
        List<NotificationRuleDefinition> rules = loadRules(subscriptionEntity.getId());
        List<NotificationChannelConfiguration> channels = loadChannels(subscriptionEntity.getId());

        return subscriptionMapper.toDomain(
                subscriptionEntity,
                server,
                externalGuid,
                rules,
                channels
        );
    }

    private Server findServer(UUID serverId) {
        ServerEntity serverEntity = serverRepository.findById(serverId)
                .orElseThrow(() -> new PersistenceMappingException(
                        "Servidor da inscrição não foi encontrado: " + serverId
                ));
        return serverMapper.toDomain(serverEntity);
    }

    private String findServerGuid(UUID serverId) {
        return identifierRepository.findByServerIdAndProviderAndType(
                        serverId,
                        ExternalIdentifierTypes.BATTLELOG,
                        ExternalIdentifierTypes.GUID
                )
                .map(ServerIdentifierEntity::getValue)
                .orElseThrow(() -> new PersistenceMappingException(
                        "GUID do servidor não foi encontrado: " + serverId
                ));
    }

    private List<NotificationRuleDefinition> loadRules(UUID subscriptionId) {
        List<NotificationRuleDefinition> rules = new ArrayList<>();

        for (NotificationRuleEntity ruleEntity : ruleRepository.findAllBySubscriptionId(subscriptionId)) {
            Map<String, Object> parameters = loadRuleParameters(ruleEntity.getId());
            rules.add(subscriptionMapper.toDomain(ruleEntity, parameters));
        }

        return List.copyOf(rules);
    }

    private Map<String, Object> loadRuleParameters(UUID ruleId) {
        Map<String, Object> parameters = new LinkedHashMap<>();

        ruleParameterRepository.findAllByRuleId(ruleId).forEach(parameterEntity -> {
            Object decodedValue = parameterValueMapper.decode(
                    parameterEntity.getValue(),
                    parameterEntity.getValueType()
            );
            parameters.put(parameterEntity.getKey(), decodedValue);
        });

        return parameters;
    }

    private List<NotificationChannelConfiguration> loadChannels(UUID subscriptionId) {
        List<NotificationChannelConfiguration> channels = new ArrayList<>();

        for (NotificationChannelConfigurationEntity channelEntity
                : channelRepository.findAllBySubscriptionId(subscriptionId)) {
            Map<String, Object> parameters = loadChannelParameters(channelEntity.getId());
            channels.add(subscriptionMapper.toDomain(channelEntity, parameters));
        }

        return List.copyOf(channels);
    }

    private Map<String, Object> loadChannelParameters(UUID channelId) {
        Map<String, Object> parameters = new LinkedHashMap<>();

        channelParameterRepository.findAllByChannelConfigurationId(channelId)
                .forEach(parameterEntity -> {
                    Object decodedValue = parameterValueMapper.decode(
                            parameterEntity.getValue(),
                            parameterEntity.getValueType()
                    );
                    parameters.put(parameterEntity.getKey(), decodedValue);
                });

        return parameters;
    }
}
