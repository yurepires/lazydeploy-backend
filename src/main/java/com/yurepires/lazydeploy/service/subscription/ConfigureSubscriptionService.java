package com.yurepires.lazydeploy.service.subscription;

import com.yurepires.lazydeploy.dto.request.ConfigureChannelRequest;
import com.yurepires.lazydeploy.dto.request.ConfigureRuleRequest;
import com.yurepires.lazydeploy.dto.request.ConfigureSubscriptionRequest;
import com.yurepires.lazydeploy.dto.request.NotificationChannelRequest;
import com.yurepires.lazydeploy.dto.request.NotificationRuleRequest;
import com.yurepires.lazydeploy.dto.request.SubscriptionCreationRequest;
import com.yurepires.lazydeploy.exception.ApplicationException;
import com.yurepires.lazydeploy.exception.DuplicateChannelTypeException;
import com.yurepires.lazydeploy.exception.DuplicateRuleTypeException;
import com.yurepires.lazydeploy.exception.ExternalProviderUnavailableException;
import com.yurepires.lazydeploy.exception.InvalidChannelConfigurationException;
import com.yurepires.lazydeploy.exception.InvalidRequestException;
import com.yurepires.lazydeploy.exception.InvalidRuleParametersException;
import com.yurepires.lazydeploy.exception.NoActiveNotificationChannelException;
import com.yurepires.lazydeploy.exception.SubscriptionAlreadyExistsException;
import com.yurepires.lazydeploy.exception.UnsupportedChannelTypeException;
import com.yurepires.lazydeploy.exception.UnsupportedRuleTypeException;
import com.yurepires.lazydeploy.model.persistence.ServerSubscription;
import com.yurepires.lazydeploy.security.CurrentUserProvider;
import com.yurepires.lazydeploy.service.observability.SubscriptionConfigurationMetrics;
import com.yurepires.lazydeploy.service.validation.channel.ChannelConfigurationValidatorRegistry;
import com.yurepires.lazydeploy.service.validation.BusinessLimitService;
import com.yurepires.lazydeploy.service.validation.rule.RuleDefinitionValidatorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Cria uma subscription e toda a sua configuração em uma única transação.
 */
