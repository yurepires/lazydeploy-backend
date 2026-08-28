package com.yurepires.lazydeploy.service.validation.rule;

import com.yurepires.lazydeploy.exception.InvalidRuleParametersException;
import com.yurepires.lazydeploy.exception.UnsupportedRuleTypeException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RuleDefinitionValidatorRegistry {

    private final Map<String, RuleDefinitionValidator> validatorsByType;

    public RuleDefinitionValidatorRegistry(List<RuleDefinitionValidator> validators) {
        this.validatorsByType = validators.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        validator -> normalizeType(validator.supportedType()),
                        validator -> validator
                ));
    }

    public String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new InvalidRuleParametersException("O tipo da regra deve ser informado");
        }

        return type.trim().toUpperCase(Locale.ROOT);
    }

    public RuleDefinitionValidator resolve(String type) {
        String normalizedType = normalizeType(type);
        RuleDefinitionValidator validator = validatorsByType.get(normalizedType);
        if (validator == null) {
            throw new UnsupportedRuleTypeException(type);
        }

        return validator;
    }

    public void validate(String type, Map<String, Object> parameters) {
        if (parameters == null) {
            throw new InvalidRuleParametersException(
                    "Os parâmetros da regra devem ser informados"
            );
        }

        resolve(type).validate(parameters);
    }
}
