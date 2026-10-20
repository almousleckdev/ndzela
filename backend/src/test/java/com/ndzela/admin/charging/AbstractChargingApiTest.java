package com.ndzela.admin.charging;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.user.domain.User;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Shared setup and helpers.
abstract class AbstractChargingApiTest extends AbstractDriverTest {
    protected User cashier;
    protected User accountant;
    protected String vehicle;
    protected String driver;
    protected String station;

    @BeforeEach
    void chargingSetup() throws Exception {
        cashier = staff("ga.cashier@ndzela.test", "GA", "CAISSIER");
        accountant = staff("ga.accountant@ndzela.test", "GA", "COMPTABLE");
        vehicle = newVehicle(manager);
        driver = id(createDriver(manager, DriverJson.create(vehicle, newCenter(manager, "NDZELA"))).andExpect(status().isCreated()));
        station = id(postJson("/api/v1/charging/stations", "{\"name\":\"Station Akanda\"}", manager).andExpect(status().isCreated()));
    }

    protected String recharge(String target, int amount, String method, String reference, int before, int after, UUID requestId) {
        return "{" + target + ",\"stationId\":\"" + station + "\",\"amount\":" + amount + ",\"method\":\"" + method + "\"" + (reference == null ? "" : ",\"reference\":\"" + reference + "\"")
                + ",\"batteryBefore\":" + before + ",\"batteryAfter\":" + after + ",\"requestId\":\"" + requestId + "\"}";
    }

    protected String own() {
        return "\"vehicleId\":\"" + vehicle + "\"";
    }

    protected ResultActions charge(User as, int before, int after) throws Exception {
        return postJson("/api/v1/charging/recharges", recharge(own(), 5000, "AIRTEL_MONEY", "R1", before, after, UUID.randomUUID()), as);
    }

    protected String entryId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
