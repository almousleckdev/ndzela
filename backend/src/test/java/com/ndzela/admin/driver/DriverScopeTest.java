package com.ndzela.admin.driver;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Current riders and former (archived) riders are listed apart
class DriverScopeTest extends AbstractDriverTest {
    @Test
    void theScopeSeparatesCurrentRidersFromFormerOnes() throws Exception {
        createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated());

        send(get("/api/v1/drivers?scope=CURRENT"), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/drivers?scope=FORMER"), manager).andExpect(jsonPath("$.total").value(0));

        jdbc.update("update drivers set status = 'INACTIVE'");

        send(get("/api/v1/drivers?scope=CURRENT"), manager).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/drivers?scope=FORMER"), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/drivers"), manager).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void anotherCountryNeverSeesTheRiders() throws Exception {
        createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated());
        jdbc.update("update drivers set status = 'INACTIVE'");

        send(get("/api/v1/drivers?scope=FORMER"), maliManager).andExpect(jsonPath("$.total").value(0));
    }
}
