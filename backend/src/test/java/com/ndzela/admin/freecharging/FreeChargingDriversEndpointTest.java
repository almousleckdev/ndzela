package com.ndzela.admin.freecharging;

import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.vehicle.VehicleApiTest;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The driver search of the Gratuité pages: staff with free_charging:manage, and Zella with its partner key.
class FreeChargingDriversEndpointTest extends AbstractFreeChargingTest {
    private static final String STAFF = "/api/v1/free-charging/drivers";
    private static final String PARTNER = "/api/v1/partners/zella/free-charging/drivers";
    private static final String KEY = "partner-test-key";

    private UUID withCar(String last, String fleet, String plate) throws Exception {
        String vehicle = id(postJson("/api/v1/vehicles", VehicleApiTest.json(fleet, plate), manager).andExpect(status().isCreated()));
        UUID created = UUID.fromString(id(createDriver(manager, DriverJson.create(vehicle, newCenter(manager, "C-" + fleet)).personal("lastName", last)).andExpect(status().isCreated())));
        link(created);
        return created;
    }

    @Test
    void staffWhoManageFindADriverByHisCarAndSeeWhatTheyNeedToPickHim() throws Exception {
        UUID paul = withCar("NZE", "V015", "PL-0150");

        send(get(STAFF).param("q", "v015"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].driverId").value(paul.toString()))
                .andExpect(jsonPath("$[0].customerId").value("cust-" + paul))
                .andExpect(jsonPath("$[0].fullName").isNotEmpty())
                .andExpect(jsonPath("$[0].fleetNumber").value("V015"))
                .andExpect(jsonPath("$[0].plate").value("PL-0150"))
                .andExpect(jsonPath("$[0].linked").value(true))
                .andExpect(jsonPath("$[0].inProgram").value(false));
    }

    @Test
    void aReaderWithoutManageIsRefused() throws Exception {
        send(get(STAFF).param("q", "x"), viewer).andExpect(status().isForbidden());
        send(get(STAFF).param("q", "x"), manager).andExpect(status().isForbidden());
    }

    @Test
    void theSizeIsBoundedAndABadSizeIsHandled() throws Exception {
        withCar("NZE", "V015", "PL-0150");
        withCar("OBIANG", "V030", "PL-0300");

        send(get(STAFF).param("size", "1"), admin).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        send(get(STAFF).param("size", "0"), admin).andExpect(status().isOk());
        send(get(STAFF).param("size", "100000"), admin).andExpect(status().isOk());
    }

    @Test
    void zellaSearchesWithItsKeyAndNothingWithoutIt() throws Exception {
        UUID paul = withCar("NZE", "V015", "PL-0150");

        mvc.perform(get(PARTNER).param("q", "nze").header("X-Api-Key", KEY)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].driverId").value(paul.toString()));
        mvc.perform(get(PARTNER).param("q", "nze")).andExpect(status().isUnauthorized());
        mvc.perform(get(PARTNER).param("q", "nze").header("X-Api-Key", "wrong")).andExpect(status().isUnauthorized());
    }
}
