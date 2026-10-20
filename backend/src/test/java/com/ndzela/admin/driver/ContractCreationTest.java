package com.ndzela.admin.driver;

import com.ndzela.admin.contract.domain.BillingCalendar;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Opening a contract with the driver: numbers, vehicle changes, database guards and the terms preview
class ContractCreationTest extends AbstractContractTest {
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
}
