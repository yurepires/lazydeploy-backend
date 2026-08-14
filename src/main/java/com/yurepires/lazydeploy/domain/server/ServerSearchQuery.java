package com.yurepires.lazydeploy.domain.server;

public record ServerSearchQuery(String text, int limit) {
    public ServerSearchQuery {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("O texto de busca deve ser informado");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("O limite deve ser maior que zero");
        }
    }
}
