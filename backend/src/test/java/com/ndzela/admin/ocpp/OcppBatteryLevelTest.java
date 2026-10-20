package com.ndzela.admin.ocpp;

import com.ndzela.admin.infrastructure.ocpp.FleetException;
import com.ndzela.admin.infrastructure.ocpp.FleetPage;
import com.ndzela.admin.infrastructure.ocpp.FleetRateLimitedException;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.ndzela.admin.support.FakeFleetClient.recent;
import static com.ndzela.admin.support.FakeFleetClient.withSoc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The battery level read for each session, and the live power
class OcppBatteryLevelTest extends AbstractOcppSyncTest {
    private Integer soc(String column, String transactionId) {
        return jdbc.queryForObject("select " + column + " from charging_sessions where transaction_id = ?", Integer.class, transactionId);
    }

    @Test
    void theStartLevelComesFromTheSyncAndTheMeteringOnlyMovesTheLatestLevel() {
        fleet.serve("", new FleetPage(List.of(withSoc(recent("7", 5, null), 55, null)), "c1", false));
        fleet.serveMetering("7", 56);
        sync.sync();
        assertEquals(55, soc("soc_before", "7"));
        assertEquals(56, soc("soc_after", "7"));

        fleet.serveMetering("7", 63);
        sync.sync();
        assertEquals(55, soc("soc_before", "7"));
        assertEquals(63, soc("soc_after", "7"));
    }

    @Test
    void aMeteringReadingNeverSetsTheStartLevelNorStoresTheZeroOfTheChargePoint() {
        fleet.serve("", new FleetPage(List.of(recent("12", 5, null)), "c1", false));
        fleet.serveMetering("12", 0);
        sync.sync();
        assertNull(soc("soc_before", "12"));
        assertNull(soc("soc_after", "12"));

        fleet.serveMetering("12", 40);
        sync.sync();
        assertNull(soc("soc_before", "12"));
        assertEquals(40, soc("soc_after", "12"));
    }

    @Test
    void aSessionFirstSeenFinishedGetsOnlyTheFinalLevelAndThenIsLeftAlone() {
        fleet.serve("", new FleetPage(List.of(recent("8", 20, 10)), "c1", false));
        fleet.serveMetering("8", 63);
        sync.sync();
        assertNull(soc("soc_before", "8"));
        assertEquals(63, soc("soc_after", "8"));

        sync.sync();
        assertEquals(1, fleet.meteringAsked().size(), "once the final level is known it is not asked again");
    }

    @Test
    void aReadingWithoutALevelLeavesTheBatteryEmpty() {
        fleet.serve("", new FleetPage(List.of(recent("9", 20, 10)), "c1", false));
        fleet.serveMetering("9", null);
        sync.sync();
        assertNull(soc("soc_after", "9"));
    }

    @Test
    void oldSessionsAreNeverAsked() {
        fleet.serve("", new FleetPage(List.of(recent("10", 60 * 30, 60 * 29), recent("11", 60 * 3, 60 * 2)), "c1", false));
        sync.sync();
        assertTrue(fleet.meteringAsked().isEmpty());
    }

    @Test
    void aBatteryProblemNeverFailsTheSyncButARateLimitPausesIt() {
        fleet.serve("", new FleetPage(List.of(recent("12", 5, null)), "c1", false));
        fleet.failMeteringWith(new FleetException("down"));
        assertEquals(1, sync.sync());
        assertEquals(1, sessionCount());
        assertNull(jdbc.queryForObject("select last_error from ocpp_sync_state", String.class));

        fleet.failMeteringWith(new FleetRateLimitedException(Duration.ofSeconds(30)));
        sync.sync();
        assertTrue(Boolean.TRUE.equals(redis.hasKey("ocpp:sync:backoff")));
    }

    @Test
    void atMostFiveSessionsAreReadPerRun() {
        fleet.serve("", new FleetPage(List.of(recent("21", 5, null), recent("22", 5, null), recent("23", 5, null), recent("24", 5, null), recent("25", 5, null), recent("26", 5, null),
                recent("27", 5, null)), "c1", false));
        sync.sync();
        assertEquals(5, fleet.meteringAsked().size());
    }

    @Test
    void thePowerIsKeptWhileTheSessionRunsAndClearedWhenItEnds() {
        fleet.serve("", new FleetPage(List.of(recent("30", 5, null)), "c1", false));
        fleet.serveMetering("30", 40, "7.2");
        sync.sync();
        assertEquals(0, new java.math.BigDecimal("7.2").compareTo(jdbc.queryForObject("select power_kw from charging_sessions", java.math.BigDecimal.class)));

        fleet.serve("c1", new FleetPage(List.of(recent("30", 5, 1)), "c2", false));
        fleet.serveMetering("30", 48, "0");
        sync.sync();
        assertNull(jdbc.queryForObject("select power_kw from charging_sessions", java.math.BigDecimal.class));
        assertEquals(48, soc("soc_after", "30"));
    }
}
