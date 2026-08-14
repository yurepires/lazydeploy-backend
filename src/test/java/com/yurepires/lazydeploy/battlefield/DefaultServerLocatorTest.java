package com.yurepires.lazydeploy.battlefield;

import com.yurepires.lazydeploy.domain.server.MonitoredServer;
import com.yurepires.lazydeploy.domain.server.ServerCatalogSnapshot;
import com.yurepires.lazydeploy.domain.server.ServerIdentifiers;
import com.yurepires.lazydeploy.domain.server.ServerSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static com.yurepires.lazydeploy.TestFixtures.monitored;
import static com.yurepires.lazydeploy.TestFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultServerLocatorTest {
    private final DefaultServerLocator locator = new DefaultServerLocator();

    @Test
    void shouldFindAnyConfiguredServerByGuid() {
        ServerSnapshot server = snapshot("guid-1", "MAP_A", 10);
        MonitoredServer configured = monitored("one", "guid-1", List.of(), List.of());

        assertThat(locator.find(configured, catalog(server))).contains(server);
    }

    @Test
    void shouldFallbackToAddressWhenGuidIsUnavailable() {
        ServerSnapshot server = snapshot("actual-guid", "MAP_A", 10);
        MonitoredServer configured = new MonitoredServer(
                "one", new ServerIdentifiers("missing", "10.0.0.1", 25226),
                "Server", true, List.of(), List.of()
        );

        assertThat(locator.find(configured, catalog(server))).contains(server);
    }

    @Test
    void shouldReturnEmptyWhenServerIsNotPresent() {
        MonitoredServer configured = monitored("one", "missing", List.of(), List.of());
        assertThat(locator.find(configured, catalog())).isEmpty();
    }

    private ServerCatalogSnapshot catalog(ServerSnapshot... servers) {
        return ServerCatalogSnapshot.from(List.of(servers), Instant.now());
    }
}
