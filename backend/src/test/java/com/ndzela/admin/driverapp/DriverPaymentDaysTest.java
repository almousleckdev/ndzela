package com.ndzela.admin.driverapp;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Paying one, two or all of the unpaid days
class DriverPaymentDaysTest extends AbstractDriverPayingTest {
    @Test
    void twoUnpaidDaysAllowPayingTheOldestOneAlone() throws Exception {
        withUnpaidDays(2);

        start("1").andExpect(status().isAccepted());

        assertEquals(0, RENT.compareTo(intentAmount()));
        assertEquals(1, jdbc.queryForObject("select days_requested from payment_intents", Integer.class));
    }

    @Test
    void twoUnpaidDaysAllowPayingBoth() throws Exception {
        withUnpaidDays(2);

        start("2").andExpect(status().isAccepted());

        assertEquals(0, RENT.multiply(BigDecimal.TWO).compareTo(intentAmount()));
    }

    @Test
    void threeUnpaidDaysMustAllBePaidAtOnce() throws Exception {
        withUnpaidDays(3);

        start("1").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_PAYMENT_DAYS"));
        start("2").andExpect(status().isUnprocessableEntity());
        start("3").andExpect(status().isAccepted());

        assertEquals(0, RENT.multiply(BigDecimal.valueOf(3)).compareTo(intentAmount()));
    }

    @Test
    void omittingTheDaysPaysEverything() throws Exception {
        withUnpaidDays(2);

        start(null).andExpect(status().isAccepted());

        assertEquals(0, RENT.multiply(BigDecimal.TWO).compareTo(intentAmount()));
    }

    @Test
    void zeroNegativeOrTooManyDaysAreRefused() throws Exception {
        withUnpaidDays(2);

        for (String days : List.of("0", "-1", "3")) {
            start(days).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_PAYMENT_DAYS"));
        }
        assertEquals(0, jdbc.queryForObject("select count(*) from payment_intents", Integer.class));
    }

    @Test
    void theQuoteListsTheOptionsAndPricesTheChosenDays() throws Exception {
        withUnpaidDays(2);

        asDriver("/api/v1/driver/payments/quote?operator=AIRTEL_MONEY&days=1", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(1)).andExpect(jsonPath("$.amount").value(23000))
                .andExpect(jsonPath("$.options.length()").value(2)).andExpect(jsonPath("$.options[1].days").value(2)).andExpect(jsonPath("$.options[1].amount").value(46000));
    }

    @Test
    void whenPvitConfirmsLessOnlyTheDaysItCoversAreSettled() throws Exception {
        withUnpaidDays(2);
        start("2").andExpect(status().isAccepted());

        pvitConfirms(RENT);

        assertEquals(1, jdbc.queryForObject("select count(*) from ledger_entry_days", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from audit_logs where action = 'PAYMENT_UNDERPAID'", Integer.class));
    }

    @Test
    void aConfirmedPaymentOfOneDayLeavesTheOtherDayUnpaid() throws Exception {
        withUnpaidDays(2);
        start("1").andExpect(status().isAccepted());

        pvitConfirms(RENT);

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.unpaidDays").value(1));
        assertTrue(jdbc.queryForObject("select count(*) from ledger_entry_days", Integer.class) == 1);
    }

    @Test
    void aSecondStartWhileOneWaitsCreatesNoSecondAttempt() throws Exception {
        withUnpaidDays(2);
        start("1").andExpect(status().isAccepted());

        start("1").andExpect(status().isUnprocessableEntity());

        assertEquals(1, jdbc.queryForObject("select count(*) from payment_intents", Integer.class));
    }

    @Test
    void aDoubleCallbackRecordsOnePaymentAndOneSnapshot() throws Exception {
        withUnpaidDays(2);
        start("1").andExpect(status().isAccepted());

        pvitConfirms(RENT);
        pvitConfirms(RENT);

        assertEquals(1, jdbc.queryForObject("select count(*) from ledger_entries where entry_type = 'PAYMENT'", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from ledger_entry_days", Integer.class));
    }

    @Test
    void theAccountSaysWhenThePaymentWeekClosesAndWhetherItIsSettled() throws Exception {
        withUnpaidDays(2);

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.weekSettled").value(false)).andExpect(jsonPath("$.weekClosesAt").exists());

        jdbc.update("update contracts set start_date = current_date + 5");
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.weekSettled").value(true));
    }

    @Test
    void theAccountListsTheNumbersOfDaysTheDriverMayPay() throws Exception {
        withUnpaidDays(2);
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.options.length()").value(2)).andExpect(jsonPath("$.options[0].days").value(1))
                .andExpect(jsonPath("$.options[1].amount").value(46000));

        withUnpaidDays(3);
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.options.length()").value(1)).andExpect(jsonPath("$.options[0].days").value(3));
    }

    @Test
    void aConfirmedPaymentShowsTheDaysItSettledToTheDriver() throws Exception {
        withUnpaidDays(2);
        String id = JsonPath.read(start("1").andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString(), "$.id");
        pvitConfirms(RENT);

        asDriver("/api/v1/driver/payments/" + id, token).andExpect(jsonPath("$.days.length()").value(1)).andExpect(jsonPath("$.days[0].kind").value("RENT"))
                .andExpect(jsonPath("$.days[0].onTime").value(false));
        asDriver("/api/v1/driver/payments", token).andExpect(jsonPath("$[0].days.length()").value(1));
    }
}
