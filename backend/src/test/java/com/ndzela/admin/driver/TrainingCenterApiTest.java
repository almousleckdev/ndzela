package com.ndzela.admin.driver;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TrainingCenterApiTest extends AbstractDriverTest {
    @Test
    void managesCentersAndAuditsEachChange() throws Exception {
        String id = newCenter(manager, "NDZELA");

        send(put("/api/v1/training-centers/" + id).contentType("application/json").content("{\"name\":\"Centre Nord\"}"), manager)
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Centre Nord"));
        send(get("/api/v1/training-centers"), manager).andExpect(jsonPath("$[*].name").value(contains("Centre Nord")));
        send(delete("/api/v1/training-centers/" + id), manager).andExpect(status().isNoContent());

        assertEquals(1, auditRows("TRAINING_CENTER_CREATED"));
        assertEquals(1, auditRows("TRAINING_CENTER_UPDATED"));
        assertEquals(1, auditRows("TRAINING_CENTER_DELETED"));
    }

    @Test
    void theNameIsUniquePerCountry() throws Exception {
        newCenter(manager, "NDZELA");

        postJson("/api/v1/training-centers", "{\"name\":\" ndzela \"}", manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("TRAINING_CENTER_NAME_TAKEN"));
        newCenter(maliManager, "NDZELA");
        postJson("/api/v1/training-centers", "{\"name\":\"\"}", manager).andExpect(status().isBadRequest());
    }

    @Test
    void theDatabaseTreatsNamesCaseInsensitively() throws Exception {
        newCenter(manager, "NDZELA");
        String country = jdbc.queryForObject("select country_id::text from training_centers", String.class);

        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                "insert into training_centers (id, country_id, name) values (gen_random_uuid(), ?::uuid, 'ndzela')", country));
    }

    @Test
    void aCenterUsedByADriverCannotBeDeleted() throws Exception {
        String center = newCenter(manager, "NDZELA");
        createDriver(manager, DriverJson.create(newVehicle(manager), center)).andExpect(status().isCreated());

        send(delete("/api/v1/training-centers/" + center), manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("TRAINING_CENTER_IN_USE"));
    }

    @Test
    void permissionsAndCountryIsolation() throws Exception {
        String id = newCenter(manager, "NDZELA");

        send(get("/api/v1/training-centers"), viewer).andExpect(status().isOk());
        postJson("/api/v1/training-centers", "{\"name\":\"X\"}", viewer).andExpect(status().isForbidden());
        postJson("/api/v1/training-centers", "{\"name\":\"X\"}", admin).andExpect(status().isForbidden());
        send(get("/api/v1/training-centers"), maliManager).andExpect(jsonPath("$.length()").value(0));
        send(put("/api/v1/training-centers/" + id).contentType("application/json").content("{\"name\":\"Y\"}"), maliManager).andExpect(status().isNotFound());
        send(delete("/api/v1/training-centers/" + id), maliManager).andExpect(status().isNotFound());
        postJson("/api/v1/training-centers", "{\"name\":\"X\"}", root).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("COUNTRY_REQUIRED"));
    }
}
