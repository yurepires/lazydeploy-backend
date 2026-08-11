package com.yurepires.lazydeploy.battlefield;

import com.yurepires.lazydeploy.config.Bf4Properties;
import com.yurepires.lazydeploy.dto.Bf4ServerPageResponse;
import com.yurepires.lazydeploy.dto.Bf4ServerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BfListClientTest {

    private static final String TARGET_GUID = "494eef2a-0155-4df3-91b0-58f565cc5f53";
    private static final String TARGET_IP = "131.196.199.123";
    private static final int TARGET_PORT = 25226;

    @Test
    void shouldFindServerByGuidOnNextPage() {
        BfListClient client = spy(createClient());
        Bf4ServerResponse lastServer = server("other-guid", "10.0.0.1", 1234, "Other");
        Bf4ServerResponse target = server(TARGET_GUID, TARGET_IP, TARGET_PORT, "LOST");

        doReturn(new Bf4ServerPageResponse(List.of(lastServer), "snapshot-cursor", true))
                .when(client).getFirstServerPage();
        doReturn(new Bf4ServerPageResponse(List.of(target), "snapshot-cursor", false))
                .when(client).getNextServerPage("snapshot-cursor", "10.0.0.1:1234");

        Optional<Bf4ServerResponse> result = client.findServer(TARGET_GUID);

        assertThat(result).contains(target);
        verify(client).getNextServerPage("snapshot-cursor", "10.0.0.1:1234");
    }

    @Test
    void shouldNotMatchAnotherGuidEvenWhenAddressIsTheSame() {
        BfListClient client = spy(createClient());
        Bf4ServerResponse target = server("different-guid", TARGET_IP, TARGET_PORT, "LOST");

        doReturn(new Bf4ServerPageResponse(List.of(target), "snapshot-cursor", false))
                .when(client).getFirstServerPage();

        Optional<Bf4ServerResponse> result = client.findServer(TARGET_GUID);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenSnapshotEnds() {
        BfListClient client = spy(createClient());

        doReturn(new Bf4ServerPageResponse(List.of(), "snapshot-cursor", false))
                .when(client).getFirstServerPage();

        Optional<Bf4ServerResponse> result = client.findServer(TARGET_GUID);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldRestartFromFirstPageWhenCursorExpires() {
        BfListClient client = spy(createClient());
        Bf4ServerResponse target = server(TARGET_GUID, TARGET_IP, TARGET_PORT, "LOST");
        WebClientResponseException gone = WebClientResponseException.create(
                410,
                "Gone",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );

        doThrow(gone)
                .doReturn(new Bf4ServerPageResponse(List.of(target), "new-cursor", false))
                .when(client).getFirstServerPage();

        Optional<Bf4ServerResponse> result = client.findServer(TARGET_GUID);

        assertThat(result).contains(target);
        verify(client, times(2)).getFirstServerPage();
    }

    private BfListClient createClient() {
        Bf4Properties properties = new Bf4Properties(
                "https://api.bflist.io/v2/bf4",
                new Bf4Properties.Server(TARGET_GUID),
                new Bf4Properties.Monitoring(Duration.ofSeconds(30)),
                new Bf4Properties.Api(100)
        );

        return new BfListClient(mock(WebClient.class), properties);
    }

    private Bf4ServerResponse server(String guid, String ip, int port, String name) {
        return new Bf4ServerResponse(
                guid,
                ip,
                port,
                name,
                10,
                64,
                "MP_Siege",
                "Siege of Shanghai",
                "CONQUEST",
                0,
                1,
                600
        );
    }
}
