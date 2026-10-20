package com.ndzela.admin.driverapp;

import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.infrastructure.pvit.PvitFees;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A driver follows a payment and reads it back
class DriverPaymentReadTest extends AbstractPvitFlowTest {
    @Test
    void theDriverFollowsAPaymentAndSeesTheirOwnHistory() throws Exception {
        String id = startOk();

        asDriver("/api/v1/driver/payments/" + id, token).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.phone").value("074111111")).andExpect(jsonPath("$.beneficiary").value(not(emptyOrNullString()))).andExpect(jsonPath("$.reference").value(reference())).andExpect(jsonPath("$.providerReference").value("PAY1"));
        asDriver("/api/v1/driver/payments", token).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void aDriverCannotReadAnotherDriversPaymentNorAnotherCountrys() throws Exception {
        String id = startOk();
        DriverJson other = DriverJson.create(newVehicle(manager), newCenter(manager, "AUTRE"));
        createDriver(manager, other).andExpect(status().isCreated());
        DriverJson mali = DriverJson.create(newVehicle(maliManager), newCenter(maliManager, "NDZELA ML"));
        createDriver(maliManager, mali).andExpect(status().isCreated());
        jdbc.update("update drivers set must_change_password = false");

        for (DriverJson intruder : new DriverJson[] {other, mali}) {
            String intruderToken = JsonPath.read(login(String.valueOf(intruder.personalValue("email")), DRIVER_PASSWORD).andReturn().getResponse().getContentAsString(), "$.accessToken");
            asDriver("/api/v1/driver/payments/" + id, intruderToken).andExpect(status().isNotFound());
            asDriver("/api/v1/driver/payments", intruderToken).andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Test
    void theQuoteShowsTheAmountTheFeeAndTheAccountHolder() throws Exception {
        when(pvit.fees(eq("MOOV_MONEY"), any())).thenReturn(new PvitFees(new BigDecimal("2415.0"), new BigDecimal("71415.0")));
        when(pvit.accountHolder(eq("MOOV_MONEY"), eq("062821855"))).thenReturn("Awa Nze");

        asDriver("/api/v1/driver/payments/quote?operator=MOOV_MONEY&phone=62821855", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(greaterThan(0))).andExpect(jsonPath("$.fee").value(2415.0))
                .andExpect(jsonPath("$.currency").value("XAF")).andExpect(jsonPath("$.accountHolder").value("Awa Nze"));
        asDriver("/api/v1/driver/payments/quote?operator=ORANGE_MONEY", token).andExpect(status().isUnprocessableEntity());
    }
}
