package com.yurepires.lazydeploy.model.monitoring;

import com.yurepires.lazydeploy.model.server.ServerSnapshot;

public interface RoundTransitionDetector {
    boolean isNewRound(ServerState previous, ServerSnapshot current);
}
