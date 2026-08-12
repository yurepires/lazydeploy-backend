package com.yurepires.lazydeploy.notification.rule;

import com.yurepires.lazydeploy.config.Bf4Properties;
import com.yurepires.lazydeploy.monitoring.event.MapChangedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRuleServiceTest {

    private final NotificationRuleService service = new NotificationRuleService();

    @Test
    void shouldApproveWhenMapIsFavoriteAndMinimumPlayersReached() {
        NotificationDecision decision = service.evaluate(server(true, 40), event("MP_Siege", 55));

        assertThat(decision).isEqualTo(new NotificationDecision(
                true,
                NotificationDecisionReason.ALL_RULES_MATCHED
        ));
    }

    @Test
    void shouldRejectWhenMapIsNotFavorite() {
        NotificationDecision decision = service.evaluate(server(true, 40), event("XP0_Caspian", 60));

        assertThat(decision).isEqualTo(new NotificationDecision(
                false,
                NotificationDecisionReason.MAP_NOT_FAVORITE
        ));
    }

    @Test
    void shouldRejectWhenMinimumPlayersIsNotReached() {
        NotificationDecision decision = service.evaluate(server(true, 40), event("MP_Siege", 28));

        assertThat(decision).isEqualTo(new NotificationDecision(
                false,
                NotificationDecisionReason.MIN_PLAYERS_NOT_REACHED
        ));
    }

    @Test
    void shouldRejectWhenServerIsDisabled() {
        NotificationDecision decision = service.evaluate(server(false, 40), event("MP_Siege", 55));

        assertThat(decision).isEqualTo(new NotificationDecision(
                false,
                NotificationDecisionReason.SERVER_DISABLED
        ));
    }

    private Bf4Properties.MonitoredServer server(boolean enabled, int minPlayers) {
        return new Bf4Properties.MonitoredServer(
                "guid", "10.0.0.1", 25226, "Server", enabled, minPlayers, Set.of("MP_Siege")
        );
    }

    private MapChangedEvent event(String currentMap, int players) {
        return new MapChangedEvent(
                "guid",
                "Server",
                "10.0.0.1",
                25226,
                "MP_Prison",
                "Operation Locker",
                currentMap,
                currentMap,
                players,
                64,
                "CONQUEST",
                300,
                Instant.parse("2026-08-11T12:00:00Z")
        );
    }
}
