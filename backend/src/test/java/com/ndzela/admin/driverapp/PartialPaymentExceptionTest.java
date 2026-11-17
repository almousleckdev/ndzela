package com.ndzela.admin.driverapp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A driver with 3+ late days pays only some of them when staff allowed it
class PartialPaymentExceptionTest extends AbstractDriverPayingTest {
    @Test
    void withTheExceptionADriverWithThreeLateDaysMayPayOne() throws Exception {
        withLateDays(3);
        start("1").andExpect(status().isUnprocessableEntity());

        jdbc.update("update drivers set partial_payment_allowed = true");

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.options[0].days").value(1)).andExpect(jsonPath("$.options[1].days").value(2));
        start("1").andExpect(status().isAccepted());
        assertThat(intentAmount().compareTo(RENT)).isZero();
    }

    @Test
    void theExceptionDoesNotUnlockTheDriversOtherRestrictions() throws Exception {
        withLateDays(3);
        jdbc.update("update drivers set partial_payment_allowed = true");

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.locked").value(true));
    }
}
