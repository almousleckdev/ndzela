package com.ndzela.admin.payment;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DayIncidentApiTest extends AbstractDriverTest {
    private static final ZoneId ZONE = ZoneId.of("Africa/Libreville");

    private String driver;
    private LocalDate pastRentDay;

    @BeforeEach
    void setup() throws Exception {
        driver = id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated()));
        jdbc.update("update contracts set start_date = current_date - 10, end_date = current_date + 300");
        LocalDate day = LocalDate.now(ZONE).minusDays(3);
        while (day.getDayOfWeek() == DayOfWeek.SUNDAY) {
            day = day.minusDays(1);
        }
        pastRentDay = day;
    }

    private String body(LocalDate day, String kind, String cause, Integer hours, String note) {
        return "{\"day\":\"" + day + "\",\"kind\":\"" + kind + "\",\"cause\":\"" + cause + "\"" + (hours == null ? "" : ",\"hours\":" + hours) + (note == null ? "" : ",\"note\":\"" + note + "\"") + "}";
    }

    private ResultActions declare(com.ndzela.admin.user.domain.User as, LocalDate day, String kind, String cause, Integer hours, String note) throws Exception {
        return postJson("/api/v1/drivers/" + driver + "/incidents", body(day, kind, cause, hours, note), as);
    }

    private int due() throws Exception {
        return JsonPath.read(send(get("/api/v1/payments/accounts/" + driver), viewer).andReturn().getResponse().getContentAsString(), "$.overdueAmount");
    }

    private int rent() throws Exception {
        return JsonPath.read(send(get("/api/v1/payments/accounts/" + driver), viewer).andReturn().getResponse().getContentAsString(), "$.dailyRent");
    }

    @Test
    void anElectricityCutOfFiveHoursMakesThatDayAHalfDay() throws Exception {
        int before = due();
        int rent = rent();

        declare(admin, pastRentDay, "HALF_DAY", "ELECTRICITY", 5, "Coupure à la station").andExpect(status().isCreated()).andExpect(jsonPath("$.kind").value("HALF_DAY"))
                .andExpect(jsonPath("$.hours").value(5)).andExpect(jsonPath("$.createdBy").value(admin.getEmail()));

        assertEquals(before - rent / 2, due());
        assertEquals(1, auditRows("DAY_INCIDENT_DECLARED"));
    }

    @Test
    void anElectricityCutOverEightHoursMeansNoRentThatDay() throws Exception {
        int before = due();
        int rent = rent();

        declare(admin, pastRentDay, "STOP", "ELECTRICITY", 9, null).andExpect(status().isCreated());

        assertEquals(before - rent, due());
    }

    @Test
    void theLengthOfAnElectricityCutMustAgreeWithWhatIsDeclared() throws Exception {
        for (Object[] wrong : new Object[][] {{"HALF_DAY", 3}, {"HALF_DAY", 9}, {"STOP", 8}, {"STOP", 4}, {"HALF_DAY", null}}) {
            declare(admin, pastRentDay, (String) wrong[0], "ELECTRICITY", (Integer) wrong[1], null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INCIDENT_HOURS_INVALID"));
        }
        declare(admin, pastRentDay, "HALF_DAY", "ELECTRICITY", 4, null).andExpect(status().isCreated());
        declare(admin, pastRentDay.minusDays(1).getDayOfWeek() == DayOfWeek.SUNDAY ? pastRentDay.minusDays(2) : pastRentDay.minusDays(1), "HALF_DAY", "ELECTRICITY", 8, null).andExpect(status().isCreated());
    }

    @Test
    void anotherCauseNeedsANote() throws Exception {
        declare(admin, pastRentDay, "STOP", "OTHER", null, null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INCIDENT_NOTE_REQUIRED"));
        declare(admin, pastRentDay, "STOP", "OTHER", null, "Panne de la voiture").andExpect(status().isCreated());
    }

    @Test
    void sundayTheFutureAndDaysBeforeTheContractAreRefused() throws Exception {
        LocalDate sunday = pastRentDay;
        while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
            sunday = sunday.minusDays(1);
        }
        declare(admin, sunday, "HALF_DAY", "ELECTRICITY", 5, null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DAY_NOT_BILLABLE"));
        declare(admin, LocalDate.now(ZONE).plusDays(2), "STOP", "ELECTRICITY", 9, null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DAY_OUT_OF_CONTRACT"));
        declare(admin, LocalDate.now(ZONE).minusDays(30), "STOP", "ELECTRICITY", 9, null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DAY_OUT_OF_CONTRACT"));
        assertEquals(0, rows("select count(*) from day_incidents"));
    }

    @Test
    void aDayHasOneIncidentAndCancellingFreesIt() throws Exception {
        int before = due();
        String id = JsonPath.read(declare(admin, pastRentDay, "STOP", "ELECTRICITY", 10, null).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        declare(admin, pastRentDay, "HALF_DAY", "ELECTRICITY", 5, null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INCIDENT_ALREADY_DECLARED"));

        send(post("/api/v1/drivers/" + driver + "/incidents/" + id + "/cancel"), admin).andExpect(status().isOk()).andExpect(jsonPath("$.cancelledAt").isNotEmpty());
        assertEquals(before, due());
        send(post("/api/v1/drivers/" + driver + "/incidents/" + id + "/cancel"), admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INCIDENT_NOT_LIVE"));
        declare(admin, pastRentDay, "HALF_DAY", "ELECTRICITY", 5, null).andExpect(status().isCreated());
        assertEquals(1, auditRows("DAY_INCIDENT_CANCELLED"));
    }

    @Test
    void onlyAdministratorsDeclareButStaffCanRead() throws Exception {
        declare(manager, pastRentDay, "STOP", "ELECTRICITY", 9, null).andExpect(status().isForbidden());
        declare(viewer, pastRentDay, "STOP", "ELECTRICITY", 9, null).andExpect(status().isForbidden());
        declare(admin, pastRentDay, "STOP", "ELECTRICITY", 9, null).andExpect(status().isCreated());

        send(get("/api/v1/drivers/" + driver + "/incidents"), viewer).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/drivers/" + driver + "/incidents")).andExpect(status().isUnauthorized());
    }

    @Test
    void anotherCountryCannotSeeOrDeclare() throws Exception {
        declare(maliManager, pastRentDay, "STOP", "ELECTRICITY", 9, null).andExpect(status().isForbidden());
        send(get("/api/v1/drivers/" + driver + "/incidents"), maliManager).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void theAccountTableShowsPaidUnpaidAndTheDaysLeftInTheContract() throws Exception {
        declare(admin, pastRentDay, "STOP", "ELECTRICITY", 9, null).andExpect(status().isCreated());

        var rows = send(get("/api/v1/payments/accounts"), viewer).andExpect(status().isOk());
        String json = rows.andReturn().getResponse().getContentAsString();
        int unpaid = JsonPath.read(json, "$.items[0].unpaidDays");
        int elapsed = JsonPath.read(json, "$.items[0].elapsedDays");
        int total = JsonPath.read(json, "$.items[0].totalDays");
        int left = JsonPath.read(json, "$.items[0].daysLeft");

        int detailUnpaid = JsonPath.read(send(get("/api/v1/payments/accounts/" + driver), viewer).andReturn().getResponse().getContentAsString(), "$.unpaid");
        assertEquals(detailUnpaid, unpaid);
        assertEquals(total - elapsed, left);
        org.junit.jupiter.api.Assertions.assertTrue(elapsed >= 8 && elapsed <= 10, "ten days since the start, Sundays are free: " + elapsed);
        org.junit.jupiter.api.Assertions.assertTrue(total > 1000);
    }

    @Test
    void theDriversListCarriesUnpaidDaysAndDaysLeft() throws Exception {
        declare(admin, pastRentDay, "HALF_DAY", "ELECTRICITY", 5, null).andExpect(status().isCreated());

        send(get("/api/v1/drivers"), viewer).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].arrears.unpaidDays").isNumber())
                .andExpect(jsonPath("$.items[0].arrears.daysLeft").value(org.hamcrest.Matchers.greaterThan(1000)));
    }
}
