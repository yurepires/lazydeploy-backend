package com.yurepires.lazydeploy.model.server;

import com.yurepires.lazydeploy.exception.InvalidRequestException;

public record ServerSearchQuery(String text, int limit) {
    public ServerSearchQuery {
        if (text == null || text.isBlank()) {
            throw new InvalidRequestException("O texto de busca deve ser informado");
        }
        if (limit <= 0) {
            throw new InvalidRequestException("O limite deve ser maior que zero");
        }
    }
}
