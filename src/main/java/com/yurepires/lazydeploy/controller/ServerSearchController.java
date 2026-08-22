package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.dto.response.ServerSearchResponse;
import com.yurepires.lazydeploy.model.server.ServerDiscoveryProvider;
import com.yurepires.lazydeploy.model.server.ServerSearchQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/bf4/servers")
public class ServerSearchController {

    private final ServerDiscoveryProvider provider;

    public ServerSearchController(ServerDiscoveryProvider provider) {
        this.provider = provider;
    }

    @GetMapping("/search")
    public List<ServerSearchResponse> search(
            @RequestParam("query") @NotBlank String query,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        ServerSearchQuery searchQuery = new ServerSearchQuery(query, limit);

        return provider.search(searchQuery)
                .stream()
                .map(ServerSearchResponse::from)
                .toList();
    }
}
