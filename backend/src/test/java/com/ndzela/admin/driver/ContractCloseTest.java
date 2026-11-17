package com.ndzela.admin.driver;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A contract closes only when no late rent is owed; the permission is assignable
class ContractCloseTest extends AbstractDriverTest {
    private String driver;

    private String driverWithCar() throws Exception {
        return id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "Centre " + System.nanoTime()))).andExpect(status().isCreated()));
    }

    @Test
    void aDriverWithLateRentCannotBeClosed() throws Exception {
        driver = driverWithCar();
        jdbc.update("update drivers set unpaid_days = 2, overdue_amount = 46000 where id = ?::uuid", driver);

        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("CONTRACT_HAS_ARREARS"));

        assertThat(jdbc.queryForObject("select count(*) from contracts where status = 'ACTIVE'", Integer.class)).isEqualTo(1);
    }

    @Test
    void onceNothingIsOwedTheContractCloses() throws Exception {
        driver = driverWithCar();
        jdbc.update("update drivers set unpaid_days = 0, overdue_amount = 0 where id = ?::uuid", driver);

        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select count(*) from contracts where status = 'TERMINATED'", Integer.class)).isEqualTo(1);
    }

    @Test
    void closingNeedsTheTerminatePermission() throws Exception {
        driver = driverWithCar();
        String grant = "role_id = (select id from roles where name = 'GESTIONNAIRE') and permission_id = (select id from permissions where name = 'contracts:terminate')";
        jdbc.update("delete from role_permissions where " + grant);
        try {
            postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager).andExpect(status().isForbidden());
        } finally {
            jdbc.update("insert into role_permissions (role_id, permission_id) select r.id, p.id from roles r, permissions p where r.name = 'GESTIONNAIRE' and p.name = 'contracts:terminate'");
        }
    }

    @Test
    void theTerminatePermissionIsHeldBySuperAdminAndGestionnaire() {
        List<String> holders = jdbc.queryForList("select r.name from roles r join role_permissions rp on rp.role_id = r.id join permissions p on p.id = rp.permission_id where p.name = 'contracts:terminate' order by r.name", String.class);

        assertThat(holders).containsExactly("GESTIONNAIRE", "SUPER_ADMIN");
    }
}
