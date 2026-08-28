package com.yurepires.lazydeploy.exception;

public class UnsupportedRuleTypeException extends IllegalArgumentException {

    private static final String ERROR_CODE = "UNSUPPORTED_RULE_TYPE";

    public UnsupportedRuleTypeException(String type) {
        super("Tipo de regra não suportado: " + type);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
