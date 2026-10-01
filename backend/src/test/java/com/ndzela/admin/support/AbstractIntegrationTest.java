package com.ndzela.admin.support;

import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.security.SecurityUser;
import com.ndzela.admin.security.jwt.JwtService;
import com.ndzela.admin.user.domain.Role;
import com.ndzela.admin.user.domain.RoleRepository;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.HashSet;
import java.util.Set;

/** Integration test base: real PostgreSQL and Redis, full context, MockMvc; countries come from the migrations. */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {
    public static final String PASSWORD = "Str0ng!Pass";

    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/test-migration");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("app.arrears.job-enabled", () -> "false");
        registry.add("app.jwt.secret", () -> "dGVzdC1vbmx5LWp3dC1zZWNyZXQtdGVzdC1vbmx5LWp3dC1zZWNyZXQ=");
        registry.add("app.mfa.encryption-key", () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        registry.add("app.mail.provider", () -> "log");
        registry.add("spring.mail.username", () -> "test@ndzela.test");
        registry.add("spring.mail.password", () -> "unused");
        registry.add("app.storage.upload-dir", () -> System.getProperty("java.io.tmpdir") + "/ndzela-it-uploads");
        registry.add("app.bootstrap.initial-admin-password", () -> "");
    }

    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected MockMvc mvc;
    @Autowired protected UserRepository users;
    @Autowired protected RoleRepository roles;
    @Autowired protected CountryRepository countries;
    @Autowired protected JwtService jwt;
    @Autowired protected PasswordEncoder encoder;

    @BeforeEach
    void resetData() {
        jdbc.execute("TRUNCATE TABLE tenant_test_items, charging_entries, stations, arrears_events, ledger_entries, contracts, contract_counters, driver_leaves, vehicle_assignments, drivers, training_centers, vehicles, notifications, audit_logs, user_roles, users CASCADE");
        jdbc.update("UPDATE countries SET active = TRUE, late_penalty = 0, charging_critical_percent = 20");
    }

    protected Country country(String code) {
        return countries.findByCode(code).orElseThrow();
    }

    protected User staff(String email, String countryCode, String... roleNames) {
        Set<Role> assigned = new HashSet<>();
        for (String roleName : roleNames) {
            assigned.add(roles.findByName(roleName).orElseThrow());
        }
        return users.save(User.builder()
                .email(email)
                .password(encoder.encode(PASSWORD))
                .status(UserStatus.ACTIVE)
                .mfaEnabled(true)
                .country(countryCode == null ? null : country(countryCode))
                .roles(assigned)
                .build());
    }

    protected String bearer(User user) {
        return "Bearer " + jwt.generateAccessToken(new SecurityUser(user));
    }
}
