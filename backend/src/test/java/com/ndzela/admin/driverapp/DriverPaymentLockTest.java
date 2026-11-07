package com.ndzela.admin.driverapp;

import com.ndzela.admin.infrastructure.pvit.PvitStatus;
import com.ndzela.admin.payment.application.ArrearsService;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The lock at three unpaid days and the suspended state
class DriverPaymentLockTest extends AbstractPvitFlowTest {
    @Autowired ArrearsService arrears;

    @Test
    void aDriverWithThreeUnpaidDaysIsLockedToThePaymentScreenUntilEverythingIsPaid() throws Exception {
        arrears.evaluateAll(Instant.now());
        assertEquals(true, jdbc.queryForObject("select unpaid_days >= 3 from drivers", Boolean.class));
        jdbc.update("update drivers set status = 'ACTIVE'");

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.locked").value(true));
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isForbidden());
        asDriver("/api/v1/driver/contract", token).andExpect(status().isForbidden());
        asDriver("/api/v1/driver/me", token).andExpect(status().isOk());
        asDriver("/api/v1/driver/payments/operators", token).andExpect(status().isOk());
        asDriver("/api/v1/driver/payments", token).andExpect(status().isOk());

        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), null);
        callback(reference(), "PAY1", 200).andExpect(status().isOk());

        assertEquals(0, rows("select unpaid_days from drivers"));
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.locked").value(false)).andExpect(jsonPath("$.totalDue").value(0));
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isOk());
        asDriver("/api/v1/driver/contract", token).andExpect(status().isOk());
    }

    @Test
    void aSuspendedDriverStaysRestrictedAfterPayingEverythingUntilAnAdministratorLiftsIt() throws Exception {
        arrears.evaluateAll(Instant.now());
        assertEquals("SUSPENDED", jdbc.queryForObject("select status from drivers", String.class));
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.suspended").value(true)).andExpect(jsonPath("$.locked").value(true));

        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), null);
        callback(reference(), "PAY1", 200).andExpect(status().isOk());

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.locked").value(false)).andExpect(jsonPath("$.suspended").value(true));
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isForbidden());

        send(post("/api/v1/drivers/" + driverId + "/reactivate"), admin).andExpect(status().isOk());
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.suspended").value(false));
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isOk());
    }

    @Test
    void theLockStartsExactlyAtTheConfiguredNumberOfUnpaidDays() throws Exception {
        jdbc.update("update drivers set unpaid_days = 2");
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isOk());

        jdbc.update("update drivers set unpaid_days = 3");
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isForbidden());
    }

    @Test
    void aDriverWhoOwesNothingIsNeverLocked() throws Exception {
        jdbc.update("update contracts set start_date = current_date + 5");

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.locked").value(false)).andExpect(jsonPath("$.unpaidDays").value(0));
    }

    @Test
    void fromThreeUnpaidDaysAPaymentCoversAllTheArrearsAndNeverJustOneOrTwoDays() throws Exception {
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.unpaidDays").value(greaterThan(2)));
        mvc.perform(post("/api/v1/driver/payments").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"AIRTEL_MONEY\",\"phoneNumber\":\"074111111\",\"amount\":23000,\"days\":1}")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_DAYS"));
        startOk();

        java.util.List<Object> amounts = JsonPath.read(asDriver("/api/v1/driver/account", token).andReturn().getResponse().getContentAsString(), "$.options[*].amount");
        assertEquals(0, amount().compareTo(new BigDecimal(amounts.get(amounts.size() - 1).toString())));
    }
}
