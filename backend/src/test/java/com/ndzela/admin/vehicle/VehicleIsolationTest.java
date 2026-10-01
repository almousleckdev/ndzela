package com.ndzela.admin.vehicle;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Vehicles belong to one country: other countries cannot see or change them. */
class VehicleIsolationTest extends AbstractIntegrationTest {
    private User gabonManager;
    private User maliManager;
    private User root;
    private String maliVehicle;

    @BeforeEach
    void data() throws Exception {
        gabonManager = staff("ga.manager@ndzela.test", "GA", "GESTIONNAIRE");
        maliManager = staff("ml.manager@ndzela.test", "ML", "GESTIONNAIRE");
        root = staff("root@ndzela.test", null, "SUPER_ADMIN");
        mvc.perform(post("/api/v1/vehicles").header("Authorization", bearer(gabonManager)).contentType(MediaType.APPLICATION_JSON).content(VehicleApiTest.json("V-001", "GA-1-GA")))
                .andExpect(status().isCreated());
        String response = mvc.perform(post("/api/v1/vehicles").header("Authorization", bearer(maliManager)).contentType(MediaType.APPLICATION_JSON).content(VehicleApiTest.json("V-900", "ML-9-ML")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        maliVehicle = com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private ResultActions call(MockHttpServletRequestBuilder request, User as) throws Exception {
        return mvc.perform(request.header("Authorization", bearer(as)));
    }

    @Test
    void anotherCountryCannotReachAVehicleByAnyAction() throws Exception {
        String base = "/api/v1/vehicles/" + maliVehicle;
        call(get(base), gabonManager).andExpect(status().isNotFound());
        call(put(base).contentType(MediaType.APPLICATION_JSON).content(VehicleApiTest.json("V-900", "ML-9-ML")), gabonManager).andExpect(status().isNotFound());
        call(post(base + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_MAINTENANCE\",\"reason\":\"test\"}"), gabonManager).andExpect(status().isNotFound());
        call(delete(base), gabonManager).andExpect(status().isNotFound());
        call(get(base), maliManager).andExpect(status().isOk());
    }

    @Test
    void listsAreScopedToTheCountryAndTheSuperAdminSeesAll() throws Exception {
        call(get("/api/v1/vehicles"), gabonManager).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].fleetNumber").value("V-001"));
        call(get("/api/v1/vehicles/counts"), gabonManager).andExpect(jsonPath("$.AVAILABLE").value(1));
        call(get("/api/v1/vehicles"), root).andExpect(jsonPath("$.total").value(2));
        call(get("/api/v1/vehicles").header("X-Country", "ML"), root).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void theSuperAdminMustPickACountryToCreate() throws Exception {
        call(post("/api/v1/vehicles").contentType(MediaType.APPLICATION_JSON).content(VehicleApiTest.json("V-002", "GA-2-GA")), root)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("COUNTRY_REQUIRED"));
        call(post("/api/v1/vehicles").header("X-Country", "GA").contentType(MediaType.APPLICATION_JSON).content(VehicleApiTest.json("V-002", "GA-2-GA")), root)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.country.code").value("GA"));
    }

    @Test
    void theSuperAdminCanUpdateAVehicleOfAnyCountryWithoutSelectingOne() throws Exception {
        call(put("/api/v1/vehicles/" + maliVehicle).contentType(MediaType.APPLICATION_JSON).content(VehicleApiTest.json("V-900", "ML-9-ML").replace("Blanc", "Rouge")), root)
                .andExpect(status().isOk()).andExpect(jsonPath("$.country.code").value("ML")).andExpect(jsonPath("$.color").value("Rouge"));
    }
}
