package com.yurepires.lazydeploy.controller;

import com.yurepires.lazydeploy.dto.response.ServerSearchResponse;
import com.yurepires.lazydeploy.model.server.ServerDiscoveryProvider;
import com.yurepires.lazydeploy.model.server.ServerSearchQuery;
import com.yurepires.lazydeploy.service.validation.ServerSearchQueryPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    private final ServerSearchQueryPolicy queryPolicy;

    public ServerSearchController(
            ServerDiscoveryProvider provider,
            ServerSearchQueryPolicy queryPolicy
    ) {
        this.provider = provider;
        this.queryPolicy = queryPolicy;
    }

    @GetMapping("/search")
    public List<ServerSearchResponse> search(
            @RequestParam("query") String query,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        String normalizedQuery = queryPolicy.validate(query);
        ServerSearchQuery searchQuery = new ServerSearchQuery(normalizedQuery, limit);

        return provider.search(searchQuery)
                .stream()
                .map(ServerSearchResponse::from)
                .toList();
    }
}
