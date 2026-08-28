package com.yurepires.lazydeploy.exception;

public class InvalidRuleParametersException extends ApplicationException {

    public InvalidRuleParametersException(String message) {
        super("INVALID_RULE_PARAMETERS", message);
    }
}
