package com.yurepires.lazydeploy.domain.monitoring;

import com.yurepires.lazydeploy.domain.server.ServerSnapshot;

public interface RoundTransitionDetector {
    boolean isNewRound(ServerState previous, ServerSnapshot current);
}
