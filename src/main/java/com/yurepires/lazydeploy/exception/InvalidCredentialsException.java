package com.yurepires.lazydeploy.exception;

public class InvalidCredentialsException extends ApplicationException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Email ou senha inválidos");
    }
}
