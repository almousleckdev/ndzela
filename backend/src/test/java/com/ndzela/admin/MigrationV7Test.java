package com.ndzela.admin;

import org.flywaydb.core.Flyway;
import com.ndzela.admin.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;

// Runs the real migrations on a scratch database in two steps to prove existing users move to Gabon.
class MigrationV7Test extends AbstractIntegrationTest {
    private Flyway flywayUpTo(String version, DriverManagerDataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target(version).load();
    }

    @Test
    void existingUsersBecomeGabonAndSuperAdminsStayGlobal() {
        jdbc.execute("DROP DATABASE IF EXISTS migration_v7");
        jdbc.execute("CREATE DATABASE migration_v7");
        String url = POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/migration_v7");
        var dataSource = new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());
        flywayUpTo("6", dataSource).migrate();

        var db = new JdbcTemplate(dataSource);
        db.update("INSERT INTO users (id, email, password, status) VALUES (gen_random_uuid(), 'staff@ndzela.test', 'x', 'ACTIVE')");
        db.update("INSERT INTO users (id, email, password, status) VALUES (gen_random_uuid(), 'root@ndzela.test', 'x', 'ACTIVE')");
        db.update("INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'staff@ndzela.test' AND r.name = 'ADMIN'");
        db.update("INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'root@ndzela.test' AND r.name = 'SUPER_ADMIN'");

        flywayUpTo("7", dataSource).migrate();

        assertEquals("GA", db.queryForObject(
                "SELECT c.code FROM users u JOIN countries c ON c.id = u.country_id WHERE u.email = 'staff@ndzela.test'", String.class));
        assertNull(db.queryForObject("SELECT country_id FROM users WHERE email = 'root@ndzela.test'", Object.class));
        assertEquals("Gabon", db.queryForObject("SELECT name FROM countries WHERE code = 'GA'", String.class));
        assertEquals("XAF", db.queryForObject("SELECT currency_code FROM countries WHERE code = 'GA'", String.class));
        assertEquals("Africa/Libreville", db.queryForObject("SELECT timezone FROM countries WHERE code = 'GA'", String.class));
        assertEquals(2, db.queryForObject(
                "SELECT count(*) FROM role_permissions rp JOIN permissions p ON p.id = rp.permission_id JOIN roles r ON r.id = rp.role_id "
                        + "WHERE p.name = 'countries:read' AND r.name IN ('ADMIN', 'SUPER_ADMIN')", Integer.class));
    }
}
