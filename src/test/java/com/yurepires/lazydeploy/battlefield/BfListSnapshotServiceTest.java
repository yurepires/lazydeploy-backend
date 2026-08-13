package com.yurepires.lazydeploy.battlefield;

import com.yurepires.lazydeploy.battlefield.dto.Bf4ServerPageResponse;
import com.yurepires.lazydeploy.battlefield.dto.Bf4ServerResponse;
import com.yurepires.lazydeploy.domain.server.ServerCatalogSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BfListSnapshotServiceTest {

    @Test
    void shouldNormalizeAllPagesIntoOneCatalog() {
        BfListClient client = mock(BfListClient.class);
        BfListSnapshotService service = new BfListSnapshotService(client, new BfListServerMapper());
        Bf4ServerResponse first = response("one", "10.0.0.1", 1001);
        Bf4ServerResponse second = response("two", "10.0.0.2", 1002);
        when(client.getFirstServerPage()).thenReturn(page(first, true));
        when(client.getNextServerPage("cursor", "10.0.0.1:1001"))
                .thenReturn(page(second, false));

        ServerCatalogSnapshot catalog = service.fetchSnapshot().orElseThrow();

        assertThat(catalog.servers()).hasSize(2);
        assertThat(catalog.byGuid()).containsKeys("one", "two");
        verify(client).getNextServerPage("cursor", "10.0.0.1:1001");
    }

    @Test
    void shouldRestartCollectionAfterGone() {
        BfListClient client = mock(BfListClient.class);
        BfListSnapshotService service = new BfListSnapshotService(client, new BfListServerMapper());
        WebClientResponseException gone = WebClientResponseException.create(
                410, "Gone", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
        );
        when(client.getFirstServerPage()).thenThrow(gone).thenReturn(page(response("one", "10.0.0.1", 1001), false));

        Optional<ServerCatalogSnapshot> result = service.fetchSnapshot();

        assertThat(result).isPresent();
        verify(client, times(2)).getFirstServerPage();
    }

    private Bf4ServerPageResponse page(Bf4ServerResponse server, boolean hasMore) {
        return new Bf4ServerPageResponse(List.of(server), "cursor", hasMore);
    }

    private Bf4ServerResponse response(String guid, String ip, int port) {
        return new Bf4ServerResponse(
                guid, ip, port, "Server", 50, 64, "MAP_A", "Map A", "CONQUEST", 0, 1, 300
        );
    }
}
