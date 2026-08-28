package com.yurepires.lazydeploy.service.validation.rule;

import com.yurepires.lazydeploy.exception.UnknownMapException;
import com.yurepires.lazydeploy.service.map.MapCatalogService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MapInRuleDefinitionValidatorTest {

    @Test
    void shouldRejectUnknownMapWhenCatalogIsAvailable() {
        MapCatalogService catalogService = mock(MapCatalogService.class);
        when(catalogService.hasCatalogEntries()).thenReturn(true);
        when(catalogService.isKnownMap("MAP_UNKNOWN")).thenReturn(false);

        MapInRuleDefinitionValidator validator = new MapInRuleDefinitionValidator(catalogService);

        assertThatThrownBy(() -> validator.validate(Map.of(
                        "values",
                        List.of("MAP_UNKNOWN")
                )))
                .isInstanceOf(UnknownMapException.class)
                .hasMessageContaining("MAP_UNKNOWN");
    }
}
