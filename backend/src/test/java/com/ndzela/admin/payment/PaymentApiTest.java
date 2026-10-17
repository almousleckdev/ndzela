package com.ndzela.admin.payment;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.test.web.servlet.ResultActions;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentApiTest extends AbstractDriverTest {
    protected User cashier;
    protected User accountant;
    protected String driver;
    protected String center;

    @BeforeEach
    void paymentSetup() throws Exception {
        cashier = staff("ga.cashier@ndzela.test", "GA", "CAISSIER");
        accountant = staff("ga.accountant@ndzela.test", "GA", "COMPTABLE");
        center = newCenter(manager, "NDZELA");
        driver = id(createDriver(manager, DriverJson.create(newVehicle(manager), center)).andExpect(status().isCreated()));
    }

    protected String payment(String driverId, Object amount, String method, String reference, UUID requestId) {
        return "{\"driverId\":\"" + driverId + "\",\"amount\":" + amount + ",\"method\":\"" + method + "\"" + (reference == null ? "" : ",\"reference\":\"" + reference + "\"")
                + ",\"requestId\":\"" + requestId + "\"}";
    }

    protected ResultActions pay(User as, Object amount) throws Exception {
        return postJson("/api/v1/payments", payment(driver, amount, "AIRTEL_MONEY", "REF-" + UUID.randomUUID(), UUID.randomUUID()), as);
    }

    protected String entryId(ResultActions result) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.entry.id");
    }

    @Test
    void aPaymentIsRecordedAndShownInTheAccountAndTheHistory() throws Exception {
        pay(manager, 23000).andExpect(status().isCreated()).andExpect(jsonPath("$.entry.type").value("PAYMENT")).andExpect(jsonPath("$.entry.amount").value(23000))
                .andExpect(jsonPath("$.entry.recordedBy").value(manager.getEmail())).andExpect(jsonPath("$.level").value("NONE")).andExpect(jsonPath("$.credit").value(23000));

        send(get("/api/v1/payments/history"), viewer).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].driverName").exists());
        send(get("/api/v1/payments/accounts/" + driver), viewer).andExpect(status().isOk()).andExpect(jsonPath("$.credit").value(23000)).andExpect(jsonPath("$.entries.length()").value(1));
        assertEquals(1, auditRows("PAYMENT_RECORDED"));
        send(get("/api/v1/payments/summary"), viewer).andExpect(jsonPath("$.collectedToday").value(23000));
    }

    @Test
    void onlyHoldersOfTheCreatePermissionCanRecord() throws Exception {
        pay(viewer, 1000).andExpect(status().isForbidden());
        pay(admin, 1000).andExpect(status().isForbidden());
        pay(accountant, 1000).andExpect(status().isForbidden());
        pay(cashier, 1000).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/payments/history")).andExpect(status().isUnauthorized());
    }

    @Test
    void amountsMethodsAndReferencesAreValidated() throws Exception {
        pay(manager, 0).andExpect(status().isBadRequest());
        pay(manager, -5).andExpect(status().isBadRequest());
        pay(manager, "12.5").andExpect(status().isBadRequest());
        postJson("/api/v1/payments", payment(driver, 1000, "BITCOIN", null, UUID.randomUUID()), manager).andExpect(status().isBadRequest());
        postJson("/api/v1/payments", "{\"driverId\":\"" + driver + "\",\"amount\":1000,\"method\":\"CASH\"}", manager).andExpect(status().isBadRequest());
        postJson("/api/v1/payments", payment(driver, 1000, "AIRTEL_MONEY", null, UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYMENT_REFERENCE_REQUIRED"));
        postJson("/api/v1/payments", payment(driver, 1000, "AIRTEL_MONEY", "AM-123", UUID.randomUUID()), manager).andExpect(status().isCreated());
        assertEquals(1, rows("select count(*) from ledger_entries"));
    }

    @Test
    void rentIsPaidOnlyWithAirtelOrMoovAndAlwaysWithAReference() throws Exception {
        for (String method : new String[] {"CASH", "OTHER"}) {
            postJson("/api/v1/payments", payment(driver, 1000, method, "R-1", UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PAYMENT_METHOD_NOT_ALLOWED"));
        }
        postJson("/api/v1/payments", payment(driver, 1000, "MOOV_MONEY", "  ", UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYMENT_REFERENCE_REQUIRED"));
        assertEquals(0, rows("select count(*) from ledger_entries"));
    }

    @Test
    void sendingTheSameRequestTwiceRecordsOneEntry() throws Exception {
        UUID request = UUID.randomUUID();
        String first = entryId(postJson("/api/v1/payments", payment(driver, 5000, "AIRTEL_MONEY", "REF-" + request, request), manager).andExpect(status().isCreated()));
        String second = entryId(postJson("/api/v1/payments", payment(driver, 5000, "AIRTEL_MONEY", "REF-" + request, request), manager).andExpect(status().isCreated()));
        assertEquals(first, second);
        assertEquals(1, rows("select count(*) from ledger_entries"));
    }

    @Test
    void simultaneousIdenticalRequestsRecordOneEntryAndDifferentOnesRecordBoth() throws Exception {
        UUID same = UUID.randomUUID();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        List<UUID> requests = List.of(same, same, UUID.randomUUID(), UUID.randomUUID());
        for (UUID request : requests) {
            Callable<Integer> call = () -> {
                start.await();
                return postJson("/api/v1/payments", payment(driver, 1000, "AIRTEL_MONEY", "REF-" + request, request), manager).andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(call));
        }
        start.countDown();
        for (Future<Integer> result : results) {
            assertEquals(201, result.get());
        }
        pool.shutdown();
        assertEquals(3, rows("select count(*) from ledger_entries"));
    }

    @Test
    void aTransactionReferenceIsRecordedOnceUntilItsPaymentIsReversed() throws Exception {
        String entry = entryId(postJson("/api/v1/payments", payment(driver, 5000, "AIRTEL_MONEY", "AM-777", UUID.randomUUID()), manager).andExpect(status().isCreated()));

        postJson("/api/v1/payments", payment(driver, 5000, "AIRTEL_MONEY", "AM-777", UUID.randomUUID()), manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PAYMENT_REFERENCE_ALREADY_USED"));
        postJson("/api/v1/payments", payment(driver, 5000, "AIRTEL_MONEY", " am-777 ", UUID.randomUUID()), manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PAYMENT_REFERENCE_ALREADY_USED"));
        postJson("/api/v1/payments", payment(driver, 5000, "MOOV_MONEY", "AM-777", UUID.randomUUID()), manager).andExpect(status().isCreated());
        assertEquals(2, rows("select count(*) from ledger_entries"));

        postJson("/api/v1/payments/" + entry + "/reverse", "{\"reason\":\"Mauvais chauffeur\"}", admin).andExpect(status().isOk());
        postJson("/api/v1/payments", payment(driver, 5000, "AIRTEL_MONEY", "AM-777", UUID.randomUUID()), manager).andExpect(status().isCreated());
    }

    @Test
    void simultaneousPaymentsWithTheSameReferenceRecordOnlyOne() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Callable<Integer> call = () -> {
                start.await();
                return postJson("/api/v1/payments", payment(driver, 1000, "AIRTEL_MONEY", "AM-RACE", UUID.randomUUID()), manager).andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(call));
        }
        start.countDown();
        int created = 0;
        for (Future<Integer> result : results) {
            int status = result.get();
            assertTrue(status == 201 || status == 422, "unexpected status " + status);
            created += status == 201 ? 1 : 0;
        }
        pool.shutdown();
        assertEquals(1, created);
        assertEquals(1, rows("select count(*) from ledger_entries"));
    }

    @Test
    void aDriverWithoutAnActiveContractCannotPay() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager).andExpect(status().isOk());
        pay(manager, 1000).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("NO_ACTIVE_CONTRACT"));
    }

    @Test
    void aPaymentIsCorrectedByAReversalNeverByAnEdit() throws Exception {
        String entry = entryId(pay(manager, 23000).andExpect(status().isCreated()));

        postJson("/api/v1/payments/" + entry + "/reverse", REASON, manager).andExpect(status().isForbidden());
        postJson("/api/v1/payments/" + entry + "/reverse", "{}", admin).andExpect(status().isBadRequest());
        String reversal = entryId(postJson("/api/v1/payments/" + entry + "/reverse", "{\"reason\":\"Erreur de saisie\"}", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.entry.type").value("REVERSAL")).andExpect(jsonPath("$.credit").value(0)));
        postJson("/api/v1/payments/" + entry + "/reverse", REASON, admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("ENTRY_ALREADY_REVERSED"));
        postJson("/api/v1/payments/" + reversal + "/reverse", REASON, admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("ENTRY_NOT_REVERSIBLE"));

        send(get("/api/v1/payments/history?type=PAYMENT"), viewer).andExpect(jsonPath("$.items[0].reversed").value(true));
        send(get("/api/v1/payments/summary"), viewer).andExpect(jsonPath("$.collectedToday").value(0));
        assertEquals(1, auditRows("PAYMENT_REVERSED"));
    }

    @Test
    void theLedgerRefusesUpdatesAndDeletesAtTheDatabase() throws Exception {
        pay(manager, 1000).andExpect(status().isCreated());
        assertThrows(DataAccessException.class, () -> jdbc.update("update ledger_entries set amount = 1"));
        assertThrows(DataAccessException.class, () -> jdbc.update("delete from ledger_entries"));
        assertEquals(1, rows("select count(*) from ledger_entries"));
    }

    @Test
    void onlyAnAdministratorWaivesRentAndTheReasonIsKept() throws Exception {
        String body = "{\"driverId\":\"" + driver + "\",\"amount\":23000,\"reason\":\"Panne du véhicule\"}";
        postJson("/api/v1/arrears/waivers", body, manager).andExpect(status().isForbidden());
        postJson("/api/v1/arrears/waivers", "{\"driverId\":\"" + driver + "\",\"amount\":23000}", admin).andExpect(status().isBadRequest());
        postJson("/api/v1/arrears/waivers", body, admin).andExpect(status().isCreated()).andExpect(jsonPath("$.entry.type").value("WAIVER")).andExpect(jsonPath("$.entry.reason").value("Panne du véhicule"));
        assertEquals(1, auditRows("ARREARS_WAIVED"));
    }

    @Test
    void theHistoryFiltersAndTheExportFollowThem() throws Exception {
        pay(manager, 1000).andExpect(status().isCreated());
        postJson("/api/v1/payments", payment(driver, 2000, "MOOV_MONEY", "MM-1", UUID.randomUUID()), manager).andExpect(status().isCreated());

        send(get("/api/v1/payments/history?method=MOOV_MONEY"), viewer).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].reference").value("MM-1"));
        send(get("/api/v1/payments/history?type=WAIVER"), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/history?driverId=" + driver), viewer).andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/payments/history?driverId=" + UUID.randomUUID()), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/history?from=2000-01-01&to=2100-01-01"), viewer).andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/payments/history?from=2100-01-01&to=2000-01-01"), viewer).andExpect(status().isUnprocessableEntity());
        send(get("/api/v1/payments/history?method=NOPE"), viewer).andExpect(status().isBadRequest());
        send(get("/api/v1/payments/history?size=500"), viewer).andExpect(jsonPath("$.size").value(100));

        var sheet = com.ndzela.admin.support.ExportSheets.rows(send(get("/api/v1/payments/export?method=MOOV_MONEY"), accountant).andExpect(status().isOk()));
        var data = com.ndzela.admin.support.ExportSheets.data(sheet);
        assertEquals("Date", sheet.get(com.ndzela.admin.support.ExportSheets.HEADER).get(0));
        assertTrue(data.size() == 1 && data.get(0).contains("MM-1") && data.get(0).contains("Moov Money"));
        assertTrue(com.ndzela.admin.support.ExportSheets.isPdf(send(get("/api/v1/payments/export?format=PDF"), accountant).andExpect(status().isOk())));
        send(get("/api/v1/payments/export"), manager).andExpect(status().isForbidden());
        assertEquals(2, auditRows("PAYMENTS_EXPORTED"));
    }

    @Test
    void theFollowUpTableListsDriversWithTheirContract() throws Exception {
        send(get("/api/v1/payments/accounts"), viewer).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].contractNumber").value("NDZ-00000001"))
                .andExpect(jsonPath("$.items[0].level").value("NONE")).andExpect(jsonPath("$.items[0].dailyRent").value(23000));
        send(get("/api/v1/payments/accounts?filter=UP_TO_DATE"), viewer).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/accounts?filter=RED"), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/accounts?filter=LATE"), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/accounts?q=nobody"), viewer).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void eachCountryOffersItsOwnMobileMoneyOperators() throws Exception {
        send(get("/api/v1/payments/operators"), manager).andExpect(jsonPath("$[0].code").value("AIRTEL_MONEY")).andExpect(jsonPath("$[1].code").value("MOOV_MONEY"));
        send(get("/api/v1/payments/operators"), maliManager).andExpect(jsonPath("$[0].code").value("ORANGE_MONEY")).andExpect(jsonPath("$[1].code").value("MOOV_MONEY"));
        postJson("/api/v1/payments", payment(driver, 1000, "ORANGE_MONEY", "REF-" + UUID.randomUUID(), UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYMENT_METHOD_NOT_ALLOWED"));
    }

    @Test
    void anotherCountryNeverSeesOrChangesTheseRecords() throws Exception {
        String entry = entryId(pay(manager, 23000).andExpect(status().isCreated()));
        postJson("/api/v1/payments", payment(driver, 1000, "AIRTEL_MONEY", "REF-" + UUID.randomUUID(), UUID.randomUUID()), maliManager).andExpect(status().isNotFound());
        send(get("/api/v1/payments/accounts/" + driver), maliManager).andExpect(status().isNotFound());
        send(get("/api/v1/payments/history"), maliManager).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/accounts"), maliManager).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/summary"), maliManager).andExpect(jsonPath("$.collectedToday").value(0));
        User maliAdmin = staff("ml.admin@ndzela.test", "ML", "ADMIN");
        postJson("/api/v1/payments/" + entry + "/reverse", REASON, maliAdmin).andExpect(status().isNotFound());
        postJson("/api/v1/arrears/waivers", "{\"driverId\":\"" + driver + "\",\"amount\":1000,\"reason\":\"x\"}", maliAdmin).andExpect(status().isNotFound());

        send(get("/api/v1/payments/history"), root).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/history").header("X-Country", "ML"), root).andExpect(jsonPath("$.total").value(0));
        postJson("/api/v1/payments", payment(driver, 1000, "AIRTEL_MONEY", "REF-" + UUID.randomUUID(), UUID.randomUUID()), root).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("COUNTRY_REQUIRED"));
    }
}
