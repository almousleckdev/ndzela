package com.ndzela.admin.payment;

import com.jayway.jsonpath.JsonPath;
import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The history shows the days each payment settled, and can tell former riders from current ones
class PaymentHistoryDaysTest extends AbstractDriverTest {
    private String driver;

    @BeforeEach
    void paidOnce() throws Exception {
        driver = id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated()));
        jdbc.update("update contracts set start_date = current_date - 30, end_date = current_date + 300");
        String body = "{\"driverId\":\"" + driver + "\",\"amount\":23000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"REF-" + UUID.randomUUID() + "\",\"requestId\":\"" + UUID.randomUUID() + "\"}";
        postJson("/api/v1/payments", body, manager).andExpect(status().isCreated());
    }

    @Test
    void eachPaymentListsTheDaysItSettled() throws Exception {
        String account = send(get("/api/v1/payments/accounts/" + driver), manager).andReturn().getResponse().getContentAsString();
        List<String> unpaid = JsonPath.read(account, "$.days[?(@.status=='UNPAID')].day");
        String oldest = unpaid.stream().sorted().findFirst().orElseThrow();
        List<String> paid = JsonPath.read(account, "$.days[?(@.status=='PAID_LATE')].day");
        String firstPaid = paid.get(0);

        send(get("/api/v1/payments/history"), viewer).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].days.length()").value(1))
                .andExpect(jsonPath("$.items[0].days[0].kind").value("RENT")).andExpect(jsonPath("$.items[0].days[0].amount").value(23000))
                .andExpect(jsonPath("$.items[0].days[0].onTime").value(false)).andExpect(jsonPath("$.items[0].days[0].day").value(firstPaid));
        assertEquals(false, unpaid.contains(firstPaid));
        assertEquals(true, oldest.compareTo(firstPaid) > 0);
    }

    @Test
    void theTransactionsTableShowsTheDaysAndWhenTheyWerePaid() throws Exception {
        send(get("/api/v1/payments/transactions"), viewer).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].days.length()").value(1))
                .andExpect(jsonPath("$.items[0].days[0].kind").value("RENT")).andExpect(jsonPath("$.items[0].paidAt").exists());
    }

    @Test
    void theScopeSeparatesFormerRidersFromCurrentOnes() throws Exception {
        jdbc.update("update drivers set status = 'INACTIVE'");

        send(get("/api/v1/payments/history?scope=FORMER"), viewer).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/history?scope=CURRENT"), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/history?scope=ALL"), viewer).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/history"), viewer).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void aCurrentRiderIsInTheCurrentScopeOnly() throws Exception {
        send(get("/api/v1/payments/history?scope=CURRENT"), viewer).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/history?scope=FORMER"), viewer).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void anotherCountrySeesNeitherThePaymentsNorTheirDays() throws Exception {
        send(get("/api/v1/payments/history"), maliManager).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void theTransactionsTableCanBeLimitedToFormerOrCurrentRiders() throws Exception {
        send(get("/api/v1/payments/transactions?scope=CURRENT"), viewer).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/transactions?scope=FORMER"), viewer).andExpect(jsonPath("$.total").value(0));

        jdbc.update("update drivers set status = 'INACTIVE'");

        send(get("/api/v1/payments/transactions?scope=CURRENT"), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/transactions?scope=FORMER"), viewer).andExpect(jsonPath("$.total").value(1));
    }
}
