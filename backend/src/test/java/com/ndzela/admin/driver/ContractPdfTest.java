package com.ndzela.admin.driver;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The printed contract: the signed facts, the audit, Malian branding and editing after signing
class ContractPdfTest extends AbstractContractTest {
    @Test
    void theContractPdfContainsTheSignedFactsAndIsAudited() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = id(createDriver(manager, DriverJson.create(vehicle, center).personal("lastName", "ATALIB AG").personal("firstName", "Almousleck")).andExpect(status().isCreated()));
        String contractId = com.jayway.jsonpath.JsonPath.read(send(get("/api/v1/contracts?driverId=" + driver), manager).andReturn().getResponse().getContentAsString(), "$[0].id");

        byte[] pdf = send(get("/api/v1/contracts/" + contractId + "/pdf"), manager).andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        PdfReader reader = new PdfReader(pdf);
        StringBuilder text = new StringBuilder();
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        String all = text.toString().replaceAll("[\\s\\u00A0]", "");
        assertTrue(reader.getNumberOfPages() >= 6, "a full contract, not a summary: " + reader.getNumberOfPages() + " pages");
        java.nio.file.Files.write(java.nio.file.Path.of("target/contract-sample.pdf"), pdf);
        for (String expected : new String[]{"CONTRAT DE LOCATION DE VÉHICULE", "NDZ-00000001", "ATALIB AG", "ALMOUSLECK", "ARTICLE 18 : ANNEXES", "250 000 FCFA", "23 000 FCFA", "1 248 jours",
                "GA-LBV-01-02-2024-B12-00707", "Ndzela SARL", "ndzelagabon@gmail.com", "Non renseigné", "À compléter", "MENTION MANUSCRITE OBLIGATOIRE DU LOCATAIRE", "Du LUNDI au SAMEDI inclus"}) {
            assertTrue(all.contains(expected.replaceAll("[\\s\\u00A0]", "")), "missing in the PDF: " + expected);
        }
        assertTrue(!all.contains("NDZELA"), "the company is written Ndzela");
        assertEquals(1, auditRows("CONTRACT_PRINTED"));
    }

    @Test
    void aSignedContractKeepsItsDataWhenTheDriverIsEditedLater() throws Exception {
        DriverJson body = DriverJson.create(newVehicle(manager), center).personal("city", "Libreville");
        String driver = id(createDriver(manager, body).andExpect(status().isCreated()));
        String contractId = com.jayway.jsonpath.JsonPath.read(send(get("/api/v1/contracts?driverId=" + driver), manager).andReturn().getResponse().getContentAsString(), "$[0].id");

        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/drivers/" + driver).contentType("application/json")
                .content(body.personal("city", "Port-Gentil").personal("address", "Nouvelle adresse").buildUpdate()), manager).andExpect(status().isOk());
        assertTrue(jdbc.queryForObject("select snapshot from contracts", String.class).contains("\"city\":\"Libreville\""));
        assertTrue(!jdbc.queryForObject("select snapshot from contracts", String.class).contains("Port-Gentil"));
        send(get("/api/v1/contracts/" + contractId + "/pdf"), manager).andExpect(status().isOk());
    }

    @Test
    void theMaliContractCarriesTheAngataBrandAndNoGabonNames() throws Exception {
        jdbc.update("update countries set contract_template = 'ML-1', legal_name = 'ANGATA' where code = 'ML'");
        String driver = id(createDriver(maliManager, DriverJson.create(newVehicle(maliManager), newCenter(maliManager, "Centre Mali"))).andExpect(status().isCreated()));
        String contractId = com.jayway.jsonpath.JsonPath.read(send(get("/api/v1/contracts?driverId=" + driver), maliManager).andReturn().getResponse().getContentAsString(), "$[0].id");

        byte[] pdf = send(get("/api/v1/contracts/" + contractId + "/pdf"), maliManager).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        PdfReader reader = new PdfReader(pdf);
        StringBuilder text = new StringBuilder();
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        String all = text.toString().replaceAll("\\s+", " ");
        assertTrue(all.contains("ANGATA SARL"), "the company is Angata");
        assertTrue(all.contains("droit malien") && all.contains("tribunal de commerce de Bamako"), "the Malian law and court");
        for (String left : new String[]{"NDZELA", "Libreville", "gabonais", "gabonaise", "au Gabon"}) {
            assertTrue(!all.contains(left), "Gabon wording left: " + left);
        }
        assertTrue(all.contains("mobilité électrique au Mali"), "the preamble names Mali");
    }
}
