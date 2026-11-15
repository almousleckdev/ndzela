package com.ndzela.admin.payment;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Drivers and ledger lines written straight in the table, for the earnings tests.
abstract class AbstractEarningsTest extends AbstractDriverTest {

    protected String newDriver() throws Exception {
        return id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "C" + System.nanoTime()))).andExpect(status().isCreated()));
    }

    // A ledger line; returns its id.
    protected UUID entry(String driverId, String type, int amount, String at, UUID reverses) {
        UUID id = UUID.randomUUID();
        UUID driver = UUID.fromString(driverId);
        var contract = jdbc.queryForMap("select id, country_id from contracts where driver_id = ? order by created_at limit 1", driver);
        jdbc.update("insert into ledger_entries (id, country_id, contract_id, driver_id, entry_type, amount, method, reference, reason, request_id, recorded_by, created_at, reverses_entry_id) "
                + "values (?, ?, ?, ?, ?, ?, 'OTHER', ?, ?, ?, 'test', ?::timestamptz, ?)", id, contract.get("country_id"), contract.get("id"), driver, type, amount, "ref-" + id, "test", id, at, reverses);
        return id;
    }

    protected UUID payment(String driverId, int amount, String at) {
        return entry(driverId, "PAYMENT", amount, at, null);
    }

    // Two payments kept, one reversed, one fee: collected 46 000, fees 500, net 45 500. Returns the driver id.
    protected String driverWithTheStandardLedger() throws Exception {
        String driverId = newDriver();
        payment(driverId, 23000, "2026-10-10T10:00:00Z");
        payment(driverId, 23000, "2026-11-05T10:00:00Z");
        UUID reversed = payment(driverId, 23000, "2026-11-06T10:00:00Z");
        entry(driverId, "FEE", 500, "2026-10-10T10:00:00Z", null);
        entry(driverId, "REVERSAL", 23000, "2026-11-07T10:00:00Z", reversed);
        return driverId;
    }
}
