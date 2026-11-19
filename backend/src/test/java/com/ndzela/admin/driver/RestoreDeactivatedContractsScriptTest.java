package com.ndzela.admin.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The one-off script that gives back the car and contract that "Mettre inactif" used to end.
class RestoreDeactivatedContractsScriptTest extends AbstractDriverTest {
    private String center;

    @BeforeEach
    void setup() throws Exception {
        center = newCenter(manager, "NDZELA");
    }

    // What the old deactivation left behind: contract ended, car freed, driver inactive, audit line.
    private String deactivatedTheOldWay(String vehicle, String status) throws Exception {
        String driver = id(createDriver(manager, DriverJson.create(vehicle, center)).andExpect(status().isCreated()));
        jdbc.update("update contracts set status = 'TERMINATED', ended_at = now(), end_reason = 'Accident' where driver_id = ?::uuid", driver);
        jdbc.update("update vehicle_assignments set ended_at = now() where driver_id = ?::uuid", driver);
        jdbc.update("update vehicles set status = 'AVAILABLE' where id = ?::uuid", vehicle);
        jdbc.update("update drivers set status = ? where id = ?::uuid", status, driver);
        jdbc.update("insert into audit_logs (id, actor, action, target, result, target_type, target_id, created_at) values (gen_random_uuid(), 'staff', 'DRIVER_DEACTIVATED', 'x', 'SUCCESS', 'DRIVER', ?, now())", driver);
        return driver;
    }

    private void runScript() throws Exception {
        String script = Files.readString(Path.of("scripts/restore-deactivated-contracts.sql")).replace("\nROLLBACK;", "\nCOMMIT;");
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            try {
                ScriptUtils.executeSqlScript(connection, new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8)));
            } finally {
                connection.createStatement().execute("ROLLBACK");
            }
        }
    }

    @Test
    void anInactiveDriverGetsTheSameContractAndCarBackWithAnOpenPause() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = deactivatedTheOldWay(vehicle, "INACTIVE");

        runScript();

        assertEquals(1, rows("select count(*) from contracts where driver_id = ?::uuid and status = 'ACTIVE' and ended_at is null", driver));
        assertEquals(1, rows("select count(*) from vehicle_assignments where driver_id = ?::uuid and vehicle_id = ?::uuid and ended_at is null", driver, vehicle));
        assertEquals("ASSIGNED", jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, vehicle));
        assertEquals(1, rows("select count(*) from driver_pauses where driver_id = ?::uuid and ended_on is null", driver));
        assertEquals(1, auditRows("CONTRACT_RESTORED"));
    }

    @Test
    void aCarGivenToSomeoneElseIsLeftForAManualDecision() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = deactivatedTheOldWay(vehicle, "INACTIVE");
        driverWith(vehicle);

        runScript();

        assertEquals(0, rows("select count(*) from contracts where driver_id = ?::uuid and status = 'ACTIVE'", driver));
        assertEquals(0, auditRows("CONTRACT_RESTORED"));
    }

    private String driverWith(String vehicle) throws Exception {
        return id(createDriver(manager, DriverJson.create(vehicle, center)).andExpect(status().isCreated()));
    }
}
