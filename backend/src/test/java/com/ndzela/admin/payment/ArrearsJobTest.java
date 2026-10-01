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

class ArrearsJobTest extends AbstractDriverTest {
    private static final ZoneId ZONE = ZoneId.of("Africa/Libreville");

    @MockitoBean
    EmailProvider email;

    @Autowired
    ArrearsService arrears;

    private String driver;
    private Instant firstDue;

    @BeforeEach
    void setup() throws Exception {
        driver = id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated()));
        LocalDate day = LocalDate.now(ZONE).plusDays(1);
        while (day.getDayOfWeek() == DayOfWeek.SUNDAY) {
            day = day.plusDays(1);
        }
        firstDue = day.plusDays(1).atTime(12, 0).atZone(ZONE).toInstant();
    }

    private void evaluateAt(long hoursAfterFirstDeadline) {
        arrears.evaluateAll(firstDue.plusSeconds(hoursAfterFirstDeadline * 3600));
    }

    private String level() throws Exception {
        return com.jayway.jsonpath.JsonPath.read(send(get("/api/v1/drivers/" + driver), manager).andReturn().getResponse().getContentAsString(), "$.arrears.level");
    }

    @Test
    void nothingHappensBeforeTheDeadline() throws Exception {
        arrears.evaluateAll(firstDue.minusSeconds(60));
        assertEquals("NONE", level());
        verifyNoInteractions(email);
    }

    @Test
    void anUnpaidRentBecomesLateThenYellowWithOneEmailAndNoRepeat() throws Exception {
        evaluateAt(1);
        assertEquals("LATE", level());
        verifyNoInteractions(email);

        evaluateAt(25);
        assertEquals("YELLOW", level());
        verify(email, times(1)).sendHtmlEmail(eq("driver" + jdbc.queryForObject("select substring(email from 7 for 4) from drivers", String.class) + "@ndzela.test"), any(), eq("arrears-email"), any());
        assertEquals(1, auditRows("ARREARS_LEVEL_REACHED"));

        evaluateAt(26);
        evaluateAt(27);
        verify(email, times(1)).sendHtmlEmail(any(), any(), any(), any());
        assertEquals(1, rows("select count(*) from arrears_events where level = 'YELLOW'"));
    }

    @Test
    void theNextLevelsEscalateOnceEach() throws Exception {
        evaluateAt(25);
        evaluateAt(49);
        assertEquals("DARK_YELLOW", level());
        evaluateAt(50);
        verify(email, times(2)).sendHtmlEmail(any(), any(), any(), any());
        assertEquals("ACTIVE", jdbc.queryForObject("select status from drivers", String.class));
        assertEquals(0, rows("select count(*) from ledger_entries"), "no penalty while the country's penalty is 0");
    }

    @Test
    void aPenaltyIsChargedOncePerUnpaidDayWhenTheCountrySetsOne() throws Exception {
        jdbc.update("update countries set late_penalty = 5000 where code = 'GA'");
        evaluateAt(49);
        evaluateAt(50);
        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'PENALTY' and amount = 5000"));
        send(get("/api/v1/payments/accounts/" + driver), viewer).andExpect(status().isOk());
        assertEquals("DARK_YELLOW", level());
        assertEquals(0, new java.math.BigDecimal("5000").compareTo(jdbc.queryForObject("select sum(amount) from ledger_entries where entry_type = 'PENALTY'", java.math.BigDecimal.class)));
    }

    @Test
    void after72HoursTheDriverIsSuspendedAutomaticallyAndStaffAreAlerted() throws Exception {
        evaluateAt(73);
        assertEquals("RED", level());
        assertEquals("SUSPENDED", jdbc.queryForObject("select status from drivers", String.class));
        assertEquals(true, jdbc.queryForObject("select status_reason from drivers", String.class).startsWith("Impayés"));
        assertEquals(1, auditRows("DRIVER_SUSPENDED"));
        assertEquals(1, rows("select count(*) from notifications n join users u on u.id = n.user_id where n.type = 'ARREARS_RED' and u.email = ?", admin.getEmail()));
        assertEquals(0, rows("select count(*) from notifications n join users u on u.id = n.user_id where n.type = 'ARREARS_RED' and u.email = ?", viewer.getEmail()));

        int alerts = rows("select count(*) from notifications where type = 'ARREARS_RED'");
        assertEquals(2, alerts, "the administrator and the global super admin");
        evaluateAt(74);
        assertEquals(1, auditRows("DRIVER_SUSPENDED"));
        assertEquals(alerts, rows("select count(*) from notifications where type = 'ARREARS_RED'"));
        verify(email, times(1)).sendHtmlEmail(any(), any(), any(), any());
    }

    @Test
    void payingEverythingClearsTheLevelButOnlyAnAdministratorLiftsTheSuspension() throws Exception {
        evaluateAt(73);
        UUID request = UUID.randomUUID();
        postJson("/api/v1/payments", "{\"driverId\":\"" + driver + "\",\"amount\":1000000,\"method\":\"CASH\",\"requestId\":\"" + request + "\"}", manager).andExpect(status().isCreated());
        assertEquals("NONE", level());
        assertEquals("SUSPENDED", jdbc.queryForObject("select status from drivers", String.class));

        send(post("/api/v1/drivers/" + driver + "/reactivate"), manager).andExpect(status().isForbidden());
        send(post("/api/v1/drivers/" + driver + "/reactivate"), admin).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void anAlreadyBlockedDriverIsNotSuspendedAgain() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/block", REASON, admin).andExpect(status().isOk());
        evaluateAt(73);
        assertEquals("BLOCKED", jdbc.queryForObject("select status from drivers", String.class));
        assertEquals(0, auditRows("DRIVER_SUSPENDED"));
    }

    @Test
    void theDriversListFiltersAndCountsLateDrivers() throws Exception {
        evaluateAt(30);
        send(get("/api/v1/drivers?late=true"), manager).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].arrears.level").value("YELLOW"));
        send(get("/api/v1/drivers?late=false"), manager).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/drivers/stats"), manager).andExpect(jsonPath("$.late").value(1));
        send(get("/api/v1/payments/accounts?filter=YELLOW"), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/accounts?filter=LATE"), manager).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].overdueAmount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(23000)));
        send(get("/api/v1/payments/summary"), manager).andExpect(jsonPath("$.yellow").value(1)).andExpect(jsonPath("$.late").value(1)).andExpect(jsonPath("$.totalOverdue").value(org.hamcrest.Matchers.greaterThanOrEqualTo(23000)));
    }

    @Test
    void aDeclaredLeaveRemovesTheRentOfThoseDays() throws Exception {
        LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);
        postJson("/api/v1/drivers/" + driver + "/leaves", "{\"type\":\"SINGLE_DAY\",\"startDate\":\"" + tomorrow + "\",\"reason\":\"PUBLIC_HOLIDAY\"}", manager).andExpect(status().isCreated());
        arrears.evaluateAll(tomorrow.plusDays(1).atTime(13, 0).atZone(ZONE).toInstant());
        assertEquals(tomorrow.getDayOfWeek() == DayOfWeek.SUNDAY ? "NONE" : "NONE", level(), "the only rent due by then was the waived day");
    }
}
