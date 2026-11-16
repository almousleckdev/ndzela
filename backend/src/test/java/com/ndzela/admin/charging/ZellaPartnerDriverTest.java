package com.ndzela.admin.charging;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The Zella app reads the contract and the car of a linked customer with its partner key, and nothing else.
class ZellaPartnerDriverTest extends AbstractZellaFeedTest {
    private static final String PATH = "/api/v1/partners/zella/customers/%s/driver";

    private ResultActions read(String customerId, String key) throws Exception {
        var request = get(PATH.formatted(customerId));
        return mvc.perform(key == null ? request : request.header("X-Api-Key", key));
    }

    @Test
    void aLinkedCustomerAnswersWithTheContractAndTheCar() throws Exception {
        read("cust-1", "partner-test-key").andExpect(status().isOk())
                .andExpect(jsonPath("$.driver.email").value(email))
                .andExpect(jsonPath("$.contract.number").isNotEmpty())
                .andExpect(jsonPath("$.contract.status").value("ACTIVE"))
                .andExpect(jsonPath("$.car.fleetNumber").isNotEmpty())
                .andExpect(jsonPath("$.car.plate").isNotEmpty());
    }

    @Test
    void theAnswerCarriesNoIdentityDocumentNorAddress() throws Exception {
        String body = read("cust-1", "partner-test-key").andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(body.contains("idNumber") || body.contains("address") || body.contains("vin") || body.contains("birth"));
    }

    @Test
    void anUnknownCustomerIsNotFound() throws Exception {
        read("someone-else", "partner-test-key").andExpect(status().isNotFound());
    }

    @Test
    void aMissingOrWrongKeyIsRefused() throws Exception {
        read("cust-1", null).andExpect(status().isUnauthorized());
        read("cust-1", "wrong").andExpect(status().isUnauthorized());
    }
}
