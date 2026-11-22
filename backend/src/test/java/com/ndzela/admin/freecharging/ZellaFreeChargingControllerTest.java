package com.ndzela.admin.freecharging;

import com.ndzela.admin.freecharging.domain.FreeChargingWindow;
import com.ndzela.admin.freecharging.domain.WindowSetting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Zella reads the free Sunday answer of a customer and manages the agreements with its partner key, nothing else.
class ZellaFreeChargingControllerTest extends AbstractFreeChargingTest {
    private static final String KEY = "partner-test-key";
    private static final String BASE = "/api/v1/partners/zella/free-charging/agreements";

    private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 22);
    private static final Instant WINDOW_END = Instant.parse("2026-11-22T21:00:00Z");

    // The window is pinned instead of opened around the wall clock, so a run that crosses midnight in Libreville cannot flake; the window itself is covered by WindowSettingTest.
    @MockitoBean private FreeChargingWindow window;

    @BeforeEach
    void windowIsOpen() {
        when(window.contains(any(), any())).thenReturn(true);
        when(window.sundayOf(any(), any())).thenReturn(SUNDAY);
        when(window.endOf(any(), any())).thenReturn(WINDOW_END);
        when(window.setting()).thenReturn(new WindowSetting(DayOfWeek.SUNDAY, LocalTime.of(12, 0), LocalTime.of(22, 0)));
    }

    private ResultActions read(String customerId, String key) throws Exception {
        var request = get("/api/v1/partners/zella/customers/" + customerId + "/free-charging");
        return mvc.perform(key == null ? request : request.header("X-Api-Key", key));
    }

    private String body(UUID driver) {
        return "{\"driverId\":\"" + driver + "\",\"startsOn\":\"2026-01-01\",\"endsOn\":null,\"conditions\":\"Partner\"}";
    }

    private ResultActions create(UUID driver, String actor) throws Exception {
        var request = post(BASE).header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON).content(body(driver));
        return mvc.perform(actor == null ? request : request.header("X-Actor", actor));
    }

    private ResultActions createFor(String customerId, String extra) throws Exception {
        String body = "{" + (customerId == null ? "" : "\"customerId\":\"" + customerId + "\",") + extra + "\"startsOn\":\"2026-01-01\"}";
        return mvc.perform(post(BASE).header("X-Api-Key", KEY).header("X-Actor", "mireille").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void aZellaCustomerIdResolvesTheLinkedDriverAndCreatesTheAgreement() throws Exception {
        createFor("cust-" + linkedDriver, "").andExpect(status().isCreated())
                .andExpect(jsonPath("$.driverId").value(linkedDriver.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertEquals(1, rows("select count(*) from audit_logs where action = 'FREE_AGREEMENT_CREATED' and actor = 'zella:mireille'"));
    }

    @Test
    void anUnknownOrUnlinkedCustomerIsNotLinked() throws Exception {
        createFor("nobody", "").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("FREE_AGREEMENT_NOT_LINKED"));
        jdbc.update("update id_tags set status = 'UNLINKED' where driver_id = ?", linkedDriver);
        createFor("cust-" + linkedDriver, "").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("FREE_AGREEMENT_NOT_LINKED"));
        assertEquals(0, rows("select count(*) from free_charging_agreements"));
    }

    @Test
    void aSuggestedButUnconfirmedTagDoesNotResolveTheCustomer() throws Exception {
        jdbc.update("update id_tags set status = 'SUGGESTED' where driver_id = ?", linkedDriver);
        createFor("cust-" + linkedDriver, "").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("FREE_AGREEMENT_NOT_LINKED"));
        assertEquals(0, rows("select count(*) from free_charging_agreements"));
    }

    @Test
    void anotherCountrysCustomerIsNotLinkedHere() throws Exception {
        createFor("cust-" + maliDriver, "").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("FREE_AGREEMENT_NOT_LINKED"));
        assertEquals(0, rows("select count(*) from free_charging_agreements"));
    }

    @Test
    void bothOrNeitherOfDriverIdAndCustomerIdIsABadRequest() throws Exception {
        createFor("cust-" + linkedDriver, "\"driverId\":\"" + linkedDriver + "\",").andExpect(status().isBadRequest());
        createFor(null, "").andExpect(status().isBadRequest());
        createFor("  ", "").andExpect(status().isBadRequest());
        assertEquals(0, rows("select count(*) from free_charging_agreements"));
    }

    @Test
    void anEligibleDriverAnswersWithTheContractJson() throws Exception {
        UUID agreement = inGabon(() -> agreements.create(linkedDriver, LocalDate.of(2026, 1, 1), null, null)).getId();

        read("cust-" + linkedDriver, KEY).andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.reason").value("ELIGIBLE"))
                .andExpect(jsonPath("$.agreementId").value(agreement.toString()))
                .andExpect(jsonPath("$.sundayDate").value("2026-11-22"))
                .andExpect(jsonPath("$.windowEndsAt").value("2026-11-22T21:00:00Z"));
    }

    @Test
    void anotherDriverGetsTheReasonAndNullFields() throws Exception {
        read("cust-" + linkedDriver, KEY).andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.reason").value("NO_AGREEMENT"))
                .andExpect(jsonPath("$.agreementId").value((Object) null))
                .andExpect(jsonPath("$.sundayDate").value((Object) null))
                .andExpect(jsonPath("$.windowEndsAt").value((Object) null));
        read("nobody", KEY).andExpect(status().isOk()).andExpect(jsonPath("$.reason").value("NOT_LINKED"));
    }

    @Test
    void aMissingOrWrongKeyIsRefusedWithNoData() throws Exception {
        for (String key : new String[] {null, "wrong"}) {
            String answer = read("cust-" + linkedDriver, key).andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
            assertFalse(answer.contains("eligible"));
        }
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE).header("X-Api-Key", "wrong")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/partners/zella/free-charging/usage")).andExpect(status().isUnauthorized());
        assertEquals(0, rows("select count(*) from free_charging_agreements"));
    }

    @Test
    void managementWritesAreRefusedWithoutTheKey() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body(linkedDriver))).andExpect(status().isUnauthorized());
        assertEquals(0, rows("select count(*) from free_charging_agreements"));
    }

    @Test
    void theXActorIsStoredInTheAuditEntryAsZellaColonActor() throws Exception {
        create(linkedDriver, "mireille").andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"));

        assertEquals(1, rows("select count(*) from audit_logs where action = 'FREE_AGREEMENT_CREATED' and actor = 'zella:mireille'"));
        assertEquals(1, rows("select count(*) from free_charging_agreements where created_by = 'zella:mireille'"));
    }

    @Test
    void aCallWithoutActorIsRecordedAsUnknown() throws Exception {
        create(linkedDriver, null).andExpect(status().isCreated());

        assertEquals(1, rows("select count(*) from audit_logs where action = 'FREE_AGREEMENT_CREATED' and actor = 'zella:unknown'"));
    }

    @Test
    void zellaListsUpdatesSuspendsResumesAndRevokes() throws Exception {
        String id = id(create(linkedDriver, "mireille").andExpect(status().isCreated()));

        mvc.perform(get(BASE).header("X-Api-Key", KEY)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].driverId").value(linkedDriver.toString()))
                .andExpect(jsonPath("$[0].eligibility").value("ELIGIBLE"))
                .andExpect(jsonPath("$[0].unpaidDays").value(0));
        mvc.perform(put(BASE + "/" + id).header("X-Api-Key", KEY).header("X-Actor", "mireille").contentType(MediaType.APPLICATION_JSON)
                .content("{\"startsOn\":\"2026-02-01\",\"endsOn\":\"2026-12-31\",\"conditions\":\"Changed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.endsOn").value("2026-12-31"));
        for (String step : new String[] {"suspend", "resume", "revoke"}) {
            mvc.perform(post(BASE + "/" + id + "/" + step).header("X-Api-Key", KEY).header("X-Actor", "mireille")).andExpect(status().isOk());
        }
        assertEquals(1, rows("select count(*) from audit_logs where action = 'FREE_AGREEMENT_REVOKED' and actor = 'zella:mireille'"));
        mvc.perform(get("/api/v1/partners/zella/free-charging/usage").header("X-Api-Key", KEY)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void theManagementCallsAreRateLimited() throws Exception {
        for (int i = 0; i < 120; i++) {
            mvc.perform(get(BASE).header("X-Api-Key", KEY)).andExpect(status().isOk());
        }
        mvc.perform(get(BASE).header("X-Api-Key", KEY)).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void theReadIsRateLimitedPerCustomer() throws Exception {
        for (int i = 0; i < 30; i++) {
            read("nobody", KEY).andExpect(status().isOk());
        }
        read("nobody", KEY).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
        read("somebody-else", KEY).andExpect(status().isOk()).andExpect(jsonPath("$.reason").value("NOT_LINKED"));
    }

    @Test
    void zellaReadsAndChangesTheWindowWithTheKeyAndTheActorIsAudited() throws Exception {
        String url = "/api/v1/partners/zella/free-charging/window";

        mvc.perform(get(url).header("X-Api-Key", KEY)).andExpect(status().isOk())
                .andExpect(jsonPath("$.day").value("SUNDAY")).andExpect(jsonPath("$.endOfDay").value(true));
        mvc.perform(put(url).header("X-Api-Key", KEY).header("X-Actor", "mireille").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"day\":\"SATURDAY\",\"from\":\"09:00\",\"until\":\"17:30\",\"endOfDay\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.from").value("09:00")).andExpect(jsonPath("$.until").value("17:30"));

        mvc.perform(get(url).header("X-Api-Key", KEY)).andExpect(jsonPath("$.day").value("SATURDAY"));
        assertEquals(1, rows("select count(*) from audit_logs where action = 'FREE_WINDOW_CHANGED' and actor = 'zella:mireille'"));
    }

    @Test
    void theWindowRoutesRefuseACallWithoutTheKey() throws Exception {
        String url = "/api/v1/partners/zella/free-charging/window";

        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{\"day\":\"SUNDAY\",\"from\":\"00:00\",\"endOfDay\":true}")).andExpect(status().isUnauthorized());
        assertEquals(0, rows("select count(*) from free_charging_window"));
    }
}
