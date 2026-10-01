package com.ndzela.admin.driver;

import com.ndzela.admin.contract.domain.BillingCalendar;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContractTest extends AbstractDriverTest {
    private String center;

    @BeforeEach
    void setup() throws Exception {
        center = newCenter(manager, "NDZELA");
    }

    @Test
    void theEndDateIsTheThousandTwoHundredAndFortyEighthBillableDay() {
        assertEquals(LocalDate.of(2030, 9, 25), BillingCalendar.endDate(LocalDate.of(2026, 9, 30), 1248));
        assertEquals(LocalDate.of(2026, 10, 1), BillingCalendar.endDate(LocalDate.of(2026, 9, 30), 1));
        assertEquals(LocalDate.of(2026, 10, 5), BillingCalendar.endDate(LocalDate.of(2026, 10, 3), 1), "a Saturday start: Sunday is skipped");
        assertEquals(LocalDate.of(2026, 10, 6), BillingCalendar.endDate(LocalDate.of(2026, 10, 3), 2));
    }

    @Test
    void creatingADriverOpensAnActiveContractWithTheCarsRent() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = id(createDriver(manager, DriverJson.create(vehicle, center)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.contract.contractNumber").value("NDZ-00000001")).andExpect(jsonPath("$.contract.status").value("ACTIVE"))
                .andExpect(jsonPath("$.contract.billableDays").value(1248)).andExpect(jsonPath("$.contract.deposit").value(250000))
                .andExpect(jsonPath("$.contract.dailyRent").value(23000)).andExpect(jsonPath("$.contract.totalPrice").value(28704000)));

        LocalDate today = LocalDate.now(ZoneId.of("Africa/Libreville"));
        send(get("/api/v1/contracts?driverId=" + driver), viewer).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].startDate").value(today.toString())).andExpect(jsonPath("$[0].endDate").value(BillingCalendar.endDate(today, 1248).toString()));
        assertEquals(1, auditRows("CONTRACT_CREATED"));
    }

    @Test
    void numbersAreSequentialPerCountry() throws Exception {
        createDriver(manager, DriverJson.create(newVehicle(manager), center)).andExpect(status().isCreated()).andExpect(jsonPath("$.contract.contractNumber").value("NDZ-00000001"));
        createDriver(manager, DriverJson.create(newVehicle(manager), center)).andExpect(status().isCreated()).andExpect(jsonPath("$.contract.contractNumber").value("NDZ-00000002"));
        createDriver(maliManager, DriverJson.create(newVehicle(maliManager), newCenter(maliManager, "Centre Mali"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.contract.contractNumber").value("MLI-00000001"));
    }

    @Test
    void simultaneousCreationsNeverShareANumber() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            DriverJson body = DriverJson.create(newVehicle(manager), center);
            Callable<Integer> call = () -> {
                start.await();
                return createDriver(manager, body).andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(call));
        }
        start.countDown();
        for (Future<Integer> result : results) {
            assertEquals(201, result.get());
        }
        pool.shutdown();
        assertEquals(4, rows("select count(distinct contract_number) from contracts"));
        assertEquals(4L, jdbc.queryForObject("select max(last_value) from contract_counters where last_value > 0", Long.class));
    }

    @Test
    void removingTheVehicleEndsTheContractAndAssigningOpensANewOne() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = id(createDriver(manager, DriverJson.create(vehicle, center)).andExpect(status().isCreated()));

        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", "{\"reason\":\"Fin de contrat\"}", manager).andExpect(status().isOk()).andExpect(jsonPath("$.contract").doesNotExist());
        assertEquals("TERMINATED", jdbc.queryForObject("select status from contracts", String.class));
        assertEquals("Fin de contrat", jdbc.queryForObject("select end_reason from contracts", String.class));
        assertEquals(1, auditRows("CONTRACT_TERMINATED"));

        postJson("/api/v1/drivers/" + driver + "/vehicle", "{\"vehicleId\":\"" + vehicle + "\",\"deposit\":300000}", manager).andExpect(status().isOk())
                .andExpect(jsonPath("$.contract.contractNumber").value("NDZ-00000002")).andExpect(jsonPath("$.contract.deposit").value(300000));
        postJson("/api/v1/drivers/" + driver + "/vehicle", "{\"vehicleId\":\"" + vehicle + "\"}", manager).andExpect(status().isBadRequest());
        assertEquals(2, rows("select count(*) from contracts"));
        assertEquals(1, rows("select count(*) from contracts where status = 'ACTIVE'"));
    }

    @Test
    void theDatabaseAllowsOneActiveContractPerDriverAndPerVehicle() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = id(createDriver(manager, DriverJson.create(vehicle, center)).andExpect(status().isCreated()));
        String other = id(createDriver(manager, DriverJson.create(newVehicle(manager), center)).andExpect(status().isCreated()));

        String insert = "insert into contracts (id, country_id, contract_number, driver_id, vehicle_id, status, template_version, start_date, end_date, billable_days, daily_rent, deposit, total_price, snapshot, created_by) "
                + "select gen_random_uuid(), country_id, ?, ?::uuid, ?::uuid, 'ACTIVE', 'GA-1', current_date, current_date + 1, 1, 1, 1, 1, '{}', 'x' from contracts limit 1";
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(insert, "X-1", driver, jdbc.queryForObject("select id::text from vehicles where id <> ?::uuid limit 1", String.class, vehicle)));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(insert, "X-2", other, vehicle));
    }

    @Test
    void theTermsArePreviewedWithoutCreatingAnything() throws Exception {
        String vehicle = newVehicle(manager);
        postJson("/api/v1/contracts/preview", "{\"vehicleId\":\"" + vehicle + "\",\"deposit\":250000}", manager).andExpect(status().isOk())
                .andExpect(jsonPath("$.billableDays").value(1248)).andExpect(jsonPath("$.totalPrice").value(28704000)).andExpect(jsonPath("$.dailyRent").value(23000));
        postJson("/api/v1/contracts/preview", "{\"vehicleId\":\"" + vehicle + "\",\"deposit\":0}", manager).andExpect(status().isBadRequest());
        postJson("/api/v1/contracts/preview", "{\"vehicleId\":\"" + vehicle + "\",\"deposit\":250000}", viewer).andExpect(status().isForbidden());
        postJson("/api/v1/contracts/preview", "{\"vehicleId\":\"" + vehicle + "\",\"deposit\":250000}", root).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("COUNTRY_REQUIRED"));
        assertEquals(0, rows("select count(*) from contracts"));
    }

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
        for (String expected : new String[]{"CONTRAT DE LOCATION DE VÉHICULE", "NDZ-00000001", "ATALIB AG", "ALMOUSLECK", "ARTICLE 17 : ANNEXES", "250 000 FCFA", "23 000 FCFA", "1 248 jours",
                "GA-LBV-01-02-2024-B12-00707", "Non renseigné", "À compléter", "MENTION MANUSCRITE OBLIGATOIRE DU LOCATAIRE", "Du LUNDI au SAMEDI inclus"}) {
            assertTrue(all.contains(expected.replaceAll("[\\s\\u00A0]", "")), "missing in the PDF: " + expected);
        }
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
