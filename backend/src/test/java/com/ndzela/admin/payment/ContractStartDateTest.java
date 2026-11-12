package com.ndzela.admin.payment;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The start date moves only while nothing is recorded on the contract.
class ContractStartDateTest extends AbstractPaymentApiTest {
    private org.springframework.test.web.servlet.ResultActions move(LocalDate date, com.ndzela.admin.user.domain.User as) throws Exception {
        return send(put("/api/v1/drivers/" + driver + "/contract/start-date").contentType("application/json").content("{\"startDate\":\"" + date + "\"}"), as);
    }

    private LocalDate today() {
        return LocalDate.parse(jdbc.queryForObject("select (now() at time zone 'Africa/Libreville')::date::text", String.class));
    }

    @Test
    void theStartDateMovesAndTheEndDateFollows() throws Exception {
        String endBefore = jdbc.queryForObject("select end_date::text from contracts", String.class);
        LocalDate later = today().plusDays(10);

        move(later, manager).andExpect(status().isOk()).andExpect(jsonPath("$.startDate").value(later.toString())).andExpect(jsonPath("$.revision").value(1));

        assertEquals(later.toString(), jdbc.queryForObject("select start_date::text from contracts", String.class));
        org.junit.jupiter.api.Assertions.assertNotEquals(endBefore, jdbc.queryForObject("select end_date::text from contracts", String.class));
        assertEquals(1, auditRows("CONTRACT_RESCHEDULED"));
    }

    @Test
    void refusesOnceAPaymentIsRecorded() throws Exception {
        pay(manager, 23000).andExpect(status().isCreated());
        move(today().plusDays(3), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("CONTRACT_HAS_ACTIVITY"));
        assertEquals(0, auditRows("CONTRACT_RESCHEDULED"));
    }

    @Test
    void refusesDatesOutOfRangeAndMissingRights() throws Exception {
        move(today().minusDays(1), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("START_DATE_OUT_OF_RANGE"));
        move(today().plusDays(91), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("START_DATE_OUT_OF_RANGE"));
        move(today().plusDays(2), viewer).andExpect(status().isForbidden());
        move(today().plusDays(2), maliManager).andExpect(status().isNotFound());
    }
}
