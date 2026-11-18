package com.ndzela.admin.driverapp;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Staff cancel the rent days a driver does not owe (a long suspension); a cancelled day is not charged, in the app too
class CancelledDaysTest extends AbstractDriverPayingTest {
    private ResultActions cancel(List<String> days, String reason, com.ndzela.admin.user.domain.User as) throws Exception {
        String body = "{\"days\":[" + String.join(",", days.stream().map(d -> "\"" + d + "\"").toList()) + "],\"reason\":\"" + reason + "\"}";
        return send(post("/api/v1/payments/accounts/" + driverId + "/cancelled-days").contentType(MediaType.APPLICATION_JSON).content(body), as);
    }

    private List<String> lateDays() throws Exception {
        String account = send(get("/api/v1/payments/accounts/" + driverId), admin).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> days = JsonPath.read(account, "$.payableDays[?(@.late==true)].day");
        return days.stream().sorted().toList();
    }

    @Test
    void cancelledDaysAreNoLongerOwedNorShownToTheDriver() throws Exception {
        withLateDays(3);
        List<String> late = lateDays();

        cancel(List.of(late.get(0), late.get(1)), "Suspendu", admin).andExpect(status().isCreated()).andExpect(jsonPath("$.length()").value(2));

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.unpaidDays").value(1)).andExpect(jsonPath("$.options[0].amount").value(23000));
        assertThat(jdbc.queryForObject("select unpaid_days from drivers", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action = 'RENT_DAYS_CANCELLED'", Integer.class)).isEqualTo(1);
    }

    @Test
    void restoringADayChargesItAgain() throws Exception {
        withLateDays(2);
        String id = JsonPath.read(cancel(List.of(lateDays().get(0)), "Erreur", admin).andReturn().getResponse().getContentAsString(), "$[0].id");
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.unpaidDays").value(1));

        send(delete("/api/v1/payments/accounts/" + driverId + "/cancelled-days/" + id), admin).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.unpaidDays").value(2));
    }

    @Test
    void aPaidDayCannotBeCancelled() throws Exception {
        withLateDays(2);
        String day = lateDays().get(0);
        postJson("/api/v1/payments", "{\"driverId\":\"" + driverId + "\",\"method\":\"AIRTEL_MONEY\",\"reference\":\"R-9\",\"requestId\":\"" + java.util.UUID.randomUUID() + "\",\"days\":[\"" + day + "\"]}", manager)
                .andExpect(status().isCreated());

        cancel(List.of(day), "Trop tard", admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DAY_NOT_PAYABLE"));
    }

    @Test
    void aReasonIsRequiredAndOnlyTheHoldersOfThePermissionCancel() throws Exception {
        withLateDays(2);
        String day = lateDays().get(0);

        cancel(List.of(day), " ", admin).andExpect(status().isBadRequest());
        cancel(List.of(day), "Suspendu", manager).andExpect(status().isForbidden());
        cancel(List.of(day), "Suspendu", maliManager).andExpect(status().isForbidden());
        cancel(List.of(day), "Suspendu", staff("ml.admin3@ndzela.test", "ML", "ADMIN")).andExpect(status().isNotFound());
    }

    @Test
    void cancellingEveryLateDayLetsTheContractClose() throws Exception {
        withLateDays(2);
        cancel(lateDays(), "Démission", admin).andExpect(status().isCreated());

        postJson("/api/v1/drivers/" + driverId + "/archive", "{\"reason\":\"Démission\"}", admin).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));
    }
}
