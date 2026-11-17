package com.ndzela.admin.driver;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Only holders of drivers:partial_payment (super admin, admin) switch the exception on
class PartialPaymentPermissionTest extends AbstractDriverTest {
    private String driverWithCar() throws Exception {
        return id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "Centre " + System.nanoTime()))).andExpect(status().isCreated()));
    }

    private org.springframework.test.web.servlet.ResultActions allow(String driver, boolean allowed, com.ndzela.admin.user.domain.User as) throws Exception {
        return send(put("/api/v1/drivers/" + driver + "/partial-payment").contentType("application/json").content("{\"allowed\":" + allowed + "}"), as);
    }

    @Test
    void anAdminSwitchesTheExceptionOnAndOffAndItIsAudited() throws Exception {
        String driver = driverWithCar();

        allow(driver, true, admin).andExpect(status().isOk()).andExpect(jsonPath("$.partialPaymentAllowed").value(true));
        allow(driver, false, root).andExpect(status().isOk()).andExpect(jsonPath("$.partialPaymentAllowed").value(false));

        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action = 'DRIVER_PARTIAL_PAYMENT_CHANGED'", Integer.class)).isEqualTo(2);
    }

    @Test
    void aManagerAndAViewerCannot() throws Exception {
        String driver = driverWithCar();

        allow(driver, true, manager).andExpect(status().isForbidden());
        allow(driver, true, viewer).andExpect(status().isForbidden());
    }

    @Test
    void anotherCountryCannotReachTheDriver() throws Exception {
        String driver = driverWithCar();
        var maliAdmin = staff("ml.admin@ndzela.test", "ML", "ADMIN");

        allow(driver, true, maliAdmin).andExpect(status().isNotFound());
    }

    @Test
    void thePermissionIsHeldBySuperAdminAndAdmin() {
        List<String> holders = jdbc.queryForList("select r.name from roles r join role_permissions rp on rp.role_id = r.id join permissions p on p.id = rp.permission_id where p.name = 'drivers:partial_payment' order by r.name", String.class);

        assertThat(holders).containsExactly("ADMIN", "SUPER_ADMIN");
    }
}
