package com.ndzela.admin.freecharging;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Staff read the free window with free_charging:read and change it with free_charging:manage; every change is audited.
class FreeChargingWindowControllerTest extends AbstractFreeChargingTest {
    private static final String WINDOW = "/api/v1/free-charging/window";

    private String body(String day, String from, String until, boolean endOfDay) {
        return "{\"day\":\"" + day + "\",\"from\":\"" + from + "\",\"until\":" + (until == null ? "null" : "\"" + until + "\"") + ",\"endOfDay\":" + endOfDay + "}";
    }

    @Test
    void withoutASettingTheWindowIsSundayAllDay() throws Exception {
        send(get(WINDOW), viewer).andExpect(status().isOk())
                .andExpect(jsonPath("$.day").value("SUNDAY"))
                .andExpect(jsonPath("$.from").value("00:00"))
                .andExpect(jsonPath("$.until").value("23:59"))
                .andExpect(jsonPath("$.endOfDay").value(true));
    }

    @Test
    void anAdministratorChangesTheWindowAndItIsReadBack() throws Exception {
        send(put(WINDOW).contentType(MediaType.APPLICATION_JSON).content(body("SATURDAY", "10:00", "20:00", false)), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.day").value("SATURDAY")).andExpect(jsonPath("$.endOfDay").value(false));

        send(get(WINDOW), viewer).andExpect(jsonPath("$.from").value("10:00")).andExpect(jsonPath("$.until").value("20:00"));
        assertEquals(1, rows("select count(*) from audit_logs where action = 'FREE_WINDOW_CHANGED' and actor = 'ga.admin@ndzela.test'"));
    }

    @Test
    void endOfDayIgnoresTheGivenEnd() throws Exception {
        send(put(WINDOW).contentType(MediaType.APPLICATION_JSON).content(body("SUNDAY", "08:00", "10:00", true)), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.until").value("23:59")).andExpect(jsonPath("$.endOfDay").value(true));
    }

    @Test
    void aUserWithoutManageCannotChangeIt() throws Exception {
        send(put(WINDOW).contentType(MediaType.APPLICATION_JSON).content(body("SUNDAY", "00:00", null, true)), viewer).andExpect(status().isForbidden());
        send(put(WINDOW).contentType(MediaType.APPLICATION_JSON).content(body("SUNDAY", "00:00", null, true)), manager).andExpect(status().isForbidden());
        send(get(WINDOW), staff("ga.nobody@ndzela.test", "GA")).andExpect(status().isForbidden());
    }

    @Test
    void anEndBeforeTheStartIsRefusedWithAStableCode() throws Exception {
        send(put(WINDOW).contentType(MediaType.APPLICATION_JSON).content(body("SUNDAY", "22:00", "12:00", false)), admin)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("FREE_WINDOW_INVALID"));
        assertEquals(0, rows("select count(*) from free_charging_window"));
    }

    @Test
    void aMissingDayOrStartIsABadRequest() throws Exception {
        send(put(WINDOW).contentType(MediaType.APPLICATION_JSON).content("{\"from\":\"00:00\",\"endOfDay\":true}"), admin).andExpect(status().isBadRequest());
        send(put(WINDOW).contentType(MediaType.APPLICATION_JSON).content("{\"day\":\"SUNDAY\",\"endOfDay\":true}"), admin).andExpect(status().isBadRequest());
    }

    @Test
    void anotherCountrysAdministratorKeepsHisOwnWindow() throws Exception {
        send(put(WINDOW).contentType(MediaType.APPLICATION_JSON).content(body("FRIDAY", "08:00", "18:00", false)), admin).andExpect(status().isOk());

        send(get(WINDOW), maliManager).andExpect(status().isOk()).andExpect(jsonPath("$.day").value("SUNDAY")).andExpect(jsonPath("$.endOfDay").value(true));
    }
}
