package com.ndzela.admin.payment;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Staff paying the days they choose, skipping older ones
class PaymentChosenDaysTest extends AbstractPaymentApiTest {
    private static final int RENT = 23000;
    private static final ZoneId LIBREVILLE = ZoneId.of("Africa/Libreville");

    @BeforeEach
    void startedLongAgo() {
        jdbc.update("update contracts set start_date = current_date - 30, end_date = current_date + 300");
    }

    private ResultActions payDays(List<String> days, Object amount, UUID requestId) throws Exception {
        String amountPart = amount == null ? "" : ",\"amount\":" + amount;
        String body = "{\"driverId\":\"" + driver + "\",\"method\":\"AIRTEL_MONEY\",\"reference\":\"REF-" + UUID.randomUUID() + "\",\"requestId\":\"" + requestId + "\""
                + amountPart + ",\"days\":[" + String.join(",", days.stream().map(d -> "\"" + d + "\"").toList()) + "]}";
        return postJson("/api/v1/payments", body, manager);
    }

    private ResultActions payDays(String... days) throws Exception {
        return payDays(List.of(days), null, UUID.randomUUID());
    }

    private List<String> unpaidDays() throws Exception {
        String body = send(get("/api/v1/payments/accounts/" + driver), manager).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> days = JsonPath.read(body, "$.days[?(@.status=='UNPAID')].day");
        return days.stream().sorted().toList();
    }

    private List<Map<String, Object>> storedDays() {
        return jdbc.queryForList("select business_day::text as day, kind, paid_on_time from ledger_entry_days order by business_day");
    }

    @Test
    void chosenDaysAreSettledAndTheAmountIsTheServers() throws Exception {
        List<String> unpaid = unpaidDays();
        String third = unpaid.get(2);

        payDays(List.of(third), 1, UUID.randomUUID()).andExpect(status().isCreated()).andExpect(jsonPath("$.entry.amount").value(RENT));

        List<String> after = unpaidDays();
        assertEquals(unpaid.size() - 1, after.size());
        assertTrue(after.contains(unpaid.get(0)) && after.contains(unpaid.get(1)));
        assertEquals(List.of(third), storedDays().stream().map(row -> (String) row.get("day")).toList());
    }

    @Test
    void aReversalMakesTheChosenDayUnpaidAgain() throws Exception {
        List<String> unpaid = unpaidDays();
        String entry = entryId(payDays(unpaid.get(2)).andExpect(status().isCreated()));

        postJson("/api/v1/payments/" + entry + "/reverse", "{\"reason\":\"wrong number\"}", admin).andExpect(status().isOk());

        assertEquals(unpaid, unpaidDays());
    }

    @Test
    void aDayAlreadyPaidIsRefused() throws Exception {
        String day = unpaidDays().get(2);
        payDays(day).andExpect(status().isCreated());

        payDays(day).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DAY_NOT_PAYABLE"));
        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT'"));
    }

    @Test
    void aSundayAFutureDayAndADuplicateAreRefused() throws Exception {
        LocalDate today = LocalDate.now(LIBREVILLE);
        LocalDate sunday = today.minusDays(1);
        while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
            sunday = sunday.minusDays(1);
        }
        String day = unpaidDays().get(0);

        payDays(sunday.toString()).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DAY_NOT_PAYABLE"));
        payDays(today.plusDays(2).toString()).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DAY_NOT_PAYABLE"));
        payDays(day, day).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DAY_NOT_PAYABLE"));
        assertEquals(0, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT'"));
    }

    @Test
    void noDaysAndNoAmountIsABadRequest() throws Exception {
        payDays(List.of(), null, UUID.randomUUID()).andExpect(status().isBadRequest());
        assertEquals(0, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT'"));
    }

    @Test
    void aDoubleSubmitRecordsOnePayment() throws Exception {
        String day = unpaidDays().get(1);
        UUID requestId = UUID.randomUUID();

        payDays(List.of(day), null, requestId).andExpect(status().isCreated());
        payDays(List.of(day), null, requestId).andExpect(status().isCreated());

        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT'"));
    }

    @Test
    void theAuditLineListsTheDays() throws Exception {
        String day = unpaidDays().get(1);
        payDays(day).andExpect(status().isCreated());

        assertEquals(1, rows("select count(*) from audit_logs where action = 'PAYMENT_RECORDED' and details like ?", "%" + day + "%"));
    }

    @Test
    void onlyHoldersOfTheCreatePermissionCanChooseDays() throws Exception {
        String body = "{\"driverId\":\"" + driver + "\",\"method\":\"AIRTEL_MONEY\",\"reference\":\"REF-1\",\"requestId\":\"" + UUID.randomUUID() + "\",\"days\":[\"" + unpaidDays().get(0) + "\"]}";

        postJson("/api/v1/payments", body, viewer).andExpect(status().isForbidden());
    }

    @Test
    void anotherCountryCannotPayTheseDays() throws Exception {
        String body = "{\"driverId\":\"" + driver + "\",\"method\":\"AIRTEL_MONEY\",\"reference\":\"REF-2\",\"requestId\":\"" + UUID.randomUUID() + "\",\"days\":[\"" + unpaidDays().get(0) + "\"]}";

        postJson("/api/v1/payments", body, maliManager).andExpect(status().isNotFound());
        assertEquals(0, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT'"));
    }

    @Test
    void todaysOpenDayIsSettledOnTimeAndStoredForTheReceipt() throws Exception {
        ZonedDateTime now = ZonedDateTime.now(LIBREVILLE);
        assumeTrue(now.getDayOfWeek() != DayOfWeek.SUNDAY && now.toLocalTime().isBefore(LocalTime.NOON.minusMinutes(2)));

        payDays(now.toLocalDate().toString()).andExpect(status().isCreated());

        assertEquals(List.of(now.toLocalDate().toString()), storedDays().stream().map(row -> (String) row.get("day")).toList());
        assertEquals(true, storedDays().get(0).get("paid_on_time"));
    }

    @Test
    void theAccountListsPayableDaysOldestFirstWithTheirAmount() throws Exception {
        List<String> unpaid = unpaidDays();
        String body = send(get("/api/v1/payments/accounts/" + driver), manager).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<String> payable = JsonPath.read(body, "$.payableDays[*].day");
        assertTrue(payable.size() >= unpaid.size());
        assertEquals(payable.stream().sorted().toList(), payable);
        assertTrue(payable.containsAll(unpaid));
        assertEquals(RENT, (Integer) JsonPath.read(body, "$.payableDays[0].amount"));
        assertEquals(0, (Integer) JsonPath.read(body, "$.payableDays[0].penalty"));
    }

    @Test
    void aPaidDayLeavesThePayableList() throws Exception {
        String day = unpaidDays().get(2);
        payDays(day).andExpect(status().isCreated());

        String body = send(get("/api/v1/payments/accounts/" + driver), manager).andReturn().getResponse().getContentAsString();
        List<String> payable = JsonPath.read(body, "$.payableDays[*].day");
        assertTrue(!payable.contains(day));
    }

    @Test
    void aDriverWithoutAContractHasNoPayableDays() throws Exception {
        jdbc.update("update contracts set status = 'ENDED', ended_at = now()");

        send(get("/api/v1/payments/accounts/" + driver), manager).andExpect(status().isOk()).andExpect(jsonPath("$.payableDays.length()").value(0));
    }
}
