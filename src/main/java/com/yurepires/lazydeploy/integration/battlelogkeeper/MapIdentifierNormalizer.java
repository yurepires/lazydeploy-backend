package com.yurepires.lazydeploy.integration.battlelogkeeper;

import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class MapIdentifierNormalizer {

    public String normalize(String externalId) {

        if (externalId == null || externalId.isBlank()) {
            return null;
        }

        return Arrays.stream(externalId.split("/"))
                .filter(segment -> !segment.isBlank())
                .reduce((first, last) -> last)
                .orElse(null);
    }
}
