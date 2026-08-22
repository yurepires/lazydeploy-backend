package com.yurepires.lazydeploy.service.monitoring;

import com.yurepires.lazydeploy.model.monitoring.ServerState;

public class MonitoringObservation {

    private final ServerState previous;
    private final ServerState current;
    private final boolean initial;
    private final boolean newRound;

    public MonitoringObservation(
            ServerState previous,
            ServerState current,
            boolean initial,
            boolean newRound
    ) {
        this.previous = previous;
        this.current = current;
        this.initial = initial;
        this.newRound = newRound;
    }

    public ServerState previous() {
        return previous;
    }

    public ServerState current() {
        return current;
    }

    public boolean initial() {
        return initial;
    }

    public boolean newRound() {
        return newRound;
    }
}
