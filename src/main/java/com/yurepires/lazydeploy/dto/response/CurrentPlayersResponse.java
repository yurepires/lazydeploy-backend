package com.yurepires.lazydeploy.dto.response;

/**
 * Quantidade de jogadores observada no servidor.
 */
public record CurrentPlayersResponse(
        int current,
        int max
) {
}
