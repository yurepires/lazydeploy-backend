package com.yurepires.lazydeploy.service.validation.channel;

import com.yurepires.lazydeploy.exception.InvalidChannelConfigurationException;
import com.yurepires.lazydeploy.exception.UnsupportedChannelTypeException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ChannelConfigurationValidatorRegistry {

    private final Map<String, ChannelConfigurationValidator> validatorsByType;

    public ChannelConfigurationValidatorRegistry(List<ChannelConfigurationValidator> validators) {
        this.validatorsByType = validators.stream()
                .collect(Collectors.toUnmodifiableMap(
                        validator -> normalizeType(validator.supportedType()),
                        validator -> validator
                ));
    }

    public String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new InvalidChannelConfigurationException(
                    "O tipo do canal deve ser informado"
            );
        }

        return type.trim().toUpperCase(Locale.ROOT);
    }

    public ChannelConfigurationValidator resolve(String type) {
        String normalizedType = normalizeType(type);
        ChannelConfigurationValidator validator = validatorsByType.get(normalizedType);
        if (validator == null) {
            throw new UnsupportedChannelTypeException(type);
        }

        return validator;
    }

    public void validate(String type, Map<String, Object> parameters) {
        if (parameters == null) {
            throw new InvalidChannelConfigurationException(
                    "Os parâmetros do canal devem ser informados"
            );
        }

        resolve(type).validate(parameters);
    }
}
