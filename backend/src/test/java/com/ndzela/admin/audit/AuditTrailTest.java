package com.ndzela.admin.audit;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Every administrative action leaves one audit row, and the table can never be changed.
class AuditTrailTest extends AbstractIntegrationTest {
    @Autowired private AuditService audit;
    @Autowired private TransactionTemplate transaction;

    private User gabonAdmin;
    private User gabonViewer;
    private User root;

    @BeforeEach
    void people() {
        gabonAdmin = staff("ga.admin@ndzela.test", "GA", "ADMIN");
        gabonViewer = staff("ga.viewer@ndzela.test", "GA", "VIEWER");
        root = staff("root@ndzela.test", null, "SUPER_ADMIN");
    }

    private int rows(String action) {
        return jdbc.queryForObject("select count(*) from audit_logs where action = ?", Integer.class, action);
    }

    private void call(MockHttpServletRequestBuilder request, User as) throws Exception {
        mvc.perform(request.header("Authorization", bearer(as))).andExpect(status().is2xxSuccessful());
    }

    @Test
    void theTableRefusesUpdatesAndDeletes() {
        audit.record("TEST_EVENT", "TEST", "1", "label", null, null);

        assertThrows(DataAccessException.class, () -> jdbc.update("update audit_logs set action = 'CHANGED'"));
        assertThrows(DataAccessException.class, () -> jdbc.update("delete from audit_logs"));
        assertEquals(1, rows("TEST_EVENT"));
    }

    @Test
    void userActionsEachWriteOneRowWithTheTargetsCountry() throws Exception {
        String base = "/api/v1/users/" + gabonViewer.getId();
        String viewerRole = roles.findByName("VIEWER").orElseThrow().getId().toString();

        call(put(base).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"X\",\"roleIds\":[\"" + viewerRole + "\"]}"), gabonAdmin);
        call(post(base + "/disable").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Motif de test\"}"), gabonAdmin);
        call(post(base + "/enable"), gabonAdmin);
        call(post(base + "/lock").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Motif de test\"}"), gabonAdmin);
        call(post(base + "/unlock"), gabonAdmin);
        call(post(base + "/reset-mfa"), gabonAdmin);
        call(post(base + "/resend-invitation"), gabonAdmin);
        call(delete(base), gabonAdmin);

        for (String action : List.of("USER_UPDATED", "USER_DISABLED", "USER_ENABLED", "USER_LOCKED", "USER_UNLOCKED",
                "USER_MFA_RESET", "USER_INVITATION_RESENT", "USER_DELETED")) {
            assertEquals(1, rows(action), action);
        }
        assertEquals(8, jdbc.queryForObject(
                "select count(*) from audit_logs a join countries c on c.id = a.country_id where c.code = 'GA' and a.actor = 'ga.admin@ndzela.test' and a.target_type = 'USER'",
                Integer.class));
    }

    @Test
    void creatingAUserIsRecordedInTheChosenCountry() throws Exception {
        String viewerRole = roles.findByName("VIEWER").orElseThrow().getId().toString();

        mvc.perform(post("/api/v1/users").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@ndzela.test\",\"roleIds\":[\"" + viewerRole + "\"],\"countryIds\":[\"" + country("ML").getId() + "\"]}"))
                .andExpect(status().isCreated());

        assertEquals("ML", jdbc.queryForObject(
                "select c.code from audit_logs a join countries c on c.id = a.country_id where a.action = 'USER_CREATED'", String.class));
    }

    @Test
    void roleActionsAreRecordedAsGlobalEvents() throws Exception {
        mvc.perform(post("/api/v1/roles").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"AUDITEUR\",\"permissions\":[\"users:read\"]}")).andExpect(status().isCreated());
        String id = roles.findByName("AUDITEUR").orElseThrow().getId().toString();
        call(put("/api/v1/roles/" + id + "/permissions").contentType(MediaType.APPLICATION_JSON).content("{\"permissions\":[\"users:read\",\"roles:read\"]}"), root);
        call(delete("/api/v1/roles/" + id), root);

        assertEquals(1, rows("ROLE_CREATED"));
        assertEquals(1, rows("ROLE_PERMISSIONS_UPDATED"));
        assertEquals(1, rows("ROLE_DELETED"));
        assertEquals(3, jdbc.queryForObject("select count(*) from audit_logs where country_id is null and target_type = 'ROLE'", Integer.class));
    }

    @Test
    void aRolledBackActionLeavesNoRow() {
        transaction.executeWithoutResult(status -> {
            audit.record("ROLLED_BACK", "TEST", "1", "label", null, null);
            status.setRollbackOnly();
        });

        assertEquals(0, rows("ROLLED_BACK"));
    }

    @Test
    void aFailedSignInIsRecordedEvenThoughTheRequestFails() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ga.admin@ndzela.test\",\"password\":\"Wrong!Pass1\"}"))
                .andExpect(status().isUnauthorized());

        assertEquals("FAILURE", jdbc.queryForObject("select result from audit_logs where action = 'LOGIN_FAILED'", String.class));
        assertEquals("ga.admin@ndzela.test", jdbc.queryForObject("select actor from audit_logs where action = 'LOGIN_FAILED'", String.class));
        assertEquals("GA", jdbc.queryForObject(
                "select c.code from audit_logs a join countries c on c.id = a.country_id where a.action = 'LOGIN_FAILED'", String.class));
    }
}
