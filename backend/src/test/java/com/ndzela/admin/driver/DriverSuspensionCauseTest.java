package com.ndzela.admin.driver;

import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Why a driver is suspended decides whether paying lifts it: only an arrears suspension is lifted automatically.
class DriverSuspensionCauseTest extends AbstractDriverTest {
    private static final String ARREARS_REASON = "Impayés : trois jours de loyer non réglés ou plus (suspension automatique)";

    @Autowired
    DriverArrearsService arrears;

    private String driver;

    @BeforeEach
    void setup() throws Exception {
        driver = id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated()));
    }

    private String column(String name) {
        return jdbc.queryForObject("select " + name + " from drivers where id = ?::uuid", String.class, driver);
    }

    private <T> T inCountry(Supplier<T> action) {
        UUID country = jdbc.queryForObject("select country_id from drivers where id = ?::uuid", UUID.class, driver);
        return TenantHolder.callAs(TenantContext.country(country), action);
    }

    private boolean suspendForArrears() {
        return inCountry(() -> arrears.suspendForArrears(UUID.fromString(driver), ARREARS_REASON));
    }

    private boolean restoreFromArrears() {
        return inCountry(() -> arrears.restoreFromArrears(UUID.fromString(driver)));
    }

    private boolean nullColumn(String name) {
        return jdbc.queryForObject("select " + name + " is null from drivers where id = ?::uuid", Boolean.class, driver);
    }

    @Test
    void aManualSuspensionHasCauseManual() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/suspend", REASON, admin).andExpect(status().isOk());

        assertEquals("MANUAL", column("suspension_cause"));
    }

    @Test
    void anArrearsSuspensionHasCauseArrears() {
        assertTrue(suspendForArrears());

        assertEquals("SUSPENDED", column("status"));
        assertEquals("ARREARS", column("suspension_cause"));
    }

    @Test
    void restoringFromArrearsReactivatesAndMarksTheAutomaticRestore() {
        suspendForArrears();

        assertTrue(restoreFromArrears());

        assertEquals("ACTIVE", column("status"));
        assertTrue(nullColumn("suspension_cause"));
        assertFalse(nullColumn("auto_restored_at"));
        assertEquals(1, auditRows("DRIVER_REACTIVATED"));
    }

    @Test
    void restoringFromArrearsLeavesAManualSuspensionAlone() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/suspend", REASON, admin).andExpect(status().isOk());

        assertFalse(restoreFromArrears());

        assertEquals("SUSPENDED", column("status"));
        assertEquals("MANUAL", column("suspension_cause"));
    }

    @Test
    void anyManualStatusChangeClearsTheAutomaticRestoreMarker() throws Exception {
        suspendForArrears();
        restoreFromArrears();
        assertNotNull(column("auto_restored_at"));

        postJson("/api/v1/drivers/" + driver + "/suspend", REASON, admin).andExpect(status().isOk());

        assertNull(column("auto_restored_at"));
    }
}
