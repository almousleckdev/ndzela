package com.ndzela.admin.payment;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A GA driver with a vehicle, a cashier and an accountant, and helpers to record payments through the API.
abstract class AbstractPaymentApiTest extends AbstractDriverTest {
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
}
