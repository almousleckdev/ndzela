package com.ndzela.admin.charging;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Recording a recharge: validation, permissions, concurrency, cancellation and the database guards
class ChargingRecordingTest extends AbstractChargingApiTest {
    @Test
    void aRechargeOfAnNdzelaCarFillsInTheDriverAndShowsTheBatteryAfter() throws Exception {
        charge(manager, 30, 90).andExpect(status().isCreated()).andExpect(jsonPath("$.driverId").value(driver)).andExpect(jsonPath("$.driverName").exists())
                .andExpect(jsonPath("$.batteryBefore").value(30)).andExpect(jsonPath("$.batteryAfter").value(90)).andExpect(jsonPath("$.station").value("Station Akanda"))
                .andExpect(jsonPath("$.recordedBy").value(manager.getEmail())).andExpect(jsonPath("$.critical").value(false)).andExpect(jsonPath("$.amount").value(5000));
        assertEquals(1, auditRows("CHARGING_RECORDED"));
        send(get("/api/v1/charging/recharges"), viewer).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].vehicle").exists());
    }

    @Test
    void everyRechargeIsPaidFromTheZellaWalletWhateverMethodIsSent() throws Exception {
        charge(manager, 30, 90).andExpect(status().isCreated()).andExpect(jsonPath("$.method").value("ZELLA_WALLET"));
        postJson("/api/v1/charging/recharges", recharge(own(), 1000, "CASH", "R9", 10, 60, UUID.randomUUID()), manager).andExpect(status().isCreated()).andExpect(jsonPath("$.method").value("ZELLA_WALLET"));
        assertEquals(2, rows("select count(*) from charging_entries where method = 'ZELLA_WALLET'"));
    }

    @Test
    void aPublicCarIsRecordedWithoutADriver() throws Exception {
        postJson("/api/v1/charging/recharges", recharge("\"publicLabel\":\"GA-123-AB Toyota\"", 3000, "AIRTEL_MONEY", "AM-1", 10, 60, UUID.randomUUID()), manager)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.driverId").doesNotExist()).andExpect(jsonPath("$.vehicle").value("GA-123-AB Toyota")).andExpect(jsonPath("$.critical").value(true));
    }

    @Test
    void theVehicleOrThePublicCarMustBeGivenExactlyOnce() throws Exception {
        postJson("/api/v1/charging/recharges", recharge(own() + ",\"publicLabel\":\"X\"", 3000, "AIRTEL_MONEY", "R1", 10, 60, UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CHARGING_TARGET_INVALID"));
        postJson("/api/v1/charging/recharges", recharge("\"publicLabel\":\"  \"", 3000, "AIRTEL_MONEY", "R1", 10, 60, UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity());
        String spare = newVehicle(manager);
        postJson("/api/v1/charging/recharges", recharge("\"vehicleId\":\"" + spare + "\"", 3000, "AIRTEL_MONEY", "R1", 10, 60, UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VEHICLE_HAS_NO_DRIVER"));
        assertEquals(0, rows("select count(*) from charging_entries"));
    }

    @Test
    void batteryLevelsAmountsAndReferencesAreValidated() throws Exception {
        charge(manager, 60, 50).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("BATTERY_LEVELS_INVALID"));
        charge(manager, -1, 50).andExpect(status().isBadRequest());
        charge(manager, 10, 101).andExpect(status().isBadRequest());
        postJson("/api/v1/charging/recharges", recharge(own(), 0, "AIRTEL_MONEY", "R1", 10, 60, UUID.randomUUID()), manager).andExpect(status().isBadRequest());
        postJson("/api/v1/charging/recharges", recharge(own(), 1000, "MOOV_MONEY", null, 10, 60, UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYMENT_REFERENCE_REQUIRED"));
        postJson("/api/v1/charging/recharges", recharge(own(), 1000, "BITCOIN", null, 10, 60, UUID.randomUUID()), manager).andExpect(status().isBadRequest());
        postJson("/api/v1/charging/recharges", "{}", manager).andExpect(status().isBadRequest());
        assertEquals(0, rows("select count(*) from charging_entries"));
    }

    @Test
    void onlyHoldersOfTheCreatePermissionRecord() throws Exception {
        charge(viewer, 10, 50).andExpect(status().isForbidden());
        charge(admin, 10, 50).andExpect(status().isForbidden());
        charge(accountant, 10, 50).andExpect(status().isForbidden());
        charge(cashier, 10, 50).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/charging/recharges")).andExpect(status().isUnauthorized());
    }

    @Test
    void sameRequestRecordsOnceEvenWhenSimultaneous() throws Exception {
        UUID same = UUID.randomUUID();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (UUID request : List.of(same, same, same, UUID.randomUUID())) {
            Callable<Integer> call = () -> {
                start.await();
                return postJson("/api/v1/charging/recharges", recharge(own(), 2000, "AIRTEL_MONEY", "R1", 40, 80, request), manager).andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(call));
        }
        start.countDown();
        for (Future<Integer> result : results) {
            assertEquals(201, result.get());
        }
        pool.shutdown();
        assertEquals(2, rows("select count(*) from charging_entries"));
    }

    @Test
    void aRechargeIsCancelledByAnAdministratorWithAReasonAndOnlyOnce() throws Exception {
        String id = entryId(charge(manager, 20, 80).andExpect(status().isCreated()));
        postJson("/api/v1/charging/recharges/" + id + "/reverse", REASON, manager).andExpect(status().isForbidden());
        postJson("/api/v1/charging/recharges/" + id + "/reverse", "{}", admin).andExpect(status().isBadRequest());
        String reversal = entryId(postJson("/api/v1/charging/recharges/" + id + "/reverse", "{\"reason\":\"Montant erroné\"}", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("REVERSAL")).andExpect(jsonPath("$.reason").value("Montant erroné")));
        postJson("/api/v1/charging/recharges/" + id + "/reverse", REASON, admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("ENTRY_ALREADY_REVERSED"));
        postJson("/api/v1/charging/recharges/" + reversal + "/reverse", REASON, admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("ENTRY_NOT_REVERSIBLE"));
        send(get("/api/v1/charging/recharges"), viewer).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].reversed").value(true));
        assertEquals(1, auditRows("CHARGING_REVERSED"));
    }

    @Test
    void theRecordsRefuseUpdatesAndDeletesAtTheDatabase() throws Exception {
        charge(manager, 20, 80).andExpect(status().isCreated());
        assertThrows(DataAccessException.class, () -> jdbc.update("update charging_entries set amount = 1"));
        assertThrows(DataAccessException.class, () -> jdbc.update("delete from charging_entries"));
        assertThrows(DataAccessException.class, () -> jdbc.update("insert into charging_entries (id, country_id, entry_type, station_id, amount, recorded_by, created_at, battery_before, battery_after) "
                + "select gen_random_uuid(), country_id, 'RECHARGE', id, 1, 'x', now(), 0, 0 from stations limit 1"), "a record needs a vehicle or a public car");
    }

    @Test
    void onlyAnAdministratorDeletesARechargeWithAReasonAndItsCancellationGoesWithIt() throws Exception {
        String id = entryId(charge(manager, 20, 80).andExpect(status().isCreated()));
        postJson("/api/v1/charging/recharges/" + id + "/reverse", REASON, admin).andExpect(status().isOk());

        mvc.perform(delete("/api/v1/charging/recharges/" + id).header("Authorization", bearer(manager)).contentType("application/json").content(REASON)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/charging/recharges/" + id).header("Authorization", bearer(admin)).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
        assertEquals(2, rows("select count(*) from charging_entries"));

        mvc.perform(delete("/api/v1/charging/recharges/" + id).header("Authorization", bearer(admin)).contentType("application/json").content(REASON)).andExpect(status().isNoContent());

        assertEquals(0, rows("select count(*) from charging_entries"));
        assertEquals(1, auditRows("CHARGING_RECHARGE_DELETED"));
    }

    @Test
    void theCancellationOfARechargeCannotBeDeletedOnItsOwn() throws Exception {
        String id = entryId(charge(manager, 20, 80).andExpect(status().isCreated()));
        String reversal = entryId(postJson("/api/v1/charging/recharges/" + id + "/reverse", REASON, admin).andExpect(status().isOk()));

        mvc.perform(delete("/api/v1/charging/recharges/" + reversal).header("Authorization", bearer(admin)).contentType("application/json").content(REASON))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("ENTRY_NOT_A_RECHARGE"));
        assertEquals(2, rows("select count(*) from charging_entries"));
    }
}
