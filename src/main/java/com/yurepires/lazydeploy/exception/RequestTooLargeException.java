package com.yurepires.lazydeploy.exception;

/** Indica que o corpo da requisição ultrapassou o limite configurado. */
public class RequestTooLargeException extends RuntimeException {

    public RequestTooLargeException() {
        super("O corpo da requisição excede o tamanho máximo permitido.");
    }
}
