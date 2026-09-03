package com.yurepires.lazydeploy.dto.response;

import java.time.Instant;

/**
 * Último estado persistido do servidor associado a uma subscription.
 *
 * <p>Quando o monitor ainda não observou o servidor, os dados de status são
 * retornados como indisponíveis em vez de provocar uma chamada externa ou um
 * erro de recurso não encontrado.</p>
 */
public record CurrentServerStatusResponse(
        boolean available,
        String availabilityReason,
        CurrentMapResponse map,
        CurrentPlayersResponse players,
        String gameMode,
        Instant lastObservedAt,
        Instant capturedAt
) {
}
