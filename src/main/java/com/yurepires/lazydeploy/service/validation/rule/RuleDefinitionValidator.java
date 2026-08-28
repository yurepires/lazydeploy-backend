package com.yurepires.lazydeploy.service.validation.rule;

import java.util.Map;

public interface RuleDefinitionValidator {

    String supportedType();

    void validate(Map<String, Object> parameters);
}
