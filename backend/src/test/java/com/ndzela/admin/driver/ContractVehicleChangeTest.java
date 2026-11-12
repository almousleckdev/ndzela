package com.ndzela.admin.driver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Changing a driver's car keeps the same contract.
class ContractVehicleChangeTest extends AbstractContractTest {
    private org.springframework.test.web.servlet.ResultActions change(String driver, String vehicle, com.ndzela.admin.user.domain.User as) throws Exception {
        return send(put("/api/v1/drivers/" + driver + "/vehicle").contentType("application/json").content("{\"vehicleId\":\"" + vehicle + "\"}"), as);
    }

    @Test
    void theDriverMovesToAnotherCarOnTheSameContract() throws Exception {
        String first = newVehicle(manager);
        String second = newVehicle(manager);
        String driver = id(createDriver(manager, DriverJson.create(first, center)).andExpect(status().isCreated()));
        String number = jdbc.queryForObject("select contract_number from contracts", String.class);
        String startDate = jdbc.queryForObject("select start_date::text from contracts", String.class);
        int rent = rows("select daily_rent::int from contracts");

        change(driver, second, manager).andExpect(status().isOk()).andExpect(jsonPath("$.vehicle.id").value(second));

        assertEquals("AVAILABLE", jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, first));
        assertEquals("ASSIGNED", jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, second));
        assertEquals(1, rows("select count(*) from contracts"));
        assertEquals(number, jdbc.queryForObject("select contract_number from contracts", String.class));
        assertEquals(startDate, jdbc.queryForObject("select start_date::text from contracts", String.class));
        assertEquals(rent, rows("select daily_rent::int from contracts"));
        assertEquals(second, jdbc.queryForObject("select vehicle_id::text from contracts", String.class));
        assertEquals(1, rows("select revision from contracts"));
        assertEquals(1, rows("select count(*) from vehicle_assignments where ended_at is null and vehicle_id = ?::uuid", second));
        assertEquals(1, rows("select count(*) from vehicle_assignments where ended_at is not null and vehicle_id = ?::uuid", first));
        assertEquals(1, auditRows("DRIVER_VEHICLE_CHANGED"));
        assertEquals(1, auditRows("CONTRACT_VEHICLE_CHANGED"));
    }

    @Test
    void theOldCarCanGoToAnotherDriverAfterwards() throws Exception {
        String first = newVehicle(manager);
        String driver = id(createDriver(manager, DriverJson.create(first, center)).andExpect(status().isCreated()));
        change(driver, newVehicle(manager), manager).andExpect(status().isOk());
        String other = id(createDriver(manager, DriverJson.create(first, center)).andExpect(status().isCreated()));
        assertEquals(2, rows("select count(*) from contracts where status = 'ACTIVE'"));
        assertEquals(1, rows("select count(*) from vehicle_assignments where ended_at is null and driver_id = ?::uuid", java.util.UUID.fromString(other)));
    }

    @Test
    void refusesATakenCarTheSameCarMissingRightsAndOtherCountries() throws Exception {
        String first = newVehicle(manager);
        String busy = newVehicle(manager);
        String driver = id(createDriver(manager, DriverJson.create(first, center)).andExpect(status().isCreated()));
        createDriver(manager, DriverJson.create(busy, center)).andExpect(status().isCreated());

        change(driver, busy, manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_NOT_AVAILABLE"));
        change(driver, first, manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_UNCHANGED"));
        change(driver, newVehicle(manager), viewer).andExpect(status().isForbidden());
        change(driver, newVehicle(maliManager), maliManager).andExpect(status().isNotFound());
        change(driver, newVehicle(maliManager), manager).andExpect(status().isNotFound());
        assertEquals(first, jdbc.queryForObject("select vehicle_id::text from contracts where driver_id = ?::uuid", String.class, driver));
        assertEquals(0, auditRows("DRIVER_VEHICLE_CHANGED"));
    }

    @Test
    void aDriverWithoutACarCannotChange() throws Exception {
        String driver = driverWithoutVehicle(manager, center);
        change(driver, newVehicle(manager), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_HAS_NO_VEHICLE"));
    }
}
