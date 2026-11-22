package com.ndzela.admin.freecharging;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.freecharging.application.FreeChargingAgreementService;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.function.Supplier;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Shared drivers and country contexts for the free-charging tests.
abstract class AbstractFreeChargingTest extends AbstractDriverTest {
    @Autowired protected FreeChargingAgreementService agreements;

    protected UUID linkedDriver;
    protected UUID unlinkedDriver;
    protected UUID maliDriver;
    protected TenantContext gabon;
    protected TenantContext mali;

    @BeforeEach
    void drivers() throws Exception {
        gabon = TenantContext.country(country("GA").getId());
        mali = TenantContext.country(country("ML").getId());
        linkedDriver = newDriver(manager, "GA-CENTER");
        unlinkedDriver = newDriver(manager, "GA-CENTER-2");
        maliDriver = newDriver(maliManager, "ML-CENTER");
        link(linkedDriver);
        link(maliDriver);
    }

    @AfterEach
    void clearTenant() {
        TenantHolder.clear();
    }

    protected <T> T inGabon(Supplier<T> action) {
        return TenantHolder.callAs(gabon, action);
    }

    protected <T> T inMali(Supplier<T> action) {
        return TenantHolder.callAs(mali, action);
    }

    private UUID newDriver(com.ndzela.admin.user.domain.User as, String center) throws Exception {
        String driver = id(createDriver(as, DriverJson.create(newVehicle(as), newCenter(as, center))).andExpect(status().isCreated()));
        return UUID.fromString(driver);
    }

    private void link(UUID driver) {
        jdbc.update("insert into id_tags (id, country_id, id_tag, customer_id, driver_id, status, matched_on, decided_by, zella_notified) "
                + "select gen_random_uuid(), country_id, 'TAG-' || id, 'cust-' || id, id, 'LINKED', 'email+phone', 'system', true from drivers where id = ?", driver);
    }
}
