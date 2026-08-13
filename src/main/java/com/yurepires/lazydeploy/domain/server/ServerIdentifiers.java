package com.yurepires.lazydeploy.domain.server;

public record ServerIdentifiers(String guid, String ip, Integer port) {
    public boolean hasGuid() {
        return guid != null && !guid.isBlank();
    }

    public boolean hasAddress() {
        return ip != null && !ip.isBlank() && port != null && port > 0;
    }

    public ServerAddress address() {
        return hasAddress() ? new ServerAddress(ip, port) : null;
    }
}
