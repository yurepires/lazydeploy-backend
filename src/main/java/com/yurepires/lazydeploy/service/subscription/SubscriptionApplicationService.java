package com.yurepires.lazydeploy.service.subscription;

import com.yurepires.lazydeploy.dto.request.CreateChannelRequest;
import com.yurepires.lazydeploy.dto.request.CreateRuleRequest;
import com.yurepires.lazydeploy.dto.request.CreateSubscriptionRequest;
import com.yurepires.lazydeploy.dto.request.NotificationChannelRequest;
import com.yurepires.lazydeploy.dto.request.NotificationRuleRequest;
import com.yurepires.lazydeploy.dto.request.PatchChannelRequest;
import com.yurepires.lazydeploy.dto.request.PatchRuleRequest;
import com.yurepires.lazydeploy.dto.request.SubscriptionCreationRequest;
import com.yurepires.lazydeploy.dto.request.UpdateChannelRequest;
import com.yurepires.lazydeploy.dto.request.UpdateRuleRequest;
import com.yurepires.lazydeploy.entity.UserEntity;
import com.yurepires.lazydeploy.exception.ChannelAlreadyExistsException;
import com.yurepires.lazydeploy.exception.InvalidChannelConfigurationException;
import com.yurepires.lazydeploy.exception.InvalidRequestException;
import com.yurepires.lazydeploy.exception.NotificationChannelNotFoundException;
import com.yurepires.lazydeploy.exception.NotificationRuleNotFoundException;
import com.yurepires.lazydeploy.exception.SubscriptionAlreadyExistsException;
import com.yurepires.lazydeploy.exception.SubscriptionNotFoundException;
import com.yurepires.lazydeploy.exception.UserNotFoundException;
import com.yurepires.lazydeploy.mapper.ServerMapper;
import com.yurepires.lazydeploy.mapper.UserMapper;
import com.yurepires.lazydeploy.model.notification.NotificationChannelConfiguration;
import com.yurepires.lazydeploy.model.persistence.ExternalIdentifierTypes;
import com.yurepires.lazydeploy.model.persistence.Server;
import com.yurepires.lazydeploy.model.persistence.ServerSubscription;
import com.yurepires.lazydeploy.model.persistence.User;
import com.yurepires.lazydeploy.model.rule.NotificationRuleDefinition;
import com.yurepires.lazydeploy.repository.ServerRepository;
import com.yurepires.lazydeploy.repository.ServerIdentifierRepository;
import com.yurepires.lazydeploy.repository.UserRepository;
import com.yurepires.lazydeploy.service.validation.channel.ChannelConfigurationValidatorRegistry;
import com.yurepires.lazydeploy.service.validation.BusinessLimitService;
import com.yurepires.lazydeploy.service.validation.rule.RuleDefinitionValidatorRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class SubscriptionApplicationService {

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final ServerIdentifierRepository identifierRepository;
    private final ServerSubscriptionPersistenceService subscriptionPersistenceService;
    private final NewServerTransaction newServerTransaction;
    private final UserMapper userMapper;
    private final ServerMapper serverMapper;
    private final Clock clock;
    private final RuleDefinitionValidatorRegistry ruleValidatorRegistry;
    private final ChannelConfigurationValidatorRegistry channelValidatorRegistry;
    private final BusinessLimitService businessLimitService;
    // Stripes fixas mantêm a serialização por usuário sem crescimento ilimitado
    // de objetos de lock quando IDs novos são recebidos.
    private static final int USER_LOCK_STRIPES = 64;
    private final Object[] subscriptionLocks = createLocks(USER_LOCK_STRIPES);

    public SubscriptionApplicationService(
            UserRepository userRepository,
            ServerRepository serverRepository,
            ServerIdentifierRepository identifierRepository,
            ServerSubscriptionPersistenceService subscriptionPersistenceService,
            NewServerTransaction newServerTransaction,
            UserMapper userMapper,
            ServerMapper serverMapper,
            Clock clock,
            RuleDefinitionValidatorRegistry ruleValidatorRegistry,
            ChannelConfigurationValidatorRegistry channelValidatorRegistry,
            BusinessLimitService businessLimitService
    ) {
        this.userRepository = userRepository;
        this.serverRepository = serverRepository;
        this.identifierRepository = identifierRepository;
        this.subscriptionPersistenceService = subscriptionPersistenceService;
        this.newServerTransaction = newServerTransaction;
        this.userMapper = userMapper;
        this.serverMapper = serverMapper;
        this.clock = clock;
        this.ruleValidatorRegistry = ruleValidatorRegistry;
        this.channelValidatorRegistry = channelValidatorRegistry;
        this.businessLimitService = businessLimitService;
    }

    @Transactional
    public ServerSubscription create(UUID userId, SubscriptionCreationRequest request) {
        businessLimitService.validateRuleCollectionSize(request.rules().size());
        businessLimitService.validateChannelCollectionSize(request.channels().size());

        List<NotificationRuleDefinition> rules = createRuleDefinitions(request.rules());
        List<NotificationChannelConfiguration> channels = createChannelConfigurations(
                request.channels()
        );

        ServerSubscription subscription = create(
                userId,
                new CreateSubscriptionRequest(
                        request.serverGuid(),
                        request.displayName(),
                        request.enabled()
                )
        );

        if (request.rules().isEmpty() && request.channels().isEmpty()) {
            return subscription;
        }

        ServerSubscription configuredSubscription = new ServerSubscription(
                subscription.id(),
                subscription.userId(),
                subscription.server(),
                subscription.externalGuid(),
                subscription.enabled(),
                rules,
                channels,
                subscription.createdAt(),
                subscription.updatedAt()
        );

        return subscriptionPersistenceService.save(configuredSubscription);
    }

    @Transactional
    public ServerSubscription create(UUID userId, CreateSubscriptionRequest request) {
        User user = findEnabledUserForCreation(userId);
        Object userLock = lockFor(user.id());

        synchronized (userLock) {
            businessLimitService.ensureSubscriptionCapacity(user.id());

            Server server = resolveServer(request.serverGuid(), request.displayName());

            boolean subscriptionAlreadyExists = subscriptionPersistenceService
                    .existsByUserIdAndServerId(user.id(), server.id());

            if (subscriptionAlreadyExists) {
                throw new SubscriptionAlreadyExistsException();
            }

            ServerSubscription subscription = createSubscription(user, server, request);

            try {
                return subscriptionPersistenceService.save(subscription);
            } catch (DataIntegrityViolationException concurrentCreationException) {
                throw new SubscriptionAlreadyExistsException();
            }
        }
    }

    public List<ServerSubscription> list(UUID userId) {
        return subscriptionPersistenceService.findAllByUserId(userId);
    }

    private Object lockFor(UUID userId) {
        int index = Math.floorMod(userId.hashCode(), subscriptionLocks.length);
        return subscriptionLocks[index];
    }

    private static Object[] createLocks(int lockCount) {
        Object[] locks = new Object[lockCount];
        for (int index = 0; index < lockCount; index++) {
            locks[index] = new Object();
        }
        return locks;
    }

    public ServerSubscription get(UUID userId, UUID subscriptionId) {
        return subscriptionPersistenceService
                .findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(SubscriptionNotFoundException::new);
    }

    public ServerSubscription update(
            UUID userId,
            UUID subscriptionId,
            Boolean enabled
    ) {
        if (enabled == null) {
            throw new InvalidRequestException(
                    "O campo enabled deve ser informado"
            );
        }

        ServerSubscription currentSubscription = get(userId, subscriptionId);
        ServerSubscription updatedSubscription = new ServerSubscription(
                currentSubscription.id(),
                currentSubscription.userId(),
                currentSubscription.server(),
                currentSubscription.externalGuid(),
                enabled,
                currentSubscription.rules(),
                currentSubscription.channels(),
                currentSubscription.createdAt(),
                Instant.now(clock)
        );

        return subscriptionPersistenceService.saveSubscription(updatedSubscription);
    }

    public void delete(UUID userId, UUID subscriptionId) {
        get(userId, subscriptionId);
        subscriptionPersistenceService.delete(subscriptionId, userId);
    }

    public ServerSubscription addRule(
            UUID userId,
            UUID subscriptionId,
            NotificationRuleRequest request
    ) {
        return addRule(
                userId,
                subscriptionId,
                new CreateRuleRequest(
                        request.type(),
                        request.enabled(),
                        request.parameters()
                )
        );
    }

    public ServerSubscription addRule(
            UUID userId,
            UUID subscriptionId,
            CreateRuleRequest request
    ) {
        return addRuleWithId(
                userId,
                subscriptionId,
                UUID.randomUUID(),
                request
        );
    }

    public NotificationRuleDefinition createRule(
            UUID userId,
            UUID subscriptionId,
            CreateRuleRequest request
    ) {
        UUID ruleId = UUID.randomUUID();
        addRuleWithId(userId, subscriptionId, ruleId, request);
        return getRule(userId, subscriptionId, ruleId);
    }

    private ServerSubscription addRuleWithId(
            UUID userId,
            UUID subscriptionId,
            UUID ruleId,
            CreateRuleRequest request
    ) {
        ServerSubscription currentSubscription = get(userId, subscriptionId);
        businessLimitService.ensureRuleCapacity(currentSubscription.rules().size());
        List<NotificationRuleDefinition> updatedRules = new ArrayList<>(currentSubscription.rules());
        updatedRules.add(createRuleDefinition(
                ruleId,
                request.type(),
                request.enabled(),
                request.parameters()
        ));

        return saveWithRules(currentSubscription, updatedRules);
    }

    public ServerSubscription updateRule(
            UUID userId,
            UUID subscriptionId,
            UUID ruleId,
            NotificationRuleRequest request
    ) {
        return updateRule(
                userId,
                subscriptionId,
                ruleId,
                new UpdateRuleRequest(
                        request.type(),
                        request.enabled(),
                        request.parameters()
                )
        );
    }

    public ServerSubscription updateRule(
            UUID userId,
            UUID subscriptionId,
            UUID ruleId,
            UpdateRuleRequest request
    ) {
        ServerSubscription currentSubscription = get(userId, subscriptionId);
        NotificationRuleDefinition updatedRule = null;

        for (NotificationRuleDefinition currentRule : currentSubscription.rules()) {
            if (ruleId.equals(currentRule.id())) {
                updatedRule = createRuleDefinition(
                        ruleId,
                        request.type(),
                        request.enabled(),
                        request.parameters(),
                        currentRule.createdAt(),
                        currentRule.updatedAt()
                );
                break;
            }
        }

        if (updatedRule == null) {
            throw new NotificationRuleNotFoundException();
        }

        return subscriptionPersistenceService.saveRule(currentSubscription, updatedRule);
    }

    public ServerSubscription patchRule(
            UUID userId,
            UUID subscriptionId,
            UUID ruleId,
            PatchRuleRequest request
    ) {
        if (request.enabled() == null) {
            throw new InvalidRequestException(
                    "O campo enabled deve ser informado"
            );
        }

        ServerSubscription currentSubscription = get(userId, subscriptionId);
        NotificationRuleDefinition updatedRule = null;

        for (NotificationRuleDefinition currentRule : currentSubscription.rules()) {
            if (ruleId.equals(currentRule.id())) {
                String updatedType = currentRule.type();
                if (request.type() != null) {
                    updatedType = request.type();
                }

                Map<String, Object> updatedParameters = currentRule.parameters();
                if (request.parameters() != null) {
                    updatedParameters = request.parameters();
                }

                updatedRule = createRuleDefinition(
                        ruleId,
                        updatedType,
                        request.enabled(),
                        updatedParameters,
                        currentRule.createdAt(),
                        currentRule.updatedAt()
                );
                break;
            }
        }

        if (updatedRule == null) {
            throw new NotificationRuleNotFoundException();
        }

        return subscriptionPersistenceService.saveRule(currentSubscription, updatedRule);
    }

    public ServerSubscription addChannel(
            UUID userId,
            UUID subscriptionId,
            NotificationChannelRequest request
    ) {
        return addChannel(
                userId,
                subscriptionId,
                new CreateChannelRequest(
                        request.type(),
                        request.enabled(),
                        request.parameters(),
                        request.recipient()
                )
        );
    }

    public ServerSubscription addChannel(
            UUID userId,
            UUID subscriptionId,
            CreateChannelRequest request
    ) {
        return addChannelWithId(
                userId,
                subscriptionId,
                UUID.randomUUID(),
                request
        );
    }

    public NotificationChannelConfiguration createChannel(
            UUID userId,
            UUID subscriptionId,
            CreateChannelRequest request
    ) {
        UUID channelId = UUID.randomUUID();
        addChannelWithId(userId, subscriptionId, channelId, request);
        return getChannel(userId, subscriptionId, channelId);
    }

    private ServerSubscription addChannelWithId(
            UUID userId,
            UUID subscriptionId,
            UUID channelId,
            CreateChannelRequest request
    ) {
        ServerSubscription currentSubscription = get(userId, subscriptionId);
        businessLimitService.ensureChannelCapacity(currentSubscription.channels().size());
        String normalizedType = channelValidatorRegistry.normalizeType(request.type());
        rejectRootRecipient(normalizedType, request.recipient());
        channelValidatorRegistry.validate(normalizedType, request.parameters());

        boolean typeAlreadyExists = currentSubscription.channels().stream()
                .anyMatch(channel -> channel.type().equalsIgnoreCase(normalizedType));
        if (typeAlreadyExists) {
            throw new ChannelAlreadyExistsException();
        }

        List<NotificationChannelConfiguration> updatedChannels = new ArrayList<>(currentSubscription.channels());
        updatedChannels.add(createChannelConfiguration(
                channelId,
                request.type(),
                request.enabled(),
                request.parameters()
        ));

        return saveWithChannels(currentSubscription, updatedChannels);
    }

    public ServerSubscription updateChannel(
            UUID userId,
            UUID subscriptionId,
            UUID channelId,
            NotificationChannelRequest request
    ) {
        return updateChannel(
                userId,
                subscriptionId,
                channelId,
                new UpdateChannelRequest(
                        request.type(),
                        request.enabled(),
                        request.parameters(),
                        request.recipient()
                )
        );
    }

    public ServerSubscription updateChannel(
            UUID userId,
            UUID subscriptionId,
            UUID channelId,
            UpdateChannelRequest request
    ) {
        ServerSubscription currentSubscription = get(userId, subscriptionId);
        String normalizedType = channelValidatorRegistry.normalizeType(request.type());
        NotificationChannelConfiguration channelToUpdate = currentSubscription.channels().stream()
                .filter(channel -> channelId.equals(channel.id()))
                .findFirst()
                .orElseThrow(NotificationChannelNotFoundException::new);
        rejectRootRecipient(normalizedType, request.recipient());
        channelValidatorRegistry.validate(normalizedType, request.parameters());

        boolean anotherChannelUsesType = currentSubscription.channels().stream()
                .anyMatch(channel -> !channel.id().equals(channelToUpdate.id())
                        && channel.type().equalsIgnoreCase(normalizedType));
        if (anotherChannelUsesType) {
            throw new ChannelAlreadyExistsException();
        }

        List<NotificationChannelConfiguration> updatedChannels = new ArrayList<>();

        for (NotificationChannelConfiguration currentChannel : currentSubscription.channels()) {
            if (channelId.equals(currentChannel.id())) {
                updatedChannels.add(createChannelConfiguration(
                        channelId,
                        normalizedType,
                        request.enabled(),
                        request.parameters()
                ));
            } else {
                updatedChannels.add(currentChannel);
            }
        }

        return saveWithChannels(currentSubscription, updatedChannels);
    }

    public ServerSubscription patchChannel(
            UUID userId,
            UUID subscriptionId,
            UUID channelId,
            PatchChannelRequest request
    ) {
        if (request.enabled() == null) {
            throw new InvalidRequestException(
                    "O campo enabled deve ser informado"
            );
        }

        ServerSubscription currentSubscription = get(userId, subscriptionId);
        List<NotificationChannelConfiguration> updatedChannels = new ArrayList<>();
        boolean channelWasFound = false;

        for (NotificationChannelConfiguration currentChannel : currentSubscription.channels()) {
            if (channelId.equals(currentChannel.id())) {
                updatedChannels.add(new NotificationChannelConfiguration(
                        channelId,
                        currentChannel.type(),
                        request.enabled(),
                        currentChannel.parameters(),
                        currentChannel.createdAt(),
                        currentChannel.updatedAt()
                ));
                channelWasFound = true;
            } else {
                updatedChannels.add(currentChannel);
            }
        }

        if (!channelWasFound) {
            throw new NotificationChannelNotFoundException();
        }

        return saveWithChannels(currentSubscription, updatedChannels);
    }

    public ServerSubscription deleteChannel(
            UUID userId,
            UUID subscriptionId,
            UUID channelId
    ) {
        ServerSubscription currentSubscription = get(userId, subscriptionId);
        List<NotificationChannelConfiguration> remainingChannels = new ArrayList<>();
        boolean channelWasFound = false;

        for (NotificationChannelConfiguration currentChannel : currentSubscription.channels()) {
            if (channelId.equals(currentChannel.id())) {
                channelWasFound = true;
            } else {
                remainingChannels.add(currentChannel);
            }
        }

        if (!channelWasFound) {
            throw new NotificationChannelNotFoundException();
        }

        return saveWithChannels(currentSubscription, remainingChannels);
    }

    public ServerSubscription deleteRule(UUID userId, UUID subscriptionId, UUID ruleId) {
        ServerSubscription currentSubscription = get(userId, subscriptionId);
        List<NotificationRuleDefinition> remainingRules = new ArrayList<>();
        boolean ruleWasFound = false;

        for (NotificationRuleDefinition currentRule : currentSubscription.rules()) {
            if (ruleId.equals(currentRule.id())) {
                ruleWasFound = true;
            } else {
                remainingRules.add(currentRule);
            }
        }

        if (!ruleWasFound) {
            throw new NotificationRuleNotFoundException();
        }

        return saveWithRules(currentSubscription, remainingRules);
    }

    public List<NotificationRuleDefinition> listRules(UUID userId, UUID subscriptionId) {
        return get(userId, subscriptionId).rules();
    }

    public NotificationRuleDefinition getRule(
            UUID userId,
            UUID subscriptionId,
            UUID ruleId
    ) {
        return get(userId, subscriptionId).rules().stream()
                .filter(rule -> ruleId.equals(rule.id()))
                .findFirst()
                .orElseThrow(NotificationRuleNotFoundException::new);
    }

    public List<NotificationChannelConfiguration> listChannels(
            UUID userId,
            UUID subscriptionId
    ) {
        return get(userId, subscriptionId).channels();
    }

    public NotificationChannelConfiguration getChannel(
            UUID userId,
            UUID subscriptionId,
            UUID channelId
    ) {
        return get(userId, subscriptionId).channels().stream()
                .filter(channel -> channelId.equals(channel.id()))
                .findFirst()
                .orElseThrow(NotificationChannelNotFoundException::new);
    }

    private User findEnabledUserForCreation(UUID userId) {
        return findUserEntityForCreation(userId)
                .map(userMapper::toDomain)
                .filter(User::enabled)
                .orElseThrow(UserNotFoundException::new);
    }

    private Optional<UserEntity> findUserEntityForCreation(UUID userId) {
        Optional<UserEntity> lockedUser = userRepository.findLockedById(userId);
        if (lockedUser.isPresent()) {
            return lockedUser;
        }

        // O fallback preserva a compatibilidade com implementações/testes que
        // ainda oferecem somente a consulta convencional por id.
        return userRepository.findById(userId);
    }

    private ServerSubscription createSubscription(
            User user,
            Server server,
            CreateSubscriptionRequest request
    ) {
        Instant currentTime = Instant.now(clock);

        return new ServerSubscription(
                UUID.randomUUID(),
                user.id(),
                server,
                request.serverGuid(),
                request.enabled(),
                List.of(),
                List.of(),
                currentTime,
                currentTime
        );
    }

    private List<NotificationRuleDefinition> createRuleDefinitions(
            List<NotificationRuleRequest> requests
    ) {
        return requests.stream()
                .map(this::createRuleDefinition)
                .toList();
    }

    private NotificationRuleDefinition createRuleDefinition(NotificationRuleRequest request) {
        return createRuleDefinition(UUID.randomUUID(), request);
    }

    private NotificationRuleDefinition createRuleDefinition(
            UUID ruleId,
            NotificationRuleRequest request
    ) {
        return createRuleDefinition(
                ruleId,
                request.type(),
                request.enabled(),
                request.parameters()
        );
    }

    private NotificationRuleDefinition createRuleDefinition(
            UUID ruleId,
            String type,
            boolean enabled,
            Map<String, Object> parameters
    ) {
        return createRuleDefinition(
                ruleId,
                type,
                enabled,
                parameters,
                null,
                null
        );
    }

    private NotificationRuleDefinition createRuleDefinition(
            UUID ruleId,
            String type,
            boolean enabled,
            Map<String, Object> parameters,
            Instant createdAt,
            Instant updatedAt
    ) {
        String normalizedType = ruleValidatorRegistry.normalizeType(type);
        ruleValidatorRegistry.validate(normalizedType, parameters);

        return new NotificationRuleDefinition(
                ruleId,
                normalizedType,
                enabled,
                parameters,
                createdAt,
                updatedAt
        );
    }

    private List<NotificationChannelConfiguration> createChannelConfigurations(
            List<NotificationChannelRequest> requests
    ) {
        Set<String> channelTypes = new HashSet<>();
        List<NotificationChannelConfiguration> configurations = new ArrayList<>();

        for (NotificationChannelRequest request : requests) {
            NotificationChannelConfiguration configuration = createChannelConfiguration(request);
            if (!channelTypes.add(configuration.type())) {
                throw new ChannelAlreadyExistsException();
            }
            configurations.add(configuration);
        }

        return List.copyOf(configurations);
    }

    private NotificationChannelConfiguration createChannelConfiguration(
            NotificationChannelRequest request
    ) {
        return createChannelConfiguration(UUID.randomUUID(), request);
    }

    private NotificationChannelConfiguration createChannelConfiguration(
            UUID channelId,
            NotificationChannelRequest request
    ) {
        rejectRootRecipient(request.type(), request.recipient());
        return createChannelConfiguration(
                channelId,
                request.type(),
                request.enabled(),
                request.parameters()
        );
    }

    private NotificationChannelConfiguration createChannelConfiguration(
            UUID channelId,
            String type,
            boolean enabled,
            Map<String, Object> parameters
    ) {
        String normalizedType = channelValidatorRegistry.normalizeType(type);
        channelValidatorRegistry.validate(normalizedType, parameters);

        return new NotificationChannelConfiguration(
                channelId,
                normalizedType,
                enabled,
                parameters
        );
    }

    private void rejectRootRecipient(String channelType, String recipient) {
        if ("EMAIL".equalsIgnoreCase(channelType) && recipient != null) {
            throw new InvalidChannelConfigurationException(
                    "O campo recipient não é aceito para canais de notificação"
            );
        }
    }

    private ServerSubscription saveWithRules(
            ServerSubscription currentSubscription,
            List<NotificationRuleDefinition> rules
    ) {
        ServerSubscription updatedSubscription = new ServerSubscription(
                currentSubscription.id(),
                currentSubscription.userId(),
                currentSubscription.server(),
                currentSubscription.externalGuid(),
                currentSubscription.enabled(),
                rules,
                currentSubscription.channels(),
                currentSubscription.createdAt(),
                Instant.now(clock)
        );

        return subscriptionPersistenceService.saveRules(updatedSubscription, rules);
    }

    private ServerSubscription saveWithChannels(
            ServerSubscription currentSubscription,
            List<NotificationChannelConfiguration> channels
    ) {
        ServerSubscription updatedSubscription = new ServerSubscription(
                currentSubscription.id(),
                currentSubscription.userId(),
                currentSubscription.server(),
                currentSubscription.externalGuid(),
                currentSubscription.enabled(),
                currentSubscription.rules(),
                channels,
                currentSubscription.createdAt(),
                Instant.now(clock)
        );

        return subscriptionPersistenceService.saveChannels(updatedSubscription, channels);
    }

    private Server resolveServer(String guid, String displayName) {
        Optional<Server> existingServer = identifierRepository
                .findByProviderAndTypeAndValue(
                        ExternalIdentifierTypes.BATTLELOG,
                        ExternalIdentifierTypes.GUID,
                        guid
                )
                .flatMap(identifier -> serverRepository.findById(identifier.getServerId()))
                .map(serverMapper::toDomain);

        if (existingServer.isPresent()) {
            return existingServer.get();
        }

        try {
            return newServerTransaction.create(guid, displayName);
        } catch (DataIntegrityViolationException concurrentCreationException) {
            return findServerCreatedConcurrently(guid, concurrentCreationException);
        }
    }

    private Server findServerCreatedConcurrently(
            String guid,
            DataIntegrityViolationException concurrentCreationException
    ) {
        return identifierRepository
                .findByProviderAndTypeAndValue(
                        ExternalIdentifierTypes.BATTLELOG,
                        ExternalIdentifierTypes.GUID,
                        guid
                )
                .flatMap(identifier -> serverRepository.findById(identifier.getServerId()))
                .map(serverMapper::toDomain)
                .orElseThrow(() -> concurrentCreationException);
    }

}
