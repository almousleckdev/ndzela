package com.ndzela.admin.charging;

import com.ndzela.admin.charging.application.ZellaCommandQueue;
import com.ndzela.admin.charging.application.ZellaCommandSender;
import com.ndzela.admin.charging.application.ZellaSuspensionReconciler;
import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// What NDZela tells the Zella app about a suspended or restored driver is queued, retried and cancelled when the opposite order follows.
class ZellaCommandQueueTest extends AbstractZellaFeedTest {
    @Autowired ZellaCommandQueue queue;
    @Autowired ZellaCommandSender sender;
    @Autowired ZellaSuspensionReconciler reconciler;

    private <T> T inCountry(Supplier<T> action) {
        return TenantHolder.callAs(TenantContext.country(country("GA").getId()), action);
    }

    private void reconcile() {
        TenantHolder.callAs(TenantContext.country(country("GA").getId()), () -> {
            reconciler.run();
            return null;
        });
    }

    private UUID driverId() {
        return jdbc.queryForObject("select driver_id from id_tags where customer_id = 'cust-1'", UUID.class);
    }

    private String statusOf(String type) {
        return jdbc.queryForObject("select string_agg(status, ',' order by created_at) from zella_commands where type = ?", String.class, type);
    }

    private int sendDue(Instant now) {
        return inCountry(() -> sender.sendDue(now));
    }

    @Test
    void aSuspendCommandIsSentWithTheOwedAmount() {
        assertTrue(inCountry(() -> queue.suspend(driverId(), 69000)));

        assertEquals(1, sendDue(Instant.now()));

        assertTrue(zella.calls().contains("SUSPEND cust-1 69000"), zella.calls().toString());
        assertEquals("SENT", statusOf("SUSPEND"));
    }

    @Test
    void aFailedSendIsRetriedWithBackoff() {
        inCountry(() -> queue.suspend(driverId(), 69000));
        zella.goDown();

        assertEquals(0, sendDue(Instant.now()));

        assertEquals("PENDING", statusOf("SUSPEND"));
        assertEquals(1, jdbc.queryForObject("select attempts from zella_commands", Integer.class));
        OffsetDateTime next = jdbc.queryForObject("select next_attempt_at from zella_commands", OffsetDateTime.class);
        assertTrue(next.isAfter(OffsetDateTime.now().plusSeconds(30)));
        assertEquals(0, sendDue(Instant.now()), "not due yet");

        zella.comeBack();
        assertEquals(1, sendDue(Instant.now().plusSeconds(180)));
        assertEquals("SENT", statusOf("SUSPEND"));
    }

    @Test
    void restoreCancelsAPendingSuspend() {
        inCountry(() -> queue.suspend(driverId(), 69000));
        inCountry(() -> queue.restore(driverId()));

        assertEquals(1, sendDue(Instant.now()));

        assertEquals("CANCELLED", statusOf("SUSPEND"));
        assertEquals("SENT", statusOf("RESTORE"));
        assertFalse(zella.calls().stream().anyMatch(call -> call.startsWith("SUSPEND")));
        assertTrue(zella.calls().contains("RESTORE cust-1"));
    }

    @Test
    void anUnlinkedDriverQueuesNothing() throws Exception {
        String other = id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "AUTRE"))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated()));

        assertFalse(inCountry(() -> queue.suspend(UUID.fromString(other), 1000)));
        assertFalse(inCountry(() -> queue.restore(UUID.fromString(other))));

        assertEquals(0, jdbc.queryForObject("select count(*) from zella_commands", Integer.class));
    }

    @Test
    void theReconcilerQueuesWhatTheTwoAppsDisagreeOn() {
        jdbc.update("update drivers set status = 'SUSPENDED', suspension_cause = 'ARREARS', overdue_amount = 69000 where id = ?", driverId());

        reconcile();
        reconcile();

        assertEquals("PENDING", statusOf("SUSPEND"));
        assertEquals(1, jdbc.queryForObject("select count(*) from zella_commands where type = 'SUSPEND'", Integer.class));
        assertEquals(69000, jdbc.queryForObject("select owed_minor from zella_commands", Long.class));

        sendDue(Instant.now());
        jdbc.update("update drivers set status = 'ACTIVE', suspension_cause = null where id = ?", driverId());
        reconcile();
        reconcile();

        assertEquals("PENDING", statusOf("RESTORE"));
        assertEquals(1, jdbc.queryForObject("select count(*) from zella_commands where type = 'RESTORE'", Integer.class));
    }

    @Test
    void aManualSuspensionIsNeverSentToZella() {
        jdbc.update("update drivers set status = 'SUSPENDED', suspension_cause = 'MANUAL' where id = ?", driverId());

        reconcile();

        assertEquals(0, jdbc.queryForObject("select count(*) from zella_commands", Integer.class));
    }

    @Test
    void aCommandStuckForAnHourIsCounted() {
        inCountry(() -> queue.suspend(driverId(), 69000));
        zella.goDown();
        sendDue(Instant.now());
        assertEquals(0, (long) inCountry(() -> queue.stuck(OffsetDateTime.now().minusHours(1))));

        jdbc.update("update zella_commands set created_at = now() - interval '2 hours'");

        assertEquals(1, (long) inCountry(() -> queue.stuck(OffsetDateTime.now().minusHours(1))));
    }
}
