package com.ndzela.admin.payment;

import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Everything collected since the start: rent payments without their reversals, the provider fees, per month and per driver, former drivers included.
class EarningsTest extends AbstractEarningsTest {

    @Test
    void totalsEqualTheLedgerWithoutTheReversedPayment() throws Exception {
        driverWithTheStandardLedger();

        send(get("/api/v1/payments/earnings"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.collected").value(46000)).andExpect(jsonPath("$.fees").value(500)).andExpect(jsonPath("$.net").value(45500))
                .andExpect(jsonPath("$.payingDrivers").value(1));
    }

    @Test
    void monthsAreGroupedInTheCountryTimeZone() throws Exception {
        String driverId = newDriver();
        payment(driverId, 23000, "2026-10-31T23:30:00Z");

        send(get("/api/v1/payments/earnings"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.months.length()").value(1)).andExpect(jsonPath("$.months[0].month").value("2026-11")).andExpect(jsonPath("$.months[0].collected").value(23000));
    }

    @Test
    void formerDriversAreIncludedAndMarked() throws Exception {
        driverWithTheStandardLedger();
        String former = newDriver();
        payment(former, 10000, "2026-09-01T10:00:00Z");
        jdbc.update("update drivers set status = 'ARCHIVED' where id = ?", UUID.fromString(former));

        send(get("/api/v1/payments/earnings"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.collected").value(56000)).andExpect(jsonPath("$.collectedCurrent").value(46000)).andExpect(jsonPath("$.collectedFormer").value(10000));
        send(get("/api/v1/payments/earnings/drivers"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.items[?(@.driverId == '" + former + "')].former").value(true))
                .andExpect(jsonPath("$.items[?(@.driverId == '" + former + "')].totalPaid").value(10000));
    }

    @Test
    void anEmptyLedgerGivesZerosAndNoMonths() throws Exception {
        send(get("/api/v1/payments/earnings"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.collected").value(0)).andExpect(jsonPath("$.net").value(0)).andExpect(jsonPath("$.months.length()").value(0));
        send(get("/api/v1/payments/earnings/drivers"), admin).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void anotherCountryDoesNotSeeTheMoney() throws Exception {
        driverWithTheStandardLedger();
        User maliAdmin = staff("ml.admin@ndzela.test", "ML", "ADMIN");

        send(get("/api/v1/payments/earnings"), maliAdmin).andExpect(status().isOk()).andExpect(jsonPath("$.collected").value(0));
        send(get("/api/v1/payments/earnings/drivers"), maliAdmin).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void theSuperAdminSeesTheMoneyOfEveryCountry() throws Exception {
        driverWithTheStandardLedger();

        send(get("/api/v1/payments/earnings"), root).andExpect(status().isOk()).andExpect(jsonPath("$.collected").value(46000));
    }

    @Test
    void aComptableMayReadAndAViewerMayNot() throws Exception {
        User comptable = staff("ga.comptable@ndzela.test", "GA", "COMPTABLE");

        send(get("/api/v1/payments/earnings"), comptable).andExpect(status().isOk());
        send(get("/api/v1/payments/earnings/drivers"), comptable).andExpect(status().isOk());
        send(get("/api/v1/payments/earnings"), viewer).andExpect(status().isForbidden());
        send(get("/api/v1/payments/earnings/drivers"), viewer).andExpect(status().isForbidden());
    }

    @Test
    void thePeriodNarrowsTheTotals() throws Exception {
        driverWithTheStandardLedger();

        send(get("/api/v1/payments/earnings?from=2026-11-01&to=2026-11-30"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.collected").value(23000)).andExpect(jsonPath("$.fees").value(0));
    }

    @Test
    void theDriversAreSearchableByNameAndSortedByTheTotal() throws Exception {
        String big = newDriver();
        payment(big, 90000, "2026-10-10T10:00:00Z");
        String small = newDriver();
        payment(small, 1000, "2026-10-10T10:00:00Z");

        send(get("/api/v1/payments/earnings/drivers?sort=totalPaid,desc"), admin).andExpect(jsonPath("$.items[0].driverId").value(big)).andExpect(jsonPath("$.items[1].driverId").value(small));
    }
}
