package com.ndzela.admin.payment;

import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The ledger after a payment: reversals, waivers, history, exports and country isolation
class PaymentLedgerTest extends AbstractPaymentApiTest {
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
