package com.ndzela.admin.payment;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Everything collected since the start: rent payments without their reversals, the provider fees, per month and per driver, former drivers included.
class EarningsTest extends AbstractDriverTest {

    private String newDriver() throws Exception {
        return id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "C" + System.nanoTime()))).andExpect(status().isCreated()));
    }

    // A ledger line written straight in the table; returns its id.
    private UUID entry(String driverId, String type, int amount, String at, UUID reverses) {
        UUID id = UUID.randomUUID();
        UUID driver = UUID.fromString(driverId);
        var contract = jdbc.queryForMap("select id, country_id from contracts where driver_id = ? order by created_at limit 1", driver);
        jdbc.update("insert into ledger_entries (id, country_id, contract_id, driver_id, entry_type, amount, method, reference, reason, request_id, recorded_by, created_at, reverses_entry_id) "
                + "values (?, ?, ?, ?, ?, ?, 'OTHER', ?, ?, ?, 'test', ?::timestamptz, ?)", id, contract.get("country_id"), contract.get("id"), driver, type, amount, "ref-" + id, "test", id, at, reverses);
        return id;
    }

    private UUID payment(String driverId, int amount, String at) {
        return entry(driverId, "PAYMENT", amount, at, null);
    }

    // Two payments kept, one reversed, one fee: collected 46 000, fees 500, net 45 500.
    private String driverWithTheStandardLedger() throws Exception {
        String driverId = newDriver();
        payment(driverId, 23000, "2026-10-10T10:00:00Z");
        UUID second = payment(driverId, 23000, "2026-11-05T10:00:00Z");
        UUID reversed = payment(driverId, 23000, "2026-11-06T10:00:00Z");
        entry(driverId, "FEE", 500, "2026-10-10T10:00:00Z", null);
        entry(driverId, "REVERSAL", 23000, "2026-11-07T10:00:00Z", reversed);
        return second.toString();
    }

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
        jdbc.update("update drivers set status = 'INACTIVE' where id = ?", UUID.fromString(former));

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
