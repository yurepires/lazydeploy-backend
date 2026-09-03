package com.yurepires.lazydeploy.dto.response;

/**
 * Identificação do mapa atualmente observado no servidor.
 */
public record CurrentMapResponse(
        String id,
        String displayName
) {
}
