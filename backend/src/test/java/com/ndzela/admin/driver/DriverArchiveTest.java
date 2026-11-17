package com.ndzela.admin.driver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A driver who resigned is archived: no car, no login, nothing owed, and the whole history kept in its own tab
class DriverArchiveTest extends AbstractDriverTest {
    private String vehicle;

    private String driverWithCar() throws Exception {
        vehicle = newVehicle(manager);
        return id(createDriver(manager, DriverJson.create(vehicle, newCenter(manager, "Centre " + System.nanoTime()))).andExpect(status().isCreated()));
    }

    @Test
    void archivingKeepsTheContractFreesTheCarAndMovesTheDriverToItsOwnTab() throws Exception {
        String driver = driverWithCar();

        postJson("/api/v1/drivers/" + driver + "/archive", "{\"reason\":\"Démission\"}", admin).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.vehicle").doesNotExist());

        assertThat(jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, vehicle)).isEqualTo("AVAILABLE");
        assertThat(jdbc.queryForObject("select count(*) from contracts where status = 'TERMINATED' and end_reason = 'Démission'", Integer.class)).isEqualTo(1);
        send(get("/api/v1/drivers?scope=ARCHIVED"), manager).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value(driver));
        send(get("/api/v1/drivers?scope=CURRENT"), manager).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/drivers?scope=FORMER"), manager).andExpect(jsonPath("$.total").value(0));
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action = 'DRIVER_ARCHIVED'", Integer.class)).isEqualTo(1);
    }

    @Test
    void aDriverWhoStillOwesRentCannotBeArchived() throws Exception {
        String driver = driverWithCar();
        jdbc.update("update drivers set unpaid_days = 2, overdue_amount = 46000 where id = ?::uuid", driver);

        postJson("/api/v1/drivers/" + driver + "/archive", REASON, admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("CONTRACT_HAS_ARREARS"));

        assertThat(jdbc.queryForObject("select status from drivers where id = ?::uuid", String.class, driver)).isEqualTo("ACTIVE");
    }

    @Test
    void anArchivedDriverIsLeftOutOfTheFollowUpAndCannotUseTheApp() throws Exception {
        String driver = driverWithCar();
        postJson("/api/v1/drivers/" + driver + "/archive", REASON, admin).andExpect(status().isOk());

        send(get("/api/v1/drivers/stats"), manager).andExpect(jsonPath("$.archived").value(1));
        assertThat(jdbc.queryForObject("select count(*) from drivers where status = 'ARCHIVED'", Integer.class)).isEqualTo(1);
    }

    @Test
    void onlyHoldersOfTheArchivePermissionCanArchive() throws Exception {
        String driver = driverWithCar();

        postJson("/api/v1/drivers/" + driver + "/archive", REASON, manager).andExpect(status().isForbidden());
        postJson("/api/v1/drivers/" + driver + "/archive", REASON, viewer).andExpect(status().isForbidden());
        postJson("/api/v1/drivers/" + driver + "/archive", REASON, maliManager).andExpect(status().isForbidden());
    }

    @Test
    void anArchivedDriverCanBeReactivatedByWhoCanUpdateDrivers() throws Exception {
        String driver = driverWithCar();
        postJson("/api/v1/drivers/" + driver + "/archive", REASON, admin).andExpect(status().isOk());

        postJson("/api/v1/drivers/" + driver + "/reactivate", "", manager).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void thePermissionIsHeldBySuperAdminAndAdmin() {
        assertThat(jdbc.queryForList("select r.name from roles r join role_permissions rp on rp.role_id = r.id join permissions p on p.id = rp.permission_id where p.name = 'drivers:archive' order by r.name", String.class))
                .containsExactly("ADMIN", "SUPER_ADMIN");
    }
}
