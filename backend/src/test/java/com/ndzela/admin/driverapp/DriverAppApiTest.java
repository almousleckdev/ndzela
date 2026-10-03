package com.ndzela.admin.driverapp;

import com.ndzela.admin.driver.DriverJson;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DriverAppApiTest extends AbstractDriverAppTest {
    @Test
    void theDriverSeesOwnProfileVehicleAndAccount() throws Exception {
        firstPasswordAlreadyChanged();
        String token = accessToken();

        asDriver("/api/v1/driver/me", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(String.valueOf(body.personalValue("firstName"))))
                .andExpect(jsonPath("$.phone").value(String.valueOf(body.personalValue("phonePrimary"))))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").isNotEmpty()).andExpect(jsonPath("$.plate").isNotEmpty());
        asDriver("/api/v1/driver/account", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.unpaidDays").value(0)).andExpect(jsonPath("$.totalDue").value(0)).andExpect(jsonPath("$.currency").value("XAF"));
    }

    @Test
    void theAccountShowsTheOverdueRent() throws Exception {
        firstPasswordAlreadyChanged();
        jdbc.update("update contracts set start_date = current_date - 10, end_date = current_date + 300");
        String token = accessToken();

        asDriver("/api/v1/driver/account", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.unpaidDays").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.totalDue").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void theOperatorsAreThoseOfTheDriversCountry() throws Exception {
        firstPasswordAlreadyChanged();
        asDriver("/api/v1/driver/payments/operators", accessToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].code").value("AIRTEL_MONEY")).andExpect(jsonPath("$[1].code").value("MOOV_MONEY"));

        DriverJson mali = DriverJson.create(newVehicle(maliManager), newCenter(maliManager, "NDZELA ML"));
        createDriver(maliManager, mali).andExpect(status().isCreated());
        jdbc.update("update drivers set must_change_password = false");
        String maliToken = com.jayway.jsonpath.JsonPath.read(login(String.valueOf(mali.personalValue("email")), DRIVER_PASSWORD).andReturn().getResponse().getContentAsString(), "$.accessToken");
        asDriver("/api/v1/driver/payments/operators", maliToken).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].code").value("ORANGE_MONEY")).andExpect(jsonPath("$[1].code").value("MOOV_MONEY"));
        asDriver("/api/v1/driver/me", maliToken).andExpect(jsonPath("$.email").value(String.valueOf(mali.personalValue("email")))).andExpect(jsonPath("$.countryCode").value("ML"));
        asDriver("/api/v1/driver/me", accessToken()).andExpect(jsonPath("$.countryCode").value("GA"));
    }

    @Test
    void aSuspendedDriverCanSeeTheAccountAndPayButNotTheContract() throws Exception {
        firstPasswordAlreadyChanged();
        String token = accessToken();
        jdbc.update("update drivers set status = 'SUSPENDED'");

        asDriver("/api/v1/driver/account", token).andExpect(status().isOk());
        asDriver("/api/v1/driver/payments/operators", token).andExpect(status().isOk());
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isForbidden());
    }

    @Test
    void aBlockedDriversTokenStopsWorkingAtOnce() throws Exception {
        firstPasswordAlreadyChanged();
        String token = accessToken();
        jdbc.update("update drivers set status = 'BLOCKED'");

        asDriver("/api/v1/driver/me", token).andExpect(status().isUnauthorized());
    }

    @Test
    void driverAndStaffTokensDoNotOpenEachOthersDoors() throws Exception {
        firstPasswordAlreadyChanged();
        String driverToken = accessToken();

        asDriver("/api/v1/drivers", driverToken).andExpect(status().isUnauthorized());
        asDriver("/api/v1/vehicles", driverToken).andExpect(status().isUnauthorized());
        asDriver("/api/v1/profile/me", driverToken).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/driver/me").header("Authorization", bearer(manager))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/driver/me").header("Authorization", bearer(root))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/driver/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/driver/me").header("Authorization", "Bearer garbage")).andExpect(status().isUnauthorized());
    }

    @Test
    void aDriverOnlyEverSeesTheirOwnData() throws Exception {
        firstPasswordAlreadyChanged();
        DriverJson other = DriverJson.create(newVehicle(manager), newCenter(manager, "AUTRE"));
        createDriver(manager, other).andExpect(status().isCreated());
        jdbc.update("update drivers set must_change_password = false");

        String token = com.jayway.jsonpath.JsonPath.read(login(String.valueOf(other.personalValue("email")), DRIVER_PASSWORD).andReturn().getResponse().getContentAsString(), "$.accessToken");
        asDriver("/api/v1/driver/me", token).andExpect(jsonPath("$.email").value(String.valueOf(other.personalValue("email"))));
        asDriver("/api/v1/driver/me", accessToken()).andExpect(jsonPath("$.email").value(email));
    }
}
