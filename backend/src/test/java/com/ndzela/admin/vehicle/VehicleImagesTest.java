package com.ndzela.admin.vehicle;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.user.domain.User;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VehicleImagesTest extends AbstractDriverTest {
    private ResultActions upload(String vehicleId, int count, User as) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/vehicles/" + vehicleId + "/images").header("Authorization", bearer(as));
        for (int i = 0; i < count; i++) {
            request.file(new MockMultipartFile("files", "photo" + i + ".png", "image/png", PNG));
        }
        return mvc.perform(request);
    }

    @Test
    void aVehicleCanHaveSeveralPhotosViewedAndDeleted() throws Exception {
        String vehicleId = newVehicle(manager);

        String body = upload(vehicleId, 2, manager).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(2, ((java.util.List<?>) JsonPath.read(body, "$")).size());
        send(get("/api/v1/vehicles/" + vehicleId + "/images"), manager).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        String imageId = JsonPath.read(body, "$[0].id");
        send(get("/api/v1/vehicles/" + vehicleId + "/images/" + imageId), manager).andExpect(status().isOk()).andExpect(content().contentType("image/png"));

        send(delete("/api/v1/vehicles/" + vehicleId + "/images/" + imageId), manager).andExpect(status().isNoContent());
        send(get("/api/v1/vehicles/" + vehicleId + "/images"), manager).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        send(get("/api/v1/vehicles/" + vehicleId + "/images/" + imageId), manager).andExpect(status().isNotFound());
    }

    @Test
    void aVehicleCannotHaveMoreThanEightPhotos() throws Exception {
        String vehicleId = newVehicle(manager);
        upload(vehicleId, 8, manager).andExpect(status().isOk());
        upload(vehicleId, 1, manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("TOO_MANY_IMAGES"));
    }

    @Test
    void imagesNeedThePermissionAndStayInsideTheCountry() throws Exception {
        String vehicleId = newVehicle(manager);
        upload(vehicleId, 1, manager).andExpect(status().isOk());
        upload(vehicleId, 1, viewer).andExpect(status().isForbidden());
        send(get("/api/v1/vehicles/" + vehicleId + "/images"), maliManager).andExpect(status().isNotFound());
        upload(vehicleId, 1, maliManager).andExpect(status().isNotFound());
    }
}