@Service
public class ConfigureSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(
            ConfigureSubscriptionService.class
    );

    private final CurrentUserProvider currentUserProvider;
    private final SubscriptionApplicationService subscriptionService;
    private final RuleDefinitionValidatorRegistry ruleValidatorRegistry;
    private final ChannelConfigurationValidatorRegistry channelValidatorRegistry;
    private final BusinessLimitService businessLimitService;
    private final SubscriptionConfigurationMetrics metrics;
    private final Clock clock;
    // Stripes fixas evitam que uma entrada de lock seja criada para cada GUID
    // recebido e permaneça na memória indefinidamente.
    private static final int SERVER_LOCK_STRIPES = 64;
    private final Object[] serverLocks = createLocks(SERVER_LOCK_STRIPES);

    public ConfigureSubscriptionService(
            CurrentUserProvider currentUserProvider,
            SubscriptionApplicationService subscriptionService,
            RuleDefinitionValidatorRegistry ruleValidatorRegistry,
            ChannelConfigurationValidatorRegistry channelValidatorRegistry,
            BusinessLimitService businessLimitService,
            SubscriptionConfigurationMetrics metrics,
            Clock clock
    ) {
        this.currentUserProvider = currentUserProvider;
        this.subscriptionService = subscriptionService;
        this.ruleValidatorRegistry = ruleValidatorRegistry;
        this.channelValidatorRegistry = channelValidatorRegistry;
        this.businessLimitService = businessLimitService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public ServerSubscription configure(ConfigureSubscriptionRequest request) {
        Instant startedAt = clock.instant();
        String outcome = "internal_error";

        try {
            // A criação delegada valida os parâmetros antes de resolver/persistir
            // o servidor; aqui ficam as regras estruturais e duplicidades do lote.
            validateConfiguration(request);

            UUID currentUserId = currentUserProvider.getCurrentUserId();
            String normalizedGuid = normalizeGuid(request.serverGuid());
            Object serverLock = lockFor(normalizedGuid);

            ServerSubscription subscription;
            synchronized (serverLock) {
                SubscriptionCreationRequest creationRequest =
                        toSubscriptionCreationRequest(request);
                subscription = subscriptionService.create(
                        currentUserId,
                        creationRequest
                );
            }

            outcome = "success";
            log.info(
                    "Subscription configurada | subscriptionId={} | serverId={}",
                    subscription.id(),
                    subscription.server().id()
            );
            return subscription;
        } catch (SubscriptionAlreadyExistsException exception) {
            outcome = "duplicate";
            throw exception;
        } catch (ExternalProviderUnavailableException exception) {
            outcome = "provider_error";
            throw exception;
        } catch (UnsupportedRuleTypeException | UnsupportedChannelTypeException exception) {
            outcome = "validation_error";
            throw exception;
        } catch (ApplicationException exception) {
            outcome = "validation_error";
            throw exception;
        } catch (RuntimeException exception) {
            outcome = "internal_error";
            throw exception;
        } finally {
            metrics.record(outcome, elapsedSince(startedAt));
        }
    }

    private void validateConfiguration(ConfigureSubscriptionRequest request) {
        if (request == null) {
            throw new InvalidRequestException("A configuração da subscription deve ser informada");
        }

        if (request.rules() == null || request.rules().isEmpty()) {
            throw new InvalidRuleParametersException(
                    "Informe pelo menos uma regra de notificação"
            );
        }

        if (request.channels() == null) {
            throw new InvalidChannelConfigurationException(
                    "A lista de canais deve ser informada"
            );
        }

        businessLimitService.validateRuleCollectionSize(request.rules().size());
        businessLimitService.validateChannelCollectionSize(request.channels().size());

        validateRules(request.rules());
        validateChannels(request.channels());

        if (Boolean.TRUE.equals(request.enabled())
                && !hasActiveChannel(request.channels())) {
            throw new NoActiveNotificationChannelException();
        }
    }

    private Object lockFor(String normalizedGuid) {
        int index = Math.floorMod(normalizedGuid.hashCode(), serverLocks.length);
        return serverLocks[index];
    }

    private static Object[] createLocks(int lockCount) {
        Object[] locks = new Object[lockCount];
        for (int index = 0; index < lockCount; index++) {
            locks[index] = new Object();
        }
        return locks;
    }

    private void validateRules(List<ConfigureRuleRequest> rules) {
        Set<String> ruleTypes = new HashSet<>();

        for (ConfigureRuleRequest rule : rules) {
            if (rule == null) {
                throw new InvalidRuleParametersException(
                        "A lista de regras não pode conter valores nulos"
                );
            }

            String normalizedType = ruleValidatorRegistry.normalizeType(rule.type());

            ruleValidatorRegistry.validate(normalizedType, rule.parameters());

            if (!ruleTypes.add(normalizedType)) {
                throw new DuplicateRuleTypeException(normalizedType);
            }
        }
    }

    private void validateChannels(List<ConfigureChannelRequest> channels) {
        Set<String> channelTypes = new HashSet<>();

        for (ConfigureChannelRequest channel : channels) {
            if (channel == null) {
                throw new InvalidChannelConfigurationException(
                        "A lista de canais não pode conter valores nulos"
                );
            }

            String normalizedType = channelValidatorRegistry.normalizeType(channel.type());
            rejectRootRecipient(channel.recipient());

            if (!channelTypes.add(normalizedType)) {
                throw new DuplicateChannelTypeException(normalizedType);
            }
        }
    }

    private boolean hasActiveChannel(List<ConfigureChannelRequest> channels) {
        for (ConfigureChannelRequest channel : channels) {
            if (Boolean.TRUE.equals(channel.enabled())) {
                return true;
            }
        }

        return false;
    }

    private void rejectRootRecipient(String recipient) {
        if (recipient != null) {
            throw new InvalidChannelConfigurationException(
                    "O campo recipient não é aceito para canais de notificação"
            );
        }
    }

    private SubscriptionCreationRequest toSubscriptionCreationRequest(
            ConfigureSubscriptionRequest request
    ) {
        List<NotificationRuleRequest> rules = new ArrayList<>();
        for (ConfigureRuleRequest rule : request.rules()) {
            rules.add(new NotificationRuleRequest(
                    rule.type(),
                    Boolean.TRUE.equals(rule.enabled()),
                    rule.parameters()
            ));
        }

        List<NotificationChannelRequest> channels = new ArrayList<>();
        for (ConfigureChannelRequest channel : request.channels()) {
            channels.add(new NotificationChannelRequest(
                    channel.type(),
                    Boolean.TRUE.equals(channel.enabled()),
                    channel.parameters()
            ));
        }

        return new SubscriptionCreationRequest(
                request.serverGuid(),
                request.displayName(),
                rules,
                channels,
                request.enabled()
        );
    }

    private Duration elapsedSince(Instant startedAt) {
        Duration elapsed = Duration.between(startedAt, clock.instant());
        if (elapsed.isNegative()) {
            return Duration.ZERO;
        }

        return elapsed;
    }

    private String normalizeGuid(String guid) {
        if (guid == null) {
            return "";
        }

        return guid.trim().toLowerCase(Locale.ROOT);
    }
}
