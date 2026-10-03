package com.ndzela.admin.driverapp;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A GA driver created by staff, with helpers to sign in and call the driver API.
abstract class AbstractDriverAppTest extends AbstractDriverTest {
    static final String DRIVER_PASSWORD = "Secret123";

    protected DriverJson body;
    protected String driverId;
    protected String email;

    @BeforeEach
    void driver() throws Exception {
        body = DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"));
        driverId = id(createDriver(manager, body).andExpect(status().isCreated()));
        email = String.valueOf(body.personalValue("email"));
    }

    protected ResultActions login(String identifier, String password) throws Exception {
        return mvc.perform(post("/api/v1/driver/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}"));
    }

    protected void firstPasswordAlreadyChanged() {
        jdbc.update("update drivers set must_change_password = false");
    }

    protected String accessToken() throws Exception {
        return JsonPath.read(login(email, DRIVER_PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    protected ResultActions asDriver(String path, String token) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + token));
    }
}
