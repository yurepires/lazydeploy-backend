package com.yurepires.lazydeploy.exception;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException() {
        super("USER_NOT_FOUND", "Usuário não encontrado ou desabilitado");
    }
}
