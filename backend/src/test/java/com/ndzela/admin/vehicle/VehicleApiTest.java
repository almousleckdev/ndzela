package com.ndzela.admin.vehicle;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class VehicleApiTest extends AbstractIntegrationTest {
    private User manager;
    private User maliManager;
    private User viewer;

    @BeforeEach
    void people() {
        manager = staff("ga.manager@ndzela.test", "GA", "GESTIONNAIRE");
        maliManager = staff("ml.manager@ndzela.test", "ML", "GESTIONNAIRE");
        viewer = staff("ga.viewer@ndzela.test", "GA", "VIEWER");
    }

    public static String json(String fleetNumber, String plate) {
        return """
                {"fleetNumber":"%s","brand":"Dongfeng","model":"Berline","year":2026,"plate":"%s","color":"Blanc",
                 "vin":null,"purchasePrice":15000000,"dailyRent":23000}""".formatted(fleetNumber, plate);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, User as) throws Exception {
        return mvc.perform(request.header("Authorization", bearer(as)));
    }

    private ResultActions create(User as, String body) throws Exception {
        return send(post("/api/v1/vehicles").contentType(MediaType.APPLICATION_JSON).content(body), as);
    }

    private String createdId(User as, String fleet, String plate) throws Exception {
        String response = create(as, json(fleet, plate)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private User root() {
        return staff("root@ndzela.test", null, "SUPER_ADMIN");
    }

    private int auditRows(String action) {
        return jdbc.queryForObject("select count(*) from audit_logs where action = ?", Integer.class, action);
    }

    @Test
    void createsAnAvailableVehicleInTheUsersCountryAndAuditsIt() throws Exception {
        create(manager, json("V-001", " ma-937-aa "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.plate").value("MA-937-AA"))
                .andExpect(jsonPath("$.country.code").value("GA"))
                .andExpect(jsonPath("$.dailyRent").value(23000));

        assertEquals(1, auditRows("VEHICLE_CREATED"));
    }

    @Test
    void rejectsInvalidData() throws Exception {
        create(manager, "{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        create(manager, json("V-002", "AA-1-AA").replace("2026", "1800")).andExpect(status().isBadRequest());
        create(manager, json("V-003", "AA-2-AA").replace("15000000", "-5")).andExpect(status().isBadRequest());
        create(manager, json("V-004", "AA-3-AA").replace("23000", "23000.50")).andExpect(status().isBadRequest());
        create(manager, json("V-005", "AA-4-AA").replace("2026", "2999")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void theFleetNumberAndPlateAreUniquePerCountry() throws Exception {
        createdId(manager, "V-001", "MA-937-AA");

        create(manager, json("V-001", "OTHER-1")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_NUMBER_TAKEN"));
        create(manager, json("V-002", "ma-937-aa")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_PLATE_TAKEN"));
        create(maliManager, json("V-001", "MA-937-AA")).andExpect(status().isCreated());
    }

    @Test
    void updatesAVehicleAndKeepsUniquenessRules() throws Exception {
        String first = createdId(manager, "V-001", "AA-1-AA");
        createdId(manager, "V-002", "AA-2-AA");

        send(put("/api/v1/vehicles/" + first).contentType(MediaType.APPLICATION_JSON).content(json("V-001", "AA-1-AA").replace("Blanc", "Noir")), manager)
                .andExpect(status().isOk()).andExpect(jsonPath("$.color").value("Noir"));
        send(put("/api/v1/vehicles/" + first).contentType(MediaType.APPLICATION_JSON).content(json("V-002", "AA-1-AA")), manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_NUMBER_TAKEN"));

        assertEquals(1, auditRows("VEHICLE_UPDATED"));
    }

    @Test
    void statusChangesFollowTheRules() throws Exception {
        String id = createdId(manager, "V-001", "AA-1-AA");
        String body = "{\"status\":\"%s\",\"reason\":\"test\"}";

        send(post("/api/v1/vehicles/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content(body.formatted("IN_MAINTENANCE")), manager)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_MAINTENANCE"));
        send(post("/api/v1/vehicles/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content(body.formatted("ASSIGNED")), manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_STATUS_MANAGED"));

        jdbc.update("update vehicles set status = 'ASSIGNED' where id = ?::uuid", id);
        send(post("/api/v1/vehicles/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content(body.formatted("AVAILABLE")), manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_ASSIGNED"));
        send(delete("/api/v1/vehicles/" + id), manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_ASSIGNED"));

        assertEquals(1, auditRows("VEHICLE_STATUS_CHANGED"));
    }

    @Test
    void theFleetNumberIsNormalisedSoLookalikesAreDuplicates() throws Exception {
        createdId(manager, "box 001", "AA-1-AA");

        create(manager, json("BOX  001", "BB-2-BB")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_NUMBER_TAKEN"));
    }

    @Test
    void anInvalidQueryParameterAnswers400NotAServerError() throws Exception {
        send(get("/api/v1/vehicles?status=FOO"), manager).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        send(get("/api/v1/audit?from=abc"), root()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void settingTheSameStatusAgainWritesNoAuditRow() throws Exception {
        String id = createdId(manager, "V-001", "AA-1-AA");

        send(post("/api/v1/vehicles/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"AVAILABLE\"}"), manager).andExpect(status().isOk());

        assertEquals(0, auditRows("VEHICLE_STATUS_CHANGED"));
    }

    @Test
    void deletesAVehicleThatWasNeverAssigned() throws Exception {
        String id = createdId(manager, "V-001", "AA-1-AA");

        send(delete("/api/v1/vehicles/" + id), manager).andExpect(status().isNoContent());
        send(get("/api/v1/vehicles/" + id), manager).andExpect(status().isNotFound());
        assertEquals(1, auditRows("VEHICLE_DELETED"));
    }

    @Test
    void listsWithPagingFiltersSearchAndCounts() throws Exception {
        createdId(manager, "V-001", "AA-1-AA");
        createdId(manager, "V-002", "BB-2-BB");
        String third = createdId(manager, "V-003", "CC-3-CC");
        send(post("/api/v1/vehicles/" + third + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OUT_OF_SERVICE\",\"reason\":\"test\"}"), manager).andExpect(status().isOk());

        send(get("/api/v1/vehicles"), manager).andExpect(jsonPath("$.total").value(3));
        send(get("/api/v1/vehicles?size=2&page=1"), manager).andExpect(jsonPath("$.items.length()").value(1));
        send(get("/api/v1/vehicles?status=OUT_OF_SERVICE"), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/vehicles?q=bb-2"), manager).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].fleetNumber").value("V-002"));
        send(get("/api/v1/vehicles/counts"), manager)
                .andExpect(jsonPath("$.AVAILABLE").value(2)).andExpect(jsonPath("$.OUT_OF_SERVICE").value(1));
    }

    @Test
    void onlyTheRightPermissionsMayCallTheApi() throws Exception {
        String id = createdId(manager, "V-001", "AA-1-AA");

        send(get("/api/v1/vehicles"), viewer).andExpect(status().isOk()).andExpect(jsonPath("$.items[*].fleetNumber").value(containsInAnyOrder("V-001")));
        create(viewer, json("V-009", "ZZ-9-ZZ")).andExpect(status().isForbidden());
        send(put("/api/v1/vehicles/" + id).contentType(MediaType.APPLICATION_JSON).content(json("V-001", "AA-1-AA")), viewer).andExpect(status().isForbidden());
        send(delete("/api/v1/vehicles/" + id), viewer).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/vehicles")).andExpect(status().isUnauthorized());
    }
}
