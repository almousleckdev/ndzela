package com.ndzela.admin.driver;

import com.ndzela.admin.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Who holds the two permissions added for the driver credentials help and the earnings page.
class CredentialsPermissionsTest extends AbstractIntegrationTest {

    private List<String> holders(String permission) {
        return jdbc.queryForList("select r.name from roles r join role_permissions rp on rp.role_id = r.id join permissions p on p.id = rp.permission_id where p.name = ? order by r.name",
                String.class, permission);
    }

    @Test
    void onlySuperAdminAndAdminHoldTheDriverCredentialsPermission() {
        assertThat(holders("drivers:credentials")).containsExactly("ADMIN", "SUPER_ADMIN");
    }

    @Test
    void superAdminAdminAndComptableHoldTheEarningsPermission() {
        assertThat(holders("earnings:read")).containsExactly("ADMIN", "COMPTABLE", "SUPER_ADMIN");
    }
}
