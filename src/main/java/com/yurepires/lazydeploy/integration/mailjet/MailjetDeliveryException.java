package com.yurepires.lazydeploy.integration.mailjet;

/** Exceção estável para falhas de comunicação ou rejeição da API do Mailjet. */
public class MailjetDeliveryException extends RuntimeException {

    private final String errorCode;

    public MailjetDeliveryException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public MailjetDeliveryException(String errorCode, Throwable cause) {
        super(errorCode, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
