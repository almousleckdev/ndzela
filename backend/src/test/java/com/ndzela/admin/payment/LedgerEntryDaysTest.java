package com.ndzela.admin.payment;

import com.jayway.jsonpath.JsonPath;
import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LedgerEntryDaysTest extends AbstractDriverTest {
    private static final int RENT = 23000;
    private String driver;

    @BeforeEach
    void startedLongAgo() throws Exception {
        driver = id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated()));
        jdbc.update("update contracts set start_date = current_date - 30, end_date = current_date + 300");
    }

    private ResultActions pay(Object amount) throws Exception {
        String body = "{\"driverId\":\"" + driver + "\",\"amount\":" + amount + ",\"method\":\"AIRTEL_MONEY\",\"reference\":\"REF-" + UUID.randomUUID() + "\",\"requestId\":\"" + UUID.randomUUID() + "\"}";
        return postJson("/api/v1/payments", body, manager);
    }

    private String entryId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.entry.id");
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
    void payingOneOfManyUnpaidDaysStoresTheOldestDayOnly() throws Exception {
        List<String> unpaid = unpaidDays();
        assertFalse(unpaid.isEmpty());

        pay(RENT).andExpect(status().isCreated());

        List<Map<String, Object>> stored = storedDays();
        assertEquals(1, stored.size());
        assertEquals(unpaid.get(0), stored.get(0).get("day"));
        assertEquals("RENT", stored.get(0).get("kind"));
        assertEquals(false, stored.get(0).get("paid_on_time"));
    }

    @Test
    void payingTwoDaysStoresBothOldestFirst() throws Exception {
        List<String> unpaid = unpaidDays();

        pay(2 * RENT).andExpect(status().isCreated());

        assertEquals(List.of(unpaid.get(0), unpaid.get(1)), storedDays().stream().map(row -> (String) row.get("day")).toList());
    }

    @Test
    void anAmountBelowOneDayStoresNoDay() throws Exception {
        pay(5000).andExpect(status().isCreated());

        assertEquals(0, storedDays().size());
    }

    @Test
    void storedDaysCannotBeChangedOrDeleted() throws Exception {
        pay(RENT).andExpect(status().isCreated());

        assertThrows(DataAccessException.class, () -> jdbc.update("update ledger_entry_days set paid_on_time = true"));
        assertThrows(DataAccessException.class, () -> jdbc.update("delete from ledger_entry_days"));
    }

    @Test
    void reversingThePaymentLeavesItsStoredDaysAsTheyWere() throws Exception {
        String entry = entryId(pay(RENT).andExpect(status().isCreated()));
        List<Map<String, Object>> before = storedDays();

        postJson("/api/v1/payments/" + entry + "/reverse", "{\"reason\":\"wrong number\"}", admin).andExpect(status().isOk());

        assertEquals(before, storedDays());
    }
}
