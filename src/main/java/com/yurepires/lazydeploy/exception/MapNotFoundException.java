package com.yurepires.lazydeploy.exception;

public class MapNotFoundException extends ResourceNotFoundException {

    public MapNotFoundException(String mapId) {
        super("MAP_NOT_FOUND", "Mapa não encontrado: " + mapId);
    }
}
