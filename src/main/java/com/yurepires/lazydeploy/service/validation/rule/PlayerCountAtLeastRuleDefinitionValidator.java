package com.yurepires.lazydeploy.service.validation.rule;

import com.yurepires.lazydeploy.exception.InvalidRuleParametersException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PlayerCountAtLeastRuleDefinitionValidator implements RuleDefinitionValidator {

    @Override
    public String supportedType() {
        return "PLAYER_COUNT_AT_LEAST";
    }

    @Override
    public void validate(Map<String, Object> parameters) {
        if (parameters == null) {
            throw new InvalidRuleParametersException(
                    "Os parâmetros de PLAYER_COUNT_AT_LEAST devem ser informados"
            );
        }

        Object value = parameters.get("value");
        if (!(value instanceof Number number) || !isWholeNumber(number)) {
            throw new InvalidRuleParametersException(
                    "PLAYER_COUNT_AT_LEAST requer um inteiro no parâmetro 'value'"
            );
        }

        long minimumPlayers = number.longValue();
        if (minimumPlayers < 0 || minimumPlayers > Integer.MAX_VALUE) {
            throw new InvalidRuleParametersException(
                    "PLAYER_COUNT_AT_LEAST requer um valor entre zero e 2147483647"
            );
        }
    }

    private boolean isWholeNumber(Number number) {
        double numericValue = number.doubleValue();
        return Double.isFinite(numericValue)
                && numericValue == Math.rint(numericValue);
    }
}
