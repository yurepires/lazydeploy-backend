package com.yurepires.lazydeploy.service.subscription;

import com.yurepires.lazydeploy.dto.request.NotificationChannelRequest;
import com.yurepires.lazydeploy.dto.request.NotificationRuleRequest;
import com.yurepires.lazydeploy.dto.request.SubscriptionCreationRequest;
import com.yurepires.lazydeploy.exception.NotificationRuleNotFoundException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    public SubscriptionApplicationService(
            UserRepository userRepository,
            ServerRepository serverRepository,
            ServerIdentifierRepository identifierRepository,
            ServerSubscriptionPersistenceService subscriptionPersistenceService,
            NewServerTransaction newServerTransaction,
            UserMapper userMapper,
            ServerMapper serverMapper,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.serverRepository = serverRepository;
        this.identifierRepository = identifierRepository;
        this.subscriptionPersistenceService = subscriptionPersistenceService;
        this.newServerTransaction = newServerTransaction;
        this.userMapper = userMapper;
        this.serverMapper = serverMapper;
        this.clock = clock;
    }

    public ServerSubscription create(UUID userId, SubscriptionCreationRequest request) {
        User user = findEnabledUser(userId);
        Server server = resolveServer(request.serverGuid(), request.displayName());

        Optional<ServerSubscription> existingSubscription = subscriptionPersistenceService
                .findByUserIdAndServerId(user.id(), server.id());

        if (existingSubscription.isPresent()) {
            return existingSubscription.get();
        }

        ServerSubscription subscription = createSubscription(user, server, request);

        try {
            return subscriptionPersistenceService.save(subscription);
        } catch (DataIntegrityViolationException concurrentCreationException) {
            return findSubscriptionCreatedConcurrently(
                    user.id(),
                    server.id(),
                    concurrentCreationException
            );
        }
    }

    public List<ServerSubscription> list(UUID userId) {
        return subscriptionPersistenceService.findAllByUserId(userId);
    }

    public ServerSubscription get(UUID userId, UUID subscriptionId) {
        return subscriptionPersistenceService
                .findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(SubscriptionNotFoundException::new);
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
        ServerSubscription currentSubscription = get(userId, subscriptionId);
        List<NotificationRuleDefinition> updatedRules = new ArrayList<>(currentSubscription.rules());
        updatedRules.add(createRuleDefinition(request));

        return saveWithRules(currentSubscription, updatedRules);
    }

    public ServerSubscription updateRule(
            UUID userId,
            UUID subscriptionId,
            UUID ruleId,
            NotificationRuleRequest request
    ) {
        ServerSubscription currentSubscription = get(userId, subscriptionId);
        List<NotificationRuleDefinition> updatedRules = new ArrayList<>();
        boolean ruleWasFound = false;

        for (NotificationRuleDefinition currentRule : currentSubscription.rules()) {
            if (ruleId.equals(currentRule.id())) {
                updatedRules.add(createRuleDefinition(ruleId, request));
                ruleWasFound = true;
            } else {
                updatedRules.add(currentRule);
            }
        }

        if (!ruleWasFound) {
            throw new NotificationRuleNotFoundException();
        }

        return saveWithRules(currentSubscription, updatedRules);
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

    private User findEnabledUser(UUID userId) {
        return userRepository
                .findById(userId)
                .map(userMapper::toDomain)
                .filter(User::enabled)
                .orElseThrow(UserNotFoundException::new);
    }

    private ServerSubscription createSubscription(
            User user,
            Server server,
            SubscriptionCreationRequest request
    ) {
        Instant currentTime = Instant.now(clock);
        List<NotificationRuleDefinition> rules = createRuleDefinitions(request.rules());
        List<NotificationChannelConfiguration> channels = createChannelConfigurations(
                request.channels()
        );

        return new ServerSubscription(
                UUID.randomUUID(),
                user.id(),
                server,
                request.serverGuid(),
                true,
                rules,
                channels,
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
        return new NotificationRuleDefinition(
                ruleId,
                request.type(),
                request.enabled(),
                request.parameters()
        );
    }

    private List<NotificationChannelConfiguration> createChannelConfigurations(
            List<NotificationChannelRequest> requests
    ) {
        return requests.stream()
                .map(this::createChannelConfiguration)
                .toList();
    }

    private NotificationChannelConfiguration createChannelConfiguration(
            NotificationChannelRequest request
    ) {
        return new NotificationChannelConfiguration(
                UUID.randomUUID(),
                request.type(),
                request.enabled(),
                request.parameters()
        );
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

        return subscriptionPersistenceService.save(updatedSubscription);
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

    private ServerSubscription findSubscriptionCreatedConcurrently(
            UUID userId,
            UUID serverId,
            DataIntegrityViolationException concurrentCreationException
    ) {
        return subscriptionPersistenceService
                .findByUserIdAndServerId(userId, serverId)
                .orElseThrow(() -> concurrentCreationException);
    }
}
