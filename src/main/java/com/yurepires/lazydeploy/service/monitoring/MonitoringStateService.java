package com.yurepires.lazydeploy.service.monitoring;

import com.yurepires.lazydeploy.mapper.MonitoringMapper;
import com.yurepires.lazydeploy.model.monitoring.RoundInstance;
import com.yurepires.lazydeploy.model.monitoring.RoundTransitionDetector;
import com.yurepires.lazydeploy.model.monitoring.ServerState;
import com.yurepires.lazydeploy.model.persistence.Server;
import com.yurepires.lazydeploy.repository.RoundInstanceRepository;
import com.yurepires.lazydeploy.repository.ServerStateRepository;
import com.yurepires.lazydeploy.model.server.ServerSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitoringStateService {

    private final ServerStateRepository states;
    private final RoundInstanceRepository rounds;
    private final RoundTransitionDetector transitionDetector;
    private final MonitoringMapper monitoringMapper;

    public MonitoringStateService(
            ServerStateRepository states,
            RoundInstanceRepository rounds,
            RoundTransitionDetector transitionDetector,
            MonitoringMapper monitoringMapper
    ) {
        this.states = states;
        this.rounds = rounds;
        this.transitionDetector = transitionDetector;
        this.monitoringMapper = monitoringMapper;
    }

    @Transactional
    public MonitoringObservation observe(Server server, ServerSnapshot currentSnapshot) {
        ServerState previousState = states.findById(server.id())
                .map(monitoringMapper::toDomain)
                .orElse(null);
        boolean newRoundDetected = previousState == null
                || transitionDetector.isNewRound(previousState, currentSnapshot);

        RoundInstance currentRound;
        if (newRoundDetected) {
            closePreviousRound(previousState, currentSnapshot);
            currentRound = createRound(server, currentSnapshot);
        } else {
            currentRound = findCurrentRound(previousState);
        }

        ServerState currentState = saveCurrentState(server, currentSnapshot, currentRound);
        boolean initialObservation = previousState == null;

        return new MonitoringObservation(
                previousState,
                currentState,
                initialObservation,
                newRoundDetected
        );
    }

    private void closePreviousRound(ServerState previousState, ServerSnapshot currentSnapshot) {
        if (previousState == null) {
            return;
        }

        rounds.findById(previousState.roundInstanceId())
                .map(monitoringMapper::toDomain)
                .map(round -> round.endedAt(currentSnapshot.capturedAt()))
                .map(monitoringMapper::toEntity)
                .ifPresent(rounds::save);
    }

    private RoundInstance createRound(Server server, ServerSnapshot currentSnapshot) {
        RoundInstance detectedRound = RoundInstance.detected(
                server.id(),
                currentSnapshot.map().normalizedId(),
                currentSnapshot.capturedAt(),
                currentSnapshot.roundTimeSeconds()
        );

        return monitoringMapper.toDomain(
                rounds.save(monitoringMapper.toEntity(detectedRound))
        );
    }

    private RoundInstance findCurrentRound(ServerState previousState) {
        return rounds.findById(previousState.roundInstanceId())
                .map(monitoringMapper::toDomain)
                .orElseThrow();
    }

    private ServerState saveCurrentState(
            Server server,
            ServerSnapshot currentSnapshot,
            RoundInstance currentRound
    ) {
        Integer playerCount = null;
        Integer maxPlayers = null;
        if (currentSnapshot.players() != null) {
            playerCount = currentSnapshot.players().current();
            maxPlayers = currentSnapshot.players().maximum();
        }

        ServerState currentState = new ServerState(
                server.id(),
                currentRound.id(),
                currentSnapshot.map().normalizedId(),
                currentSnapshot.roundTimeSeconds(),
                currentRound.detectedAt(),
                currentSnapshot.capturedAt(),
                playerCount,
                maxPlayers,
                currentSnapshot.gameMode()
        );

        return monitoringMapper.toDomain(
                states.save(monitoringMapper.toEntity(currentState))
        );
    }
}
