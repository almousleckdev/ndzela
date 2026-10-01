package com.ndzela.admin.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DriverIsolationTest extends AbstractDriverTest {
    private String maliDriver;
    private String gabonVehicle;

    @BeforeEach
    void setup() throws Exception {
        String gabonCenter = newCenter(manager, "NDZELA");
        gabonVehicle = newVehicle(manager);
        createDriver(manager, DriverJson.create(gabonVehicle, gabonCenter)).andExpect(status().isCreated());
        maliDriver = id(createDriver(maliManager, DriverJson.create(newVehicle(maliManager), newCenter(maliManager, "Centre Mali"))).andExpect(status().isCreated()));
    }

    @Test
    void anotherCountryCannotReachADriverByAnyAction() throws Exception {
        String base = "/api/v1/drivers/" + maliDriver;
        send(get(base), manager).andExpect(status().isNotFound());
        send(put(base).contentType("application/json").content(DriverJson.create(null, "00000000-0000-0000-0000-000000000001").buildUpdate()), manager).andExpect(status().isNotFound());
        postJson(base + "/suspend", REASON, admin).andExpect(status().isNotFound());
        send(post(base + "/reactivate"), admin).andExpect(status().isNotFound());
        send(delete(base), manager).andExpect(status().isNotFound());
        postJson(base + "/vehicle/remove", REASON, manager).andExpect(status().isNotFound());
        postJson(base + "/vehicle", "{\"vehicleId\":\"" + gabonVehicle + "\",\"deposit\":250000}", manager).andExpect(status().isNotFound());
    }

    @Test
    void listsAreScopedAndTheSuperAdminSeesEveryCountry() throws Exception {
        send(get("/api/v1/drivers"), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/drivers/stats"), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/drivers"), root).andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/drivers").header("X-Country", "ML"), root).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/drivers/" + maliDriver), root).andExpect(status().isOk()).andExpect(jsonPath("$.country.code").value("ML"));
    }
}
