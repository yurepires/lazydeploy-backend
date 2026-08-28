package com.yurepires.lazydeploy.service.map;

import com.yurepires.lazydeploy.entity.BattlefieldMapEntity;
import com.yurepires.lazydeploy.mapper.BattlefieldMapMapper;
import com.yurepires.lazydeploy.model.map.BattlefieldMap;
import com.yurepires.lazydeploy.repository.BattlefieldMapRepository;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class MapCatalogService {

    private final BattlefieldMapRepository repository;
    private final BattlefieldMapMapper mapper;

    public MapCatalogService(BattlefieldMapRepository repository) {
        this(repository, Mappers.getMapper(BattlefieldMapMapper.class));
    }

    @Autowired
    public MapCatalogService(
            BattlefieldMapRepository repository,
            BattlefieldMapMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Optional<BattlefieldMap> findById(String mapId) {
        if (mapId == null || mapId.isBlank()) {
            return Optional.empty();
        }

        String normalizedMapId = mapId.trim();
        return repository.findById(normalizedMapId).map(mapper::toDomain);
    }

    public String getDisplayName(String mapId) {
        if (mapId == null || mapId.isBlank()) {
            return mapId;
        }

        String normalizedMapId = mapId.trim();
        return findById(normalizedMapId)
                .map(map -> displayNameOrId(map, normalizedMapId))
                .orElse(normalizedMapId);
    }

    public List<BattlefieldMap> listAvailableMaps() {
        List<BattlefieldMapEntity> enabledEntities = repository.findAllEnabled();
        if (enabledEntities == null) {
            enabledEntities = repository.findAllByEnabledTrueOrderByDisplayNameAsc();
        }

        if (enabledEntities == null) {
            return List.of();
        }

        return enabledEntities
                .stream()
                .map(mapper::toDomain)
                .filter(BattlefieldMap::enabled)
                .sorted(Comparator.comparing(
                        BattlefieldMap::displayName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .toList();
    }

    public boolean isKnownMap(String mapId) {
        if (mapId == null || mapId.isBlank()) {
            return false;
        }

        return repository.existsById(mapId.trim());
    }

    /**
     * Informa se o catálogo possui dados cadastrados. Isso permite manter a
     * aplicação tolerante durante uma inicialização sem seed, sem desativar a
     * validação quando o catálogo estiver disponível.
     */
    public boolean hasCatalogEntries() {
        return repository.count() > 0;
    }

    private String displayNameOrId(BattlefieldMap map, String mapId) {
        if (map.displayName() == null || map.displayName().isBlank()) {
            return mapId;
        }

        return map.displayName();
    }
}
