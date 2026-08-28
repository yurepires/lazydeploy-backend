package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.dto.response.BattlefieldMapResponse;
import com.yurepires.lazydeploy.exception.MapNotFoundException;
import com.yurepires.lazydeploy.service.map.MapCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bf4/maps")
public class BattlefieldMapController {

    private final MapCatalogService mapCatalogService;

    public BattlefieldMapController(MapCatalogService mapCatalogService) {
        this.mapCatalogService = mapCatalogService;
    }

    @GetMapping
    public List<BattlefieldMapResponse> listMaps() {
        return mapCatalogService.listAvailableMaps()
                .stream()
                .map(BattlefieldMapResponse::from)
                .toList();
    }

    @GetMapping("/{mapId}")
    public BattlefieldMapResponse getMap(@PathVariable String mapId) {
        return mapCatalogService.findById(mapId)
                .map(BattlefieldMapResponse::from)
                .orElseThrow(() -> new MapNotFoundException(mapId));
    }
}
