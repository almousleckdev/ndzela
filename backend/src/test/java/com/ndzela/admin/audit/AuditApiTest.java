package com.ndzela.admin.audit;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditApiTest extends AbstractIntegrationTest {
    @Autowired private AuditService audit;

    private User gabonAdmin;
    private User maliAdmin;
    private User root;

    @BeforeEach
    void data() {
        gabonAdmin = staff("ga.admin@ndzela.test", "GA", "ADMIN");
        maliAdmin = staff("ml.admin@ndzela.test", "ML", "ADMIN");
        root = staff("root@ndzela.test", null, "SUPER_ADMIN");
        audit.record("VEHICLE_CREATED", "VEHICLE", "1", "V-001", country("GA").getId(), "a");
        audit.record("VEHICLE_UPDATED", "VEHICLE", "1", "V-001", country("GA").getId(), "b");
        audit.record("DRIVER_CREATED", "DRIVER", "2", "Jean", country("GA").getId(), "c");
        audit.record("VEHICLE_CREATED", "VEHICLE", "3", "V-900", country("ML").getId(), "d");
        audit.record("ROLE_CREATED", "ROLE", "4", "AUDITEUR", null, "e");
    }

    private ResultActions list(User as, String query) throws Exception {
        return list(get("/api/v1/audit" + query), as);
    }

    private ResultActions list(MockHttpServletRequestBuilder request, User as) throws Exception {
        return mvc.perform(request.header("Authorization", bearer(as)));
    }

    @Test
    void countryStaffSeeOnlyTheirCountry() throws Exception {
        list(gabonAdmin, "").andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[*].action").value(containsInAnyOrder("VEHICLE_CREATED", "VEHICLE_UPDATED", "DRIVER_CREATED")));
        list(maliAdmin, "").andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].target").value("V-900"));
    }

    @Test
    void theGlobalSuperAdminSeesEverythingOrOneCountry() throws Exception {
        list(root, "").andExpect(jsonPath("$.total").value(5));
        list(get("/api/v1/audit").header("X-Country", "GA"), root).andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void filtersNarrowTheResult() throws Exception {
        list(root, "?action=VEHICLE_CREATED").andExpect(jsonPath("$.total").value(2));
        list(root, "?targetType=DRIVER").andExpect(jsonPath("$.total").value(1));
        list(root, "?actor=nobody").andExpect(jsonPath("$.total").value(0));
        list(root, "?from=2999-01-01").andExpect(jsonPath("$.total").value(0));
        list(root, "?to=2000-01-01").andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void resultsArePagedNewestFirst() throws Exception {
        list(root, "?size=2&page=0").andExpect(jsonPath("$.items.length()").value(2)).andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.items[0].action").value("ROLE_CREATED"));
        list(root, "?size=2&page=2").andExpect(jsonPath("$.items.length()").value(1));
        list(root, "?size=1000").andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void requiresTheAuditReadPermission() throws Exception {
        list(staff("ga.viewer@ndzela.test", "GA", "VIEWER"), "").andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/audit")).andExpect(status().isUnauthorized());
    }
}
