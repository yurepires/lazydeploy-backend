package com.yurepires.lazydeploy.exception;

public class RuleLimitReachedException extends ApplicationException {

    public RuleLimitReachedException() {
        super(
                "RULE_LIMIT_REACHED",
                "Este alerta atingiu o número máximo de condições permitidas."
        );
    }
}
