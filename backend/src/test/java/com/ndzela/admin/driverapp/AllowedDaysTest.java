package com.ndzela.admin.driverapp;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Staff pick which late days a driver with 3+ late days may pay alone, and can take them back
class AllowedDaysTest extends AbstractDriverPayingTest {
    private ResultActions allow(List<String> days, com.ndzela.admin.user.domain.User as) throws Exception {
        String body = "{\"days\":[" + String.join(",", days.stream().map(d -> "\"" + d + "\"").toList()) + "]}";
        return send(put("/api/v1/payments/accounts/" + driverId + "/allowed-days").contentType(MediaType.APPLICATION_JSON).content(body), as);
    }

    private List<String> lateDays() throws Exception {
        String account = send(get("/api/v1/payments/accounts/" + driverId), admin).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> days = JsonPath.read(account, "$.payableDays[?(@.late==true)].day");
        return days.stream().sorted().toList();
    }

    @Test
    void theDriverMayPayOnlyTheDaysStaffAllowed() throws Exception {
        withLateDays(4);
        List<String> late = lateDays();

        allow(List.of(late.get(1), late.get(3)), admin).andExpect(status().isOk());

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.options.length()").value(2)).andExpect(jsonPath("$.options[0].days").value(1))
                .andExpect(jsonPath("$.options[0].amount").value(23000)).andExpect(jsonPath("$.options[1].amount").value(46000));
        start("1").andExpect(status().isAccepted());
        pvitConfirms(RENT);

        assertThat(jdbc.queryForList("select business_day::text from ledger_entry_days order by business_day", String.class)).containsExactly(late.get(1));
    }

    @Test
    void revokingTheDaysBringsBackPayEverything() throws Exception {
        withLateDays(3);
        List<String> late = lateDays();
        allow(List.of(late.get(0)), admin).andExpect(status().isOk());
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.options.length()").value(1)).andExpect(jsonPath("$.options[0].amount").value(23000));

        allow(List.of(), admin).andExpect(status().isOk());

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.options.length()").value(1)).andExpect(jsonPath("$.options[0].amount").value(69000));
        start("1").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aDayStaffPaidDropsOutOfTheAllowedOptions() throws Exception {
        withLateDays(4);
        List<String> late = lateDays();
        allow(List.of(late.get(0), late.get(1)), admin).andExpect(status().isOk());

        postJson("/api/v1/payments", "{\"driverId\":\"" + driverId + "\",\"method\":\"AIRTEL_MONEY\",\"reference\":\"R-1\",\"requestId\":\"" + java.util.UUID.randomUUID() + "\",\"days\":[\"" + late.get(0) + "\"]}", manager)
                .andExpect(status().isCreated());

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.options.length()").value(1));
    }

    @Test
    void onlyLateUnpaidDaysCanBeAllowed() throws Exception {
        withLateDays(3);

        allow(List.of(java.time.LocalDate.now().plusDays(3).toString()), admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DAY_NOT_PAYABLE"));
    }

    @Test
    void theAllowanceIsAuditedAndNeedsThePermission() throws Exception {
        withLateDays(3);
        List<String> late = lateDays();

        allow(List.of(late.get(0)), manager).andExpect(status().isForbidden());
        allow(List.of(late.get(0)), admin).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action = 'PAYMENT_DAYS_ALLOWED'", Integer.class)).isEqualTo(1);
    }

    @Test
    void anotherCountryCannotAllowDays() throws Exception {
        withLateDays(3);

        allow(List.of(lateDays().get(0)), maliManager).andExpect(status().isForbidden());
        allow(List.of(lateDays().get(0)), staff("ml.admin2@ndzela.test", "ML", "ADMIN")).andExpect(status().isNotFound());
    }

    @Test
    void withFewerThanThreeLateDaysTheAllowanceChangesNothing() throws Exception {
        withLateDays(2);
        allow(List.of(lateDays().get(1)), admin).andExpect(status().isOk());

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.options[0].amount").value(23000)).andExpect(jsonPath("$.options[1].amount").value(46000));
    }
}
