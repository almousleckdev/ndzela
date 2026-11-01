package com.ndzela.admin.payment;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Recording a payment: validation, idempotence, references and concurrency
class PaymentRecordingTest extends AbstractPaymentApiTest {
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
}
