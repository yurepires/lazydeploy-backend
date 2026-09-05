package com.yurepires.lazydeploy.service.validation.rule;

import com.yurepires.lazydeploy.exception.InvalidRuleParametersException;
import com.yurepires.lazydeploy.exception.UnknownMapException;
import com.yurepires.lazydeploy.exception.TooManyMapsException;
import com.yurepires.lazydeploy.service.validation.BusinessLimitService;
import com.yurepires.lazydeploy.service.map.MapCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Component
public class MapInRuleDefinitionValidator implements RuleDefinitionValidator {

    private final MapCatalogService mapCatalogService;
    private final BusinessLimitService businessLimitService;

    public MapInRuleDefinitionValidator() {
        this(null);
    }

    public MapInRuleDefinitionValidator(MapCatalogService mapCatalogService) {
        this.mapCatalogService = mapCatalogService;
        this.businessLimitService = null;
    }

    @Autowired
    public MapInRuleDefinitionValidator(
            MapCatalogService mapCatalogService,
            BusinessLimitService businessLimitService
    ) {
        this.mapCatalogService = mapCatalogService;
        this.businessLimitService = businessLimitService;
    }

    @Override
    public String supportedType() {
        return "MAP_IN";
    }

    @Override
    public void validate(Map<String, Object> parameters) {
        if (parameters == null) {
            throw new InvalidRuleParametersException(
                    "Os parâmetros de MAP_IN devem ser informados"
            );
        }

        Object values = parameters.get("values");
        if (!(values instanceof Collection<?> collection) || collection.isEmpty()) {
            throw new InvalidRuleParametersException(
                    "MAP_IN requer uma coleção não vazia no parâmetro 'values'"
            );
        }

        int maximumMaps = 20;
        if (businessLimitService != null) {
            maximumMaps = businessLimitService.maximumMapsPerMapInRule();
        }
        if (collection.size() > maximumMaps) {
            if (businessLimitService != null) {
                businessLimitService.recordMapLimitRejection();
            }
            throw new TooManyMapsException();
        }

        boolean catalogHasEntries = mapCatalogService != null
                && mapCatalogService.hasCatalogEntries();
        HashSet<String> uniqueValues = new HashSet<>();
        List<String> unknownMapIds = new ArrayList<>();
        for (Object value : collection) {
            if (!(value instanceof String string) || string.isBlank()) {
                throw new InvalidRuleParametersException(
                        "MAP_IN aceita somente identificadores de mapa preenchidos"
                );
            }

            if (!uniqueValues.add(string)) {
                throw new InvalidRuleParametersException(
                        "MAP_IN não aceita mapas duplicados"
                );
            }

            if (catalogHasEntries && !mapCatalogService.isKnownMap(string)) {
                unknownMapIds.add(string);
            }
        }

        if (!unknownMapIds.isEmpty()) {
            throw new UnknownMapException(unknownMapIds);
        }
    }
}
