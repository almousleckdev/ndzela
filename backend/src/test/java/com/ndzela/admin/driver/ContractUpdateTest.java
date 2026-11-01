package com.ndzela.admin.driver;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Template availability, country scoping and updating an active contract
class ContractUpdateTest extends AbstractContractTest {
    @Test
    void aCountryWithoutATemplateCannotCreateDrivers() throws Exception {
        jdbc.update("update countries set contract_prefix = null, contract_template = null where code = 'ML'");
        createDriver(maliManager, DriverJson.create(newVehicle(maliManager), newCenter(maliManager, "Centre Mali"))).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CONTRACT_TEMPLATE_UNAVAILABLE"));
        assertEquals(0, rows("select count(*) from drivers"));
    }

    @Test
    void contractsAreScopedToTheirCountryAndPermissions() throws Exception {
        String driver = id(createDriver(manager, DriverJson.create(newVehicle(manager), center)).andExpect(status().isCreated()));
        String contractId = com.jayway.jsonpath.JsonPath.read(send(get("/api/v1/contracts?driverId=" + driver), manager).andReturn().getResponse().getContentAsString(), "$[0].id");

        send(get("/api/v1/contracts/" + contractId), maliManager).andExpect(status().isNotFound());
        send(get("/api/v1/contracts/" + contractId + "/pdf"), maliManager).andExpect(status().isNotFound());
        send(get("/api/v1/contracts?driverId=" + driver), maliManager).andExpect(jsonPath("$.length()").value(0));
        send(get("/api/v1/contracts/" + contractId), viewer).andExpect(status().isOk());
        send(get("/api/v1/contracts/" + contractId + "/pdf"), viewer).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/contracts/" + contractId)).andExpect(status().isUnauthorized());
    }

    @Test
    void theActiveContractCanBeUpdatedAfterTheDriverIsEdited() throws Exception {
        DriverJson body = DriverJson.create(newVehicle(manager), center).personal("city", "Libreville");
        String driver = id(createDriver(manager, body).andExpect(status().isCreated()));
        String contractId = com.jayway.jsonpath.JsonPath.read(send(get("/api/v1/contracts?driverId=" + driver), manager).andReturn().getResponse().getContentAsString(), "$[0].id");
        String number = jdbc.queryForObject("select contract_number from contracts", String.class);
        String endDate = jdbc.queryForObject("select end_date::text from contracts", String.class);

        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/drivers/" + driver).contentType("application/json")
                .content(body.personal("city", "Port-Gentil").personal("address", "Nouvelle adresse").buildUpdate()), manager).andExpect(status().isOk());
        assertTrue(jdbc.queryForObject("select snapshot from contracts", String.class).contains("\"city\":\"Libreville\""), "editing the driver alone leaves the signed contract as it is");

        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder put = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/drivers/" + driver + "/contract");
        send(put.contentType("application/json").content("{\"deposit\":300000}"), manager).andExpect(status().isOk()).andExpect(jsonPath("$.deposit").value(300000))
                .andExpect(jsonPath("$.revision").value(1)).andExpect(jsonPath("$.contractNumber").value(number)).andExpect(jsonPath("$.endDate").value(endDate));
        String snapshot = jdbc.queryForObject("select snapshot from contracts", String.class);
        assertTrue(snapshot.contains("Port-Gentil") && snapshot.contains("Nouvelle adresse") && snapshot.contains("GA-LBV-01-02-2024-B12-00707"), "the printed parties follow the driver, the bailleur stays as signed");
        assertEquals(1, auditRows("CONTRACT_UPDATED"));

        byte[] pdf = send(get("/api/v1/contracts/" + contractId + "/pdf"), manager).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        PdfReader reader = new PdfReader(pdf);
        String text = new PdfTextExtractor(reader).getTextFromPage(1).replaceAll("[\\s\\u00A0]", "");
        assertTrue(text.contains("révision1") && text.contains("Port-Gentil"), text);
        send(get("/api/v1/contracts/" + contractId), viewer).andExpect(jsonPath("$.revision").value(1));
    }

    @Test
    void updatingAContractNeedsThePermissionAValidDepositAndAnActiveContract() throws Exception {
        String driver = id(createDriver(manager, DriverJson.create(newVehicle(manager), center)).andExpect(status().isCreated()));
        String url = "/api/v1/drivers/" + driver + "/contract";
        var put = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url).contentType("application/json");
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url).contentType("application/json").content("{\"deposit\":1000}"), viewer).andExpect(status().isForbidden());
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url).contentType("application/json").content("{\"deposit\":1000}"), maliManager).andExpect(status().isNotFound());
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url).contentType("application/json").content("{\"deposit\":0}"), manager).andExpect(status().isBadRequest());
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url).contentType("application/json").content("{\"deposit\":1000.5}"), manager).andExpect(status().isBadRequest());
        send(put.content("{}"), manager).andExpect(status().isBadRequest());
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url).contentType("application/json").content("{\"deposit\":1000}"), admin).andExpect(status().isOk());
        assertEquals(1000, rows("select deposit::int from contracts"));

        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager).andExpect(status().isOk());
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url).contentType("application/json").content("{\"deposit\":2000}"), manager).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NO_ACTIVE_CONTRACT"));
    }
}
