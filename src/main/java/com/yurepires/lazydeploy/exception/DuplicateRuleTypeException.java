package com.yurepires.lazydeploy.exception;

public class DuplicateRuleTypeException extends ApplicationException {

    public DuplicateRuleTypeException(String type) {
        super(
                "DUPLICATE_RULE_TYPE",
                "A regra " + type + " foi informada mais de uma vez"
        );
    }
}
