package com.ndzela.admin.payment;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.infrastructure.email.EmailProvider;
import com.ndzela.admin.payment.application.ArrearsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArrearsJobTest extends AbstractArrearsTest {
    @Test
    void nothingHappensBeforeTheDeadline() throws Exception {
        arrears.evaluateAll(firstDue.minusSeconds(60));
        assertEquals("NONE", level());
        verifyNoInteractions(email);
    }

    @Test
    void oneUnpaidDayIsAReminderWithOneEmailAndNoRepeat() throws Exception {
        evaluateWhenUnpaidDaysAre(1);
        assertEquals("YELLOW", level());
        verify(email, times(1)).sendHtmlEmail(eq("driver" + jdbc.queryForObject("select substring(email from 7 for 4) from drivers", String.class) + "@ndzela.test"), any(), eq("arrears-email"), any());
        assertEquals(1, auditRows("ARREARS_LEVEL_REACHED"));

        arrears.evaluateAll(dueOfRentDay(1).plusSeconds(7200));
        arrears.evaluateAll(dueOfRentDay(1).plusSeconds(10800));
        verify(email, times(1)).sendHtmlEmail(any(), any(), any(), any());
        assertEquals(1, rows("select count(*) from arrears_events where level = 'YELLOW'"));
    }

    @Test
    void twoUnpaidDaysAreUrgentAndEachStepHappensOnce() throws Exception {
        evaluateWhenUnpaidDaysAre(1);
        evaluateWhenUnpaidDaysAre(2);
        assertEquals("DARK_YELLOW", level());
        arrears.evaluateAll(dueOfRentDay(2).plusSeconds(7200));
        verify(email, times(2)).sendHtmlEmail(any(), any(), any(), any());
        assertEquals("ACTIVE", jdbc.queryForObject("select status from drivers", String.class));
        assertEquals(0, rows("select count(*) from ledger_entries"), "no penalty while the country's penalty is 0");
    }

    @Test
    void aPenaltyIsChargedOncePerUnpaidDayWhenTheCountrySetsOne() throws Exception {
        jdbc.update("update countries set late_penalty = 5000 where code = 'GA'");
        evaluateAt(49);
        evaluateAt(50);
        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'PENALTY' and amount = 5000"), "only the day unpaid for more than 48 hours");
        send(get("/api/v1/payments/accounts/" + driver), viewer).andExpect(status().isOk());
    }

    @Test
    void threeUnpaidDaysSuspendTheDriverAutomaticallyAndStaffAreAlerted() throws Exception {
        evaluateWhenUnpaidDaysAre(3);
        assertEquals("RED", level());
        assertEquals("SUSPENDED", jdbc.queryForObject("select status from drivers", String.class));
        assertEquals(true, jdbc.queryForObject("select status_reason from drivers", String.class).startsWith("Impayés"));
        assertEquals(1, auditRows("DRIVER_SUSPENDED"));
        assertEquals(1, rows("select count(*) from notifications n join users u on u.id = n.user_id where n.type = 'ARREARS_RED' and u.email = ?", admin.getEmail()));
        assertEquals(0, rows("select count(*) from notifications n join users u on u.id = n.user_id where n.type = 'ARREARS_RED' and u.email = ?", viewer.getEmail()));

        int alerts = rows("select count(*) from notifications where type = 'ARREARS_RED'");
        assertEquals(2, alerts, "the administrator and the global super admin");
        arrears.evaluateAll(dueOfRentDay(3).plusSeconds(7200));
        assertEquals(1, auditRows("DRIVER_SUSPENDED"));
        assertEquals(alerts, rows("select count(*) from notifications where type = 'ARREARS_RED'"));
        verify(email, times(1)).sendHtmlEmail(any(), any(), any(), any());
    }

    @Test
    void payingEverythingClearsTheLevelAtOnceAndTheNextRunRestoresTheDriverWithoutAnAdministrator() throws Exception {
        evaluateWhenUnpaidDaysAre(3);
        UUID request = UUID.randomUUID();
        postJson("/api/v1/payments", "{\"driverId\":\"" + driver + "\",\"amount\":1000000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"R1\",\"requestId\":\"" + request + "\"}", manager).andExpect(status().isCreated());
        assertEquals("NONE", level());
        assertEquals("SUSPENDED", jdbc.queryForObject("select status from drivers", String.class));

        evaluateWhenUnpaidDaysAre(3);

        assertEquals("ACTIVE", jdbc.queryForObject("select status from drivers", String.class));
        assertEquals(1, auditRows("DRIVER_REACTIVATED"));
    }

    @Test
    void anAlreadyBlockedDriverIsNotSuspendedAgain() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/block", REASON, admin).andExpect(status().isOk());
        evaluateWhenUnpaidDaysAre(3);
        assertEquals("BLOCKED", jdbc.queryForObject("select status from drivers", String.class));
        assertEquals(0, auditRows("DRIVER_SUSPENDED"));
    }

    @Test
    void theDriversListFiltersAndCountsLateDrivers() throws Exception {
        evaluateWhenUnpaidDaysAre(1);
        send(get("/api/v1/drivers?late=true"), manager).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].arrears.level").value("YELLOW"));
        send(get("/api/v1/drivers?late=false"), manager).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/drivers/stats"), manager).andExpect(jsonPath("$.late").value(1));
        send(get("/api/v1/payments/accounts?filter=YELLOW"), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/accounts?filter=LATE"), manager).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].overdueAmount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(23000)));
        send(get("/api/v1/payments/summary"), manager).andExpect(jsonPath("$.yellow").value(1)).andExpect(jsonPath("$.late").value(1)).andExpect(jsonPath("$.totalOverdue").value(org.hamcrest.Matchers.greaterThanOrEqualTo(23000)));
    }

    @Test
    void riderWhoReachesThreeDaysAtNoonIsAtRiskBeforeAndSuspendedAfter() throws Exception {
        arrears.evaluateAll(dueOfRentDay(3).minusSeconds(6 * 3600));
        send(get("/api/v1/payments/summary"), manager).andExpect(jsonPath("$.red").value(0)).andExpect(jsonPath("$.atRisk").value(1))
                .andExpect(jsonPath("$.totalOverdue").value(46000)).andExpect(jsonPath("$.totalAwaiting").value(23000));
        assertEquals("DARK_YELLOW", level());
        assertEquals("ACTIVE", jdbc.queryForObject("select status from drivers", String.class));
        send(get("/api/v1/payments/accounts?filter=DARK_YELLOW"), manager).andExpect(jsonPath("$.items[0].awaitingDays").value(1)).andExpect(jsonPath("$.items[0].awaitingAmount").value(23000));

        arrears.evaluateAll(dueOfRentDay(3).plusSeconds(60));
        send(get("/api/v1/payments/summary"), manager).andExpect(jsonPath("$.red").value(1)).andExpect(jsonPath("$.atRisk").value(0))
                .andExpect(jsonPath("$.totalOverdue").value(69000)).andExpect(jsonPath("$.totalAwaiting").value(0));
        assertEquals("SUSPENDED", jdbc.queryForObject("select status from drivers", String.class));
    }

    @Test
    void theArrearsListPutsTheMostUnpaidDaysFirstAndLeavesOutInactiveDrivers() throws Exception {
        String other = id(createDriver(manager, DriverJson.create(newVehicle(manager), jdbc.queryForObject("select id::text from training_centers limit 1", String.class))).andExpect(status().isCreated()));
        evaluateWhenUnpaidDaysAre(2);
        postJson("/api/v1/payments", "{\"driverId\":\"" + other + "\",\"amount\":23000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"R-OTHER\",\"requestId\":\"" + UUID.randomUUID() + "\"}", manager).andExpect(status().isCreated());
        evaluateWhenUnpaidDaysAre(2);

        send(get("/api/v1/payments/accounts?filter=ALL"), manager).andExpect(jsonPath("$.items[0].driverId").value(driver)).andExpect(jsonPath("$.items[0].unpaidDays").value(2))
                .andExpect(jsonPath("$.items[1].driverId").value(other)).andExpect(jsonPath("$.items[1].unpaidDays").value(1));

        jdbc.update("update drivers set status = 'INACTIVE' where id = ?::uuid", other);
        send(get("/api/v1/payments/accounts?filter=ALL"), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/summary"), manager).andExpect(jsonPath("$.yellow").value(0)).andExpect(jsonPath("$.upToDate").value(0));
    }

    @Test
    void aDeclaredLeaveRemovesTheRentOfThoseDays() throws Exception {
        LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);
        while (tomorrow.getDayOfWeek() == DayOfWeek.SUNDAY) {
            tomorrow = tomorrow.plusDays(1);
        }
        postJson("/api/v1/drivers/" + driver + "/leaves", "{\"type\":\"SINGLE_DAY\",\"startDate\":\"" + tomorrow + "\",\"reason\":\"PUBLIC_HOLIDAY\"}", manager).andExpect(status().isCreated());
        arrears.evaluateAll(tomorrow.atTime(13, 0).atZone(ZONE).toInstant());
        assertEquals("NONE", level(), "the only rent due by then was the waived day");
    }
}
