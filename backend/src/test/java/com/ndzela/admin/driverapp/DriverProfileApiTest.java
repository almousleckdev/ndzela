package com.ndzela.admin.driverapp;

import com.ndzela.admin.driver.DriverJson;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DriverProfileApiTest extends AbstractDriverAppTest {
    private String token;

    @BeforeEach
    void signedIn() throws Exception {
        firstPasswordAlreadyChanged();
        token = accessToken();
    }

    private void message(String text, int expected) throws Exception {
        mvc.perform(post("/api/v1/driver/support/messages").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"" + text + "\"}")).andExpect(status().is(expected));
    }

    @Test
    void aDriverCanAskForTheDeletionOfTheirAccountAndTheStaffIsAlerted() throws Exception {
        mvc.perform(post("/api/v1/driver/account/deletion-request").header("Authorization", "Bearer " + token)).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/driver/account/deletion-request")).andExpect(status().isUnauthorized());
        org.junit.jupiter.api.Assertions.assertTrue(rows("select count(*) from notifications where type = 'DRIVER_DELETION_REQUEST'") >= 1);
        org.junit.jupiter.api.Assertions.assertEquals(1, auditRows("DRIVER_DELETION_REQUESTED"));
        org.junit.jupiter.api.Assertions.assertEquals(1, rows("select count(*) from support_messages where body like 'Demande de suppression%'"));
    }

    @Test
    void theProfileCarriesTheBirthDateAndTheAddress() throws Exception {
        asDriver("/api/v1/driver/me", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.localPhone").value("0" + String.valueOf(body.personalValue("phonePrimary")).substring(4)))
                .andExpect(jsonPath("$.birthDate").value(String.valueOf(body.personalValue("birthDate"))))
                .andExpect(jsonPath("$.address").value(org.hamcrest.Matchers.containsString(String.valueOf(body.personalValue("city")))));
    }

    @Test
    void theContractShowsNumberDatesDurationAndDailyRent() throws Exception {
        asDriver("/api/v1/driver/contract", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.contractNumber").value(org.hamcrest.Matchers.startsWith("NDZ-")))
                .andExpect(jsonPath("$.startDate").isNotEmpty()).andExpect(jsonPath("$.endDate").isNotEmpty())
                .andExpect(jsonPath("$.durationMonths").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.dailyRent").value(org.hamcrest.Matchers.greaterThan(0))).andExpect(jsonPath("$.currency").value("XAF"))
                .andExpect(jsonPath("$.progress.totalDays").value(1248)).andExpect(jsonPath("$.progress.paidAmount").value(0))
                .andExpect(jsonPath("$.progress.remainingAmount").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void theDocumentsTellWhichOnesExist() throws Exception {
        asDriver("/api/v1/driver/documents", token).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[?(@.kind=='CONTRACT')].available").value(true))
                .andExpect(jsonPath("$[?(@.kind=='LICENCE')].available").value(false));

        jdbc.update("update drivers set licence_file = 'licence.pdf'");
        asDriver("/api/v1/driver/documents", token).andExpect(jsonPath("$[?(@.kind=='LICENCE')].available").value(true));
    }

    @Test
    void theSupportContactsAreThoseOfTheDriversCountry() throws Exception {
        asDriver("/api/v1/driver/support", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+24162821855")).andExpect(jsonPath("$.whatsapp").value("+24102821855")).andExpect(jsonPath("$.email").value("ndzelagabon@gmail.com"));

        DriverJson mali = DriverJson.create(newVehicle(maliManager), newCenter(maliManager, "NDZELA ML"));
        createDriver(maliManager, mali).andExpect(status().isCreated());
        jdbc.update("update drivers set must_change_password = false");
        jdbc.update("update countries set support_phone = null, support_whatsapp = null, support_email = null where code = 'ML'");
        String maliToken = JsonPath.read(login(String.valueOf(mali.personalValue("email")), DRIVER_PASSWORD).andReturn().getResponse().getContentAsString(), "$.accessToken");
        asDriver("/api/v1/driver/support", maliToken).andExpect(status().isOk()).andExpect(jsonPath("$.phone").doesNotExist()).andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void aMessageIsStoredAndAlertsTheStaffOfThatCountryOnly() throws Exception {
        message("Bonjour, ma voiture fait un bruit.", 201);

        assertEquals(1, rows("select count(*) from support_messages where body like 'Bonjour%'"));
        assertEquals(1, rows("select count(*) from notifications n join users u on u.id = n.user_id where n.type = 'DRIVER_MESSAGE' and u.email = ?", manager.getEmail()));
        assertEquals(0, rows("select count(*) from notifications n join users u on u.id = n.user_id where n.type = 'DRIVER_MESSAGE' and u.email = ?", maliManager.getEmail()));
        assertEquals(0, rows("select count(*) from notifications n join users u on u.id = n.user_id where n.type = 'DRIVER_MESSAGE' and u.email = ?", viewer.getEmail()));
    }

    @Test
    void aMessageMustHaveATextAndSpamIsLimited() throws Exception {
        message("   ", 400);
        message("x".repeat(1001), 400);
        for (int i = 0; i < 5; i++) {
            message("Message " + i, 201);
        }
        mvc.perform(post("/api/v1/driver/support/messages").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"encore\"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("TOO_MANY_MESSAGES"));
    }

    @Test
    void staffTokensAndFirstLoginsAreRefused() throws Exception {
        for (String path : new String[] {"/api/v1/driver/contract", "/api/v1/driver/documents", "/api/v1/driver/support"}) {
            mvc.perform(get(path).header("Authorization", bearer(manager))).andExpect(status().isUnauthorized());
        }
        jdbc.update("update drivers set must_change_password = true");
        String first = accessToken();
        asDriver("/api/v1/driver/documents", first).andExpect(status().isForbidden());
        asDriver("/api/v1/driver/support", first).andExpect(status().isForbidden());
    }

    @Test
    void theDriverCanFetchOwnPhotoButNobodyElsesAndStaffTokensCannot() throws Exception {
        asDriver("/api/v1/driver/me/photo", token).andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(PNG));

        mvc.perform(get("/api/v1/driver/me/photo").header("Authorization", bearer(manager))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/driver/me/photo")).andExpect(status().isUnauthorized());

        jdbc.update("update drivers set photo_file = null");
        asDriver("/api/v1/driver/me/photo", token).andExpect(status().isNotFound());
    }
}
