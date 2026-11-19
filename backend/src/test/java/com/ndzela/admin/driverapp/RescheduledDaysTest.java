package com.ndzela.admin.driverapp;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Staff push late rent days to the end of the contract (rééchelonner): they stop being owed now and are charged after the contract end
class RescheduledDaysTest extends AbstractDriverPayingTest {
    private ResultActions reschedule(List<String> days, String reason, com.ndzela.admin.user.domain.User as) throws Exception {
        String body = "{\"days\":[" + String.join(",", days.stream().map(d -> "\"" + d + "\"").toList()) + "],\"reason\":\"" + reason + "\"}";
        return send(post("/api/v1/payments/accounts/" + driverId + "/rescheduled-days").contentType(MediaType.APPLICATION_JSON).content(body), as);
    }

    private List<String> lateDays() throws Exception {
        String account = send(get("/api/v1/payments/accounts/" + driverId), admin).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> days = JsonPath.read(account, "$.payableDays[?(@.late==true)].day");
        return days.stream().sorted().toList();
    }

    @Test
    void pushedDaysStopBeingOwedAndLandAfterTheContractEnd() throws Exception {
        withLateDays(4);
        List<String> late = lateDays();

        String response = reschedule(List.of(late.get(0), late.get(1)), "Panne", admin).andExpect(status().isCreated()).andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.unpaidDays").value(2));
        assertThat(jdbc.queryForObject("select unpaid_days from drivers", Integer.class)).isEqualTo(2);
        LocalDate end = jdbc.queryForObject("select end_date from contracts where status = 'ACTIVE'", LocalDate.class);
        for (String newDay : JsonPath.<List<String>>read(response, "$[*].newDay")) {
            LocalDate day = LocalDate.parse(newDay);
            assertThat(day).isAfter(end);
            assertThat(day.getDayOfWeek()).isNotEqualTo(DayOfWeek.SUNDAY);
        }
        assertThat(jdbc.queryForObject("select count(distinct new_day) from rescheduled_rent_days", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action = 'RENT_DAYS_RESCHEDULED'", Integer.class)).isEqualTo(1);
    }

    @Test
    void undoingChargesTheDayAgain() throws Exception {
        withLateDays(2);
        String id = JsonPath.read(reschedule(List.of(lateDays().get(0)), "Erreur", admin).andReturn().getResponse().getContentAsString(), "$[0].id");
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.unpaidDays").value(1));

        send(delete("/api/v1/payments/accounts/" + driverId + "/rescheduled-days/" + id), admin).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.unpaidDays").value(2));
    }

    @Test
    void aPaidDayCannotBePushedAndOnlyHoldersOfThePermissionPush() throws Exception {
        withLateDays(2);
        String day = lateDays().get(0);

        reschedule(List.of(day), " ", admin).andExpect(status().isBadRequest());
        reschedule(List.of(day), "Panne", manager).andExpect(status().isForbidden());
        reschedule(List.of(day), "Panne", maliManager).andExpect(status().isForbidden());
        reschedule(List.of(day), "Panne", staff("ml.admin4@ndzela.test", "ML", "ADMIN")).andExpect(status().isNotFound());

        postJson("/api/v1/payments", "{\"driverId\":\"" + driverId + "\",\"method\":\"AIRTEL_MONEY\",\"reference\":\"R-8\",\"requestId\":\"" + java.util.UUID.randomUUID() + "\",\"days\":[\"" + day + "\"]}", manager)
                .andExpect(status().isCreated());
        reschedule(List.of(day), "Trop tard", admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DAY_NOT_PAYABLE"));
    }
}
