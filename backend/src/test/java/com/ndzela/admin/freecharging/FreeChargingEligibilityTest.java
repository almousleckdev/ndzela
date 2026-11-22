package com.ndzela.admin.freecharging;

import com.ndzela.admin.freecharging.application.Eligibility;
import com.ndzela.admin.freecharging.application.FreeChargingEligibility;
import com.ndzela.admin.freecharging.application.FreeChargingWindowService;
import com.ndzela.admin.freecharging.domain.EligibilityReason;
import com.ndzela.admin.freecharging.domain.FreeChargingAgreement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// One answer per reason, decided in a fixed order, from the driver linked to a Zella customer.
class FreeChargingEligibilityTest extends AbstractFreeChargingTest {
    private static final ZoneId ZONE = ZoneId.of("Africa/Libreville");
    private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 22);
    private static final Instant SUNDAY_NOON = at("2026-11-22T12:00:00");

    @Autowired private FreeChargingEligibility eligibility;
    @Autowired private FreeChargingWindowService windows;

    @BeforeEach
    void noonToTenWindow() {
        inGabon(() -> windows.update(DayOfWeek.SUNDAY, LocalTime.of(12, 0), LocalTime.of(22, 0)));
    }

    private static Instant at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(ZONE).toInstant();
    }

    private String customerOf(UUID driver) {
        return "cust-" + driver;
    }

    private Eligibility check(UUID driver, Instant now) {
        return inGabon(() -> eligibility.check(customerOf(driver), now));
    }

    private FreeChargingAgreement agreement(LocalDate startsOn, LocalDate endsOn) {
        return inGabon(() -> agreements.create(linkedDriver, startsOn, endsOn, null));
    }

    private FreeChargingAgreement validAgreement() {
        return agreement(SUNDAY.minusDays(30), SUNDAY.plusDays(30));
    }

    private void assertRefused(Eligibility answer, EligibilityReason reason) {
        assertFalse(answer.eligible());
        assertEquals(reason, answer.reason());
        assertNull(answer.agreementId());
        assertNull(answer.sundayDate());
        assertNull(answer.windowEndsAt());
    }

    @Test
    void eligibleWithZeroUnpaidDaysInsideTheWindow() {
        FreeChargingAgreement agreement = validAgreement();

        Eligibility answer = check(linkedDriver, SUNDAY_NOON);

        assertTrue(answer.eligible());
        assertEquals(EligibilityReason.ELIGIBLE, answer.reason());
        assertEquals(agreement.getId(), answer.agreementId());
        assertEquals(SUNDAY, answer.sundayDate());
        assertEquals(at("2026-11-22T22:00:00"), answer.windowEndsAt());
    }

    @Test
    void unknownCustomerGivesNOT_LINKED() {
        assertRefused(inGabon(() -> eligibility.check("nobody", SUNDAY_NOON)), EligibilityReason.NOT_LINKED);
    }

    @Test
    void driverWithoutAgreementGivesNO_AGREEMENT() {
        assertRefused(check(linkedDriver, SUNDAY_NOON), EligibilityReason.NO_AGREEMENT);
    }

    @Test
    void revokedAgreementGivesNO_AGREEMENT() {
        FreeChargingAgreement agreement = validAgreement();
        inGabon(() -> agreements.revoke(agreement.getId()));

        assertRefused(check(linkedDriver, SUNDAY_NOON), EligibilityReason.NO_AGREEMENT);
    }

    @Test
    void suspendedAgreementGivesAGREEMENT_SUSPENDED() {
        FreeChargingAgreement agreement = validAgreement();
        inGabon(() -> agreements.suspend(agreement.getId()));

        assertRefused(check(linkedDriver, SUNDAY_NOON), EligibilityReason.AGREEMENT_SUSPENDED);
    }

    @Test
    void agreementStartingTomorrowGivesAGREEMENT_NOT_STARTED() {
        agreement(SUNDAY.plusDays(1), null);

        assertRefused(check(linkedDriver, SUNDAY_NOON), EligibilityReason.AGREEMENT_NOT_STARTED);
    }

    @Test
    void agreementEndedYesterdayGivesAGREEMENT_ENDED() {
        agreement(SUNDAY.minusDays(30), SUNDAY.minusDays(1));

        assertRefused(check(linkedDriver, SUNDAY_NOON), EligibilityReason.AGREEMENT_ENDED);
    }

    @Test
    void agreementEndingTodayIsStillEligible() {
        agreement(SUNDAY.minusDays(30), SUNDAY);

        assertEquals(EligibilityReason.ELIGIBLE, check(linkedDriver, SUNDAY_NOON).reason());
    }

    @Test
    void agreementStartingTodayIsEligible() {
        agreement(SUNDAY, null);

        assertTrue(check(linkedDriver, at("2026-11-22T21:59:59")).eligible());
    }

    @Test
    void suspendedDriverGivesDRIVER_SUSPENDED() {
        validAgreement();
        jdbc.update("update drivers set status = 'SUSPENDED' where id = ?", linkedDriver);

        assertRefused(check(linkedDriver, SUNDAY_NOON), EligibilityReason.DRIVER_SUSPENDED);
    }

    @Test
    void blockedDriverGivesDRIVER_SUSPENDED() {
        validAgreement();
        jdbc.update("update drivers set status = 'BLOCKED' where id = ?", linkedDriver);

        assertRefused(check(linkedDriver, SUNDAY_NOON), EligibilityReason.DRIVER_SUSPENDED);
    }

    @Test
    void archivedDriverGivesDRIVER_SUSPENDED() {
        validAgreement();
        jdbc.update("update drivers set status = 'ARCHIVED' where id = ?", linkedDriver);

        assertRefused(check(linkedDriver, SUNDAY_NOON), EligibilityReason.DRIVER_SUSPENDED);
    }

    @Test
    void inactiveDriverGivesDRIVER_SUSPENDED() {
        validAgreement();
        jdbc.update("update drivers set status = 'INACTIVE' where id = ?", linkedDriver);

        assertRefused(check(linkedDriver, SUNDAY_NOON), EligibilityReason.DRIVER_SUSPENDED);
    }

    @Test
    void outsideTheWindowGivesOUTSIDE_WINDOW() {
        validAgreement();

        assertRefused(check(linkedDriver, at("2026-11-22T11:59:59")), EligibilityReason.OUTSIDE_WINDOW);
        assertRefused(check(linkedDriver, at("2026-11-22T22:00:00")), EligibilityReason.OUTSIDE_WINDOW);
        assertRefused(check(linkedDriver, at("2026-11-23T15:00:00")), EligibilityReason.OUTSIDE_WINDOW);
    }

    private void saturdayIsTheFirstRentDay() {
        jdbc.update("update contracts set start_date = date '2026-11-20', end_date = date '2027-11-20' where driver_id = ?", linkedDriver);
    }

    private void saturdayRentPaid() {
        jdbc.update("insert into ledger_entries (id, country_id, contract_id, driver_id, entry_type, amount, method, reference, recorded_by, created_at) "
                + "select gen_random_uuid(), country_id, id, driver_id, 'PAYMENT', daily_rent, 'CASH', 'free-sunday-test', 'test', ?::timestamptz from contracts where driver_id = ?",
                "2026-11-21T10:00:00Z", linkedDriver);
    }

    @Test
    void unpaidSaturdayRentAtNoonSharpGivesUNPAID_DAYSWhileTheCachedCountIsStillZero() {
        validAgreement();
        saturdayIsTheFirstRentDay();

        assertEquals(0, jdbc.queryForObject("select unpaid_days from drivers where id = ?", Integer.class, linkedDriver));
        assertRefused(check(linkedDriver, at("2026-11-22T12:00:00")), EligibilityReason.UNPAID_DAYS);
    }

    @Test
    void saturdayRentPaidAtNoonSharpIsEligible() {
        validAgreement();
        saturdayIsTheFirstRentDay();
        saturdayRentPaid();

        assertEquals(EligibilityReason.ELIGIBLE, check(linkedDriver, at("2026-11-22T12:00:00")).reason());
    }

    @Test
    void aDriverWithoutAnActiveContractHasNoUnpaidDays() {
        validAgreement();
        jdbc.update("delete from contracts where driver_id = ?", linkedDriver);

        assertEquals(EligibilityReason.ELIGIBLE, check(linkedDriver, SUNDAY_NOON).reason());
    }

    @Test
    void theOrderPutsTheAgreementBeforeTheDriverAndTheDriverBeforeTheWindow() {
        FreeChargingAgreement agreement = validAgreement();
        jdbc.update("update drivers set status = 'SUSPENDED' where id = ?", linkedDriver);
        Instant saturday = at("2026-11-21T15:00:00");

        assertRefused(check(linkedDriver, saturday), EligibilityReason.DRIVER_SUSPENDED);
        inGabon(() -> agreements.suspend(agreement.getId()));
        assertRefused(check(linkedDriver, saturday), EligibilityReason.AGREEMENT_SUSPENDED);
    }

    @Test
    void theAnswerCarriesTheWindowSoClientsCanShowIt() {
        validAgreement();

        Eligibility answer = check(linkedDriver, SUNDAY_NOON);

        assertEquals(DayOfWeek.SUNDAY, answer.window().day());
        assertEquals("12:00", answer.window().from());
        assertEquals("22:00", answer.window().until());
        assertFalse(answer.window().endOfDay());
    }

    @Test
    void aRefusedAnswerOfAKnownDriverAlsoCarriesTheWindow() {
        Eligibility answer = check(linkedDriver, SUNDAY_NOON);

        assertEquals(EligibilityReason.NO_AGREEMENT, answer.reason());
        assertNotNull(answer.window());
        assertEquals("12:00", answer.window().from());
    }

    @Test
    void anUnknownCustomerGetsNoWindow() {
        assertNull(inGabon(() -> eligibility.check("nobody", SUNDAY_NOON)).window());
    }

    @Test
    void theWholeDayWindowIsEligibleFromMidnightAndEndsAtTheNextMidnight() {
        validAgreement();
        inGabon(() -> windows.update(DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, null));

        Eligibility early = check(linkedDriver, at("2026-11-22T00:00:00"));
        Eligibility late = check(linkedDriver, at("2026-11-22T23:59:59"));

        assertTrue(early.eligible());
        assertTrue(late.eligible());
        assertEquals(at("2026-11-23T00:00:00"), late.windowEndsAt());
        assertEquals(SUNDAY, late.sundayDate());
        assertTrue(late.window().endOfDay());
        assertEquals("23:59", late.window().until());
        assertEquals(EligibilityReason.OUTSIDE_WINDOW, check(linkedDriver, at("2026-11-23T00:00:00")).reason());
    }

    @Test
    void aChangedWindowAppliesToTheNextAnswer() {
        validAgreement();
        assertEquals(EligibilityReason.OUTSIDE_WINDOW, check(linkedDriver, at("2026-11-22T09:00:00")).reason());

        inGabon(() -> windows.update(DayOfWeek.SUNDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)));

        assertTrue(check(linkedDriver, at("2026-11-22T09:00:00")).eligible());
    }
}
