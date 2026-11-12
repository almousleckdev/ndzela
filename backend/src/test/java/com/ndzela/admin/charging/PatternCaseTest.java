package com.ndzela.admin.charging;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// While a driver is suspended for arrears, an account of ours whose charges look like two cars is worth a look, even with no money trail to the suspended driver.
class PatternCaseTest extends AbstractEvidenceTest {
    private static final String PATTERNS = "/api/v1/charging/suspension-cases/patterns";

    private void theFirstDriverIsSuspendedForArrears() {
        jdbc.update("update drivers set status = 'SUSPENDED', suspension_cause = 'ARREARS' where id = (select driver_id from id_tags where customer_id = 'cust-1')");
    }

    @Test
    void anAccountChargingTwoCarsAtOnceIsListedWithTheSuspendedDrivers() throws Exception {
        theFirstDriverIsSuspendedForArrears();
        secondLinkedDriver();
        charge("Z-DEF", "P1", -120, 60, "CP-1", 1, 20, 90, 20000);
        charge("Z-DEF", "P2", -60, 30, "CP-2", 1, 30, 70, 15000);

        send(get(PATTERNS), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].account.name").isNotEmpty())
                .andExpect(jsonPath("$[0].account.vehicle").isNotEmpty())
                .andExpect(jsonPath("$[0].reasons[0]").value("OVERLAP"))
                .andExpect(jsonPath("$[0].suspendedDrivers.length()").value(1));
    }

    @Test
    void theGlobalViewListsPatternsWithoutPickingACountry() throws Exception {
        theFirstDriverIsSuspendedForArrears();
        secondLinkedDriver();
        charge("Z-DEF", "G3", -120, 60, "CP-1", 1, 20, 90, 20000);
        charge("Z-DEF", "G4", -60, 30, "CP-2", 1, 30, 70, 15000);

        send(get(PATTERNS), root).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reasons[0]").value("OVERLAP"));
    }

    @Test
    void aQuietAccountIsNotListed() throws Exception {
        theFirstDriverIsSuspendedForArrears();
        secondLinkedDriver();
        charge("Z-DEF", "P3", -120, -60, "CP-1", 1, 20, 90, 20000);

        send(get(PATTERNS), admin).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void nothingIsListedWhileNobodyIsSuspendedForArrears() throws Exception {
        secondLinkedDriver();
        charge("Z-DEF", "P4", -120, 60, "CP-1", 1, 20, 90, 20000);
        charge("Z-DEF", "P5", -60, 30, "CP-2", 1, 30, 70, 15000);

        send(get(PATTERNS), admin).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void theSuspendedDriversOwnChargesAreNeverTheirOwnCase() throws Exception {
        theFirstDriverIsSuspendedForArrears();
        charge("Z-ABC", "P6", -120, 60, "CP-1", 1, 20, 90, 20000);
        charge("Z-ABC", "P7", -60, 30, "CP-2", 1, 30, 70, 15000);

        send(get(PATTERNS), admin).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void theListNeedsArrearsManage() throws Exception {
        send(get(PATTERNS), manager).andExpect(status().isForbidden());
        send(get(PATTERNS), admin).andExpect(status().isOk());
    }
}
