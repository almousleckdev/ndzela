package com.ndzela.admin.user;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Country staff never see or change another country's staff; only the global super admin crosses. */
class UserIsolationTest extends AbstractIntegrationTest {
    private User gabonAdmin;
    private User gabonViewer;
    private User maliAdmin;
    private User maliViewer;
    private User root;

    @BeforeEach
    void people() {
        gabonAdmin = staff("ga.admin@ndzela.test", "GA", "ADMIN");
        gabonViewer = staff("ga.viewer@ndzela.test", "GA", "VIEWER");
        maliAdmin = staff("ml.admin@ndzela.test", "ML", "ADMIN");
        maliViewer = staff("ml.viewer@ndzela.test", "ML", "VIEWER");
        root = staff("root@ndzela.test", null, "SUPER_ADMIN");
    }

    private ResultActions call(MockHttpServletRequestBuilder request, User as) throws Exception {
        return mvc.perform(request.header("Authorization", bearer(as)));
    }

    private ResultActions callSelecting(MockHttpServletRequestBuilder request, User as, String country) throws Exception {
        return mvc.perform(request.header("Authorization", bearer(as)).header("X-Country", country));
    }

    @Test
    void countryStaffListOnlyTheUsersOfTheirCountry() throws Exception {
        call(get("/api/v1/users"), gabonAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email").value(containsInAnyOrder("ga.admin@ndzela.test", "ga.viewer@ndzela.test")));
    }

    @Test
    void theGlobalSuperAdminSeesEveryoneWhenNoCountryIsSelected() throws Exception {
        call(get("/api/v1/users"), root)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void theSuperAdminSelectingACountrySeesOnlyThatCountry() throws Exception {
        callSelecting(get("/api/v1/users"), root, "GA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email").value(containsInAnyOrder("ga.admin@ndzela.test", "ga.viewer@ndzela.test")));
    }

    @Test
    void theCountryHeaderIsIgnoredForCountryStaff() throws Exception {
        callSelecting(get("/api/v1/users"), gabonAdmin, "ML")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email").value(containsInAnyOrder("ga.admin@ndzela.test", "ga.viewer@ndzela.test")));
    }

    @Test
    void anUnknownCountryHeaderIsRejected() throws Exception {
        callSelecting(get("/api/v1/users"), root, "XX")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COUNTRY"));
    }

    private static final String REASON = "{\"reason\":\"Motif de test\"}";

    @Test
    void countryStaffCannotReachAUserOfAnotherCountryByAnyAction() throws Exception {
        UUID id = maliViewer.getId();
        UUID viewerRole = roles.findByName("VIEWER").orElseThrow().getId();
        List<MockHttpServletRequestBuilder> attempts = List.of(
                put("/api/v1/users/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"X\",\"roleIds\":[\"" + viewerRole + "\"]}"),
                post("/api/v1/users/" + id + "/disable").contentType(MediaType.APPLICATION_JSON).content(REASON),
                post("/api/v1/users/" + id + "/enable"),
                post("/api/v1/users/" + id + "/lock").contentType(MediaType.APPLICATION_JSON).content(REASON),
                post("/api/v1/users/" + id + "/unlock"),
                post("/api/v1/users/" + id + "/reset-mfa"),
                post("/api/v1/users/" + id + "/resend-invitation"),
                delete("/api/v1/users/" + id));

        for (MockHttpServletRequestBuilder attempt : attempts) {
            call(attempt, gabonAdmin)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        User untouched = users.findById(id).orElseThrow();
        assertEquals(UserStatus.ACTIVE, untouched.getStatus());
        assertEquals("ml.viewer@ndzela.test", untouched.getEmail());
    }

    @Test
    void countryStaffCannotReachAGlobalUser() throws Exception {
        call(post("/api/v1/users/" + root.getId() + "/disable").contentType(MediaType.APPLICATION_JSON).content(REASON), gabonAdmin)
                .andExpect(status().isNotFound());
    }

    @Test
    void theGlobalSuperAdminCanManageAUserOfAnyCountryWithoutSelectingOne() throws Exception {
        call(post("/api/v1/users/" + maliViewer.getId() + "/disable").contentType(MediaType.APPLICATION_JSON).content(REASON), root)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void roleMemberCountsAreCountedInTheCountryInUse() throws Exception {
        call(get("/api/v1/roles"), gabonAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='VIEWER')].userCount").value(1))
                .andExpect(jsonPath("$[?(@.name=='ADMIN')].userCount").value(1));

        call(get("/api/v1/roles"), root)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='VIEWER')].userCount").value(2))
                .andExpect(jsonPath("$[?(@.name=='ADMIN')].userCount").value(2));
    }
}
