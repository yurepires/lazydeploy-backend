package com.yurepires.lazydeploy.exception;

public class PersistenceMappingException extends ApplicationException {

    public PersistenceMappingException(String message) {
        super("PERSISTENCE_MAPPING_ERROR", message);
    }
}
