package com.yurepires.lazydeploy.service.validation.rule;

import com.yurepires.lazydeploy.exception.InvalidRuleParametersException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

@Component
public class MapInRuleDefinitionValidator implements RuleDefinitionValidator {

    @Override
    public String supportedType() {
        return "MAP_IN";
    }

    @Override
    public void validate(Map<String, Object> parameters) {
        if (parameters == null) {
            throw new InvalidRuleParametersException(
                    "Os parâmetros de MAP_IN devem ser informados"
            );
        }

        Object values = parameters.get("values");
        if (!(values instanceof Collection<?> collection) || collection.isEmpty()) {
            throw new InvalidRuleParametersException(
                    "MAP_IN requer uma coleção não vazia no parâmetro 'values'"
            );
        }

        HashSet<String> uniqueValues = new HashSet<>();
        for (Object value : collection) {
            if (!(value instanceof String string) || string.isBlank()) {
                throw new InvalidRuleParametersException(
                        "MAP_IN aceita somente nomes de mapa preenchidos"
                );
            }

            if (!uniqueValues.add(string)) {
                throw new InvalidRuleParametersException(
                        "MAP_IN não aceita mapas duplicados"
                );
            }
        }
    }
}
