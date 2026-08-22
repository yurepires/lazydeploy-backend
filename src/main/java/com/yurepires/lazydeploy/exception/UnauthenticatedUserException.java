package com.yurepires.lazydeploy.exception;

public class UnauthenticatedUserException extends ApplicationException {

    public UnauthenticatedUserException() {
        super("UNAUTHENTICATED", "Usuário não autenticado");
    }
}
