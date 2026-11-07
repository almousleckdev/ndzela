package com.ndzela.admin.driverapp;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The recent rent days the weekly history shows
class DriverAccountDaysTest extends AbstractDriverPayingTest {
    private List<String> field(String path) throws Exception {
        String body = asDriver("/api/v1/driver/account", token).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, path);
    }

    @Test
    void theAccountListsTheRecentRentDaysOldestFirst() throws Exception {
        withUnpaidDays(2);

        List<String> days = field("$.days[*].day");

        assertTrue(days.size() >= 2);
        assertEquals(days.stream().sorted().toList(), days);
        assertTrue(days.stream().allMatch(day -> !LocalDate.parse(day).isAfter(LocalDate.now())));
    }

    @Test
    void theUnpaidDaysAreMarkedUnpaid() throws Exception {
        withUnpaidDays(2);

        List<String> unpaid = field("$.days[?(@.status=='UNPAID')].day");

        int late = com.jayway.jsonpath.JsonPath.read(asDriver("/api/v1/driver/account", token).andReturn().getResponse().getContentAsString(), "$.unpaidDays");
        assertEquals(late, unpaid.size());
        assertTrue(late >= 1);
    }

    @Test
    void aDriverWithNothingOwedStillGetsAnEmptyList() throws Exception {
        jdbc.update("update contracts set start_date = current_date, end_date = current_date + 300");

        assertEquals(0, field("$.days[*]").size());
    }
}
