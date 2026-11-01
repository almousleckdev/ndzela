package com.ndzela.admin.ocpp;

import com.ndzela.admin.infrastructure.ocpp.FleetException;
import com.ndzela.admin.infrastructure.ocpp.FleetCursorExpiredException;
import com.ndzela.admin.infrastructure.ocpp.FleetPage;
import com.ndzela.admin.infrastructure.ocpp.FleetRateLimitedException;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.ndzela.admin.support.FakeFleetClient.session;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The sync loop: pages, cursor, versions, failures, rate limits and the webhook trigger
class OcppSyncRunTest extends AbstractOcppSyncTest {
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
    void aCursorThePlatformRefusesIsDroppedAndTheSyncStartsOverOnce() {
        fleet.serve("", new FleetPage(List.of(session("1", "CHARGING", "1", 350L, "2026-10-04T09:00:00Z")), "c1", false));
        sync.sync();
        fleet.expireCursor("c1");
        fleet.serve("", new FleetPage(List.of(session("1", "CHARGING", "1", 350L, "2026-10-04T09:00:00Z")), "c2", false));

        sync.sync();

        assertEquals(List.of("", "c1", ""), fleet.asked(), "the refused cursor is tried once, then the sync restarts from the beginning");
        assertEquals("c2", cursor());
        assertEquals(1, sessionCount(), "replaying the sessions duplicates nothing");
        assertNull(jdbc.queryForObject("select last_error from ocpp_sync_state", String.class));
    }

    @Test
    void aRefusalWithoutASavedCursorIsRecordedAndNotRetried() {
        fleet.failWith(new FleetCursorExpiredException(null));

        assertEquals(0, sync.sync());

        assertEquals(List.of(""), fleet.asked());
        assertNotNull(jdbc.queryForObject("select last_error from ocpp_sync_state", String.class));
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
}
