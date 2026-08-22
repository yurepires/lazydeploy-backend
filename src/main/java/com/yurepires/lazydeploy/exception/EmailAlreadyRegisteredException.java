package com.yurepires.lazydeploy.exception;

public class EmailAlreadyRegisteredException extends ApplicationException {

    public EmailAlreadyRegisteredException() {
        super("EMAIL_ALREADY_REGISTERED", "Este email já está cadastrado");
    }
}
