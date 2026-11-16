package com.ndzela.admin.payment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

// Yesterday's rent shows as owed from 00:00 in the staff screens, while the level, the stored late count and the suspension still wait for 12:00
class ArrearsGraceDisplayTest extends AbstractArrearsTest {
    @Test
    void theFirstRentIsCountedFromMidnightOfItsNextDay() throws Exception {
        arrears.evaluateAll(firstDue.minusSeconds(6 * 3600));

        assertEquals(0, rows("select unpaid_days from drivers"), "the late count that drives the rules still waits for noon");
        assertEquals(1, rows("select grace_days from drivers"));
        assertEquals("NONE", level());
        send(get("/api/v1/payments/accounts"), manager).andExpect(jsonPath("$.items[0].unpaidDays").value(1)).andExpect(jsonPath("$.items[0].overdueAmount").value(23000));
        send(get("/api/v1/drivers/" + driver), manager).andExpect(jsonPath("$.arrears.unpaidDays").value(1)).andExpect(jsonPath("$.arrears.overdueAmount").value(23000));
        send(get("/api/v1/payments/summary"), manager).andExpect(jsonPath("$.totalOverdue").value(23000)).andExpect(jsonPath("$.red").value(0));
    }

    @Test
    void atNoonTheDayBecomesLateWithoutBeingCountedTwice() throws Exception {
        arrears.evaluateAll(firstDue.plusSeconds(3600));

        assertEquals(1, rows("select unpaid_days from drivers"));
        assertEquals(0, rows("select grace_days from drivers"));
        send(get("/api/v1/payments/accounts"), manager).andExpect(jsonPath("$.items[0].unpaidDays").value(1)).andExpect(jsonPath("$.items[0].overdueAmount").value(23000));
        send(get("/api/v1/payments/summary"), manager).andExpect(jsonPath("$.totalOverdue").value(23000));
    }

    @Test
    void theAwaitingFigureNoLongerRepeatsYesterdaysRent() throws Exception {
        arrears.evaluateAll(firstDue.minusSeconds(6 * 3600));

        // Only today's open rent remains awaiting (nothing when today is a Sunday); yesterday's moved to the owed count
        int awaiting = rows("select awaiting_days from drivers") - rows("select grace_days from drivers");
        send(get("/api/v1/payments/accounts"), manager).andExpect(jsonPath("$.items[0].awaitingDays").value(awaiting));
    }
}
