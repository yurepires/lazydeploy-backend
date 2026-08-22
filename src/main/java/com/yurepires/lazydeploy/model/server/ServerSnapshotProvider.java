package com.yurepires.lazydeploy.model.server;

import java.util.Optional;

public interface ServerSnapshotProvider {

    Optional<ServerSnapshot> getSnapshot(ServerReference server);

    String providerId();

}
