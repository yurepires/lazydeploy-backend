package com.yurepires.lazydeploy.battlefield;

import com.yurepires.lazydeploy.config.Bf4Properties;
import com.yurepires.lazydeploy.dto.Bf4ServerPageResponse;
import com.yurepires.lazydeploy.dto.Bf4ServerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BfListSnapshotServiceTest {

    @Test
    void shouldFetchAllPagesAndBuildLookupIndexes() {
        BfListClient client = mock(BfListClient.class);
        BfListSnapshotService service = new BfListSnapshotService(client);
        Bf4ServerResponse first = server("guid-1", "10.0.0.1", 1001, "MP_Prison");
        Bf4ServerResponse second = server("guid-2", "10.0.0.2", 1002, "MP_Siege");

        when(client.getFirstServerPage())
                .thenReturn(new Bf4ServerPageResponse(List.of(first), "cursor", true));
        when(client.getNextServerPage("cursor", "10.0.0.1:1001"))
                .thenReturn(new Bf4ServerPageResponse(List.of(second), "cursor", false));

        Optional<Bf4ServerSnapshot> result = service.fetchSnapshot();

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().servers()).containsExactly(first, second);
        assertThat(result.orElseThrow().find(target("guid-2", "ignored", 1))).contains(second);
        assertThat(result.orElseThrow().find(target("missing", "10.0.0.1", 1001))).contains(first);
        verify(client).getNextServerPage("cursor", "10.0.0.1:1001");
    }

    @Test
    void shouldRestartSnapshotWhenCursorExpires() {
        BfListClient client = mock(BfListClient.class);
        BfListSnapshotService service = new BfListSnapshotService(client);
        Bf4ServerResponse server = server("guid-1", "10.0.0.1", 1001, "MP_Prison");
        WebClientResponseException gone = WebClientResponseException.create(
                410,
                "Gone",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );

        when(client.getFirstServerPage())
                .thenThrow(gone)
                .thenReturn(new Bf4ServerPageResponse(List.of(server), "new-cursor", false));

        Optional<Bf4ServerSnapshot> result = service.fetchSnapshot();

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().servers()).containsExactly(server);
        verify(client, times(2)).getFirstServerPage();
    }

    private Bf4Properties.MonitoredServer target(String guid, String ip, int port) {
        return new Bf4Properties.MonitoredServer(
                guid, ip, port, "Server", true, 0, Set.of("MP_Prison")
        );
    }

    private Bf4ServerResponse server(String guid, String ip, int port, String map) {
        return new Bf4ServerResponse(
                guid, ip, port, "Server", 50, 64, map, map, "CONQUEST", 0, 1, 300
        );
    }
}
