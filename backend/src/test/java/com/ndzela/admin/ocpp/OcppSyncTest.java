package com.ndzela.admin.ocpp;

import com.ndzela.admin.infrastructure.ocpp.FleetException;
import com.ndzela.admin.infrastructure.ocpp.FleetPage;
import com.ndzela.admin.infrastructure.ocpp.FleetRateLimitedException;
import com.ndzela.admin.ocpp.application.OcppSyncService;
import com.ndzela.admin.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;

import static com.ndzela.admin.support.FakeFleetClient.recent;
import static com.ndzela.admin.support.FakeFleetClient.session;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcppSyncTest extends AbstractIntegrationTest {
    @Autowired OcppSyncService sync;

    private String cursor() {
        return jdbc.query("select sync_cursor from ocpp_sync_state", rs -> rs.next() ? rs.getString(1) : null);
    }

    private int sessionCount() {
        return jdbc.queryForObject("select count(*) from charging_sessions", Integer.class);
    }

    @Test
    void pagesAreFollowedAndTheCursorIsSavedExactlyAsReceived() {
        fleet.serve("", new FleetPage(List.of(session("1", "COMPLETED", "10", 3500L, "2026-10-04T09:00:00Z")), "eyJwIjoiSiJ9.sig-1", true));
        fleet.serve("eyJwIjoiSiJ9.sig-1", new FleetPage(List.of(session("2", "COMPLETED", "5.5", 1925L, "2026-10-04T10:00:00Z")), "eyJwIjoiSiJ9.sig-2", false));

        assertEquals(2, sync.sync());

        assertEquals(List.of("", "eyJwIjoiSiJ9.sig-1"), fleet.asked());
        assertEquals("eyJwIjoiSiJ9.sig-2", cursor());
        assertEquals(2, sessionCount());
        assertEquals(5500, jdbc.queryForObject("select energy_wh from charging_sessions where transaction_id = '2'", Integer.class));
    }

    @Test
    void anEmptyPageStillMovesTheCursorAndTheNextRunResumesFromIt() {
        fleet.serve("", new FleetPage(List.of(), "start-cursor", false));

        assertEquals(0, sync.sync());
        assertEquals("start-cursor", cursor());

        sync.sync();
        assertEquals(List.of("", "start-cursor"), fleet.asked(), "resumes from the saved cursor, not from the beginning");
    }

    @Test
    void replayingAPageNeverDuplicatesASession() {
        fleet.serve("", new FleetPage(List.of(session("1", "CHARGING", "1", 350L, "2026-10-04T09:00:00Z")), "c1", false));
        sync.sync();
        jdbc.update("update ocpp_sync_state set sync_cursor = null");
        sync.sync();

        assertEquals(1, sessionCount());
    }

    @Test
    void aLaterVersionReplacesTheKnownOneAndAnOlderOneNeverDoes() {
        fleet.serve("", new FleetPage(List.of(session("1", "CHARGING", "1", 350L, "2026-10-04T09:00:00Z")), "c1", false));
        sync.sync();
        fleet.serve("c1", new FleetPage(List.of(session("1", "COMPLETED", "21", 7350L, "2026-10-04T09:40:00Z")), "c2", false));
        sync.sync();
        assertEquals("COMPLETED", jdbc.queryForObject("select status from charging_sessions", String.class));

        fleet.serve("c2", new FleetPage(List.of(session("1", "CHARGING", "1", 350L, "2026-10-04T09:00:00Z")), "c3", false));
        sync.sync();

        assertEquals("COMPLETED", jdbc.queryForObject("select status from charging_sessions", String.class));
        assertEquals(7350, jdbc.queryForObject("select cost_minor from charging_sessions", Integer.class));
        assertEquals("c3", cursor(), "the cursor still advances");
    }

    @Test
    void aFailedSyncChangesNothingAndKeepsTheReason() {
        fleet.failWith(new FleetException("down"));

        sync.sync();
        assertNull(cursor());
        assertEquals(0, sessionCount());
        assertNotNull(jdbc.queryForObject("select last_error from ocpp_sync_state", String.class));
    }

    @Test
    void whenThePlatformSaysWaitNothingIsAskedUntilItIsOver() {
        fleet.failWith(new FleetRateLimitedException(Duration.ofSeconds(30)));
        sync.runIfDue();
        assertEquals(1, fleet.asked().size());
        assertTrue(Boolean.TRUE.equals(redis.hasKey("ocpp:sync:backoff")));

        sync.requestSync();
        sync.runIfDue();
        assertEquals(1, fleet.asked().size(), "paused while the platform asked us to wait");
    }

    @Test
    void oneWebhookOrManyMakeOneSyncAndNothingRunsWhileNotDue() {
        fleet.serve("", new FleetPage(List.of(), "c1", false));
        sync.runIfDue();
        assertEquals(1, fleet.asked().size(), "the first run is due");

        sync.runIfDue();
        assertEquals(1, fleet.asked().size(), "not due again within the interval");

        sync.requestSync();
        sync.requestSync();
        sync.requestSync();
        sync.runIfDue();
        sync.runIfDue();
        assertEquals(2, fleet.asked().size(), "three webhooks made one sync");
    }

    @Test
    void aSyncAlreadyRunningIsNotStartedTwice() {
        redis.opsForValue().set("ocpp:sync:lock", "another-instance", Duration.ofMinutes(1));

        assertEquals(0, sync.sync());
        assertTrue(fleet.asked().isEmpty());
        assertEquals("another-instance", redis.opsForValue().get("ocpp:sync:lock"), "someone else's lock is left alone");
    }

    private Integer soc(String column, String transactionId) {
        return jdbc.queryForObject("select " + column + " from charging_sessions where transaction_id = ?", Integer.class, transactionId);
    }

    @Test
    void aRunningSessionKeepsTheFirstBatteryLevelSeenAsBeforeAndTheLatestAsAfter() {
        fleet.serve("", new FleetPage(List.of(recent("7", 5, null)), "c1", false));
        fleet.serveMetering("7", 40);
        sync.sync();
        assertEquals(40, soc("soc_before", "7"));
        assertEquals(40, soc("soc_after", "7"));

        fleet.serveMetering("7", 47);
        sync.sync();
        assertEquals(40, soc("soc_before", "7"));
        assertEquals(47, soc("soc_after", "7"));
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
