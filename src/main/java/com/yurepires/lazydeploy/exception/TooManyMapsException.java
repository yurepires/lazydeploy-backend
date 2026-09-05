package com.yurepires.lazydeploy.exception;

public class TooManyMapsException extends ApplicationException {

    public TooManyMapsException() {
        super(
                "TOO_MANY_MAPS",
                "A quantidade de mapas selecionados excede o limite permitido."
        );
    }
}
