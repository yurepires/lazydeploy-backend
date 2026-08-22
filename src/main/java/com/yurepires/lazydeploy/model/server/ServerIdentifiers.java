package com.yurepires.lazydeploy.model.server;

public record ServerIdentifiers(String guid, String ip, Integer port) {

    public boolean hasGuid() {
        return guid != null && !guid.isBlank();
    }

}
