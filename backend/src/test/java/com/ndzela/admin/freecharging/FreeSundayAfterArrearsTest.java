package com.ndzela.admin.freecharging;

import com.ndzela.admin.freecharging.application.Eligibility;
import com.ndzela.admin.freecharging.application.FreeChargingEligibility;
import com.ndzela.admin.freecharging.application.FreeChargingWindowService;
import com.ndzela.admin.freecharging.domain.EligibilityReason;
import com.ndzela.admin.freecharging.domain.FreeChargingAgreement;
import com.ndzela.admin.payment.application.ArrearsService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The 3-unpaid-day lock (ADR-0033) and the free Sunday charge (ADR-0048): the arrears job suspends the driver, which
// removes the benefit; paying every day and the next job run restore the driver, and the benefit comes back in the window.
class FreeSundayAfterArrearsTest extends AbstractFreeChargingTest {
    private static final ZoneId ZONE = ZoneId.of("Africa/Libreville");
    private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 22);

    @Autowired private FreeChargingEligibility eligibility;
    @Autowired private ArrearsService arrears;
    @Autowired private FreeChargingWindowService windows;

    private static Instant at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(ZONE).toInstant();
    }

    private Eligibility check(Instant now) {
        return inGabon(() -> eligibility.check("cust-" + linkedDriver, now));
    }

    private String driverColumn(String column) {
        return jdbc.queryForObject("select " + column + " from drivers where id = ?", String.class, linkedDriver);
    }

    // Rent days Thursday, Friday and Saturday: due Friday, Saturday and Sunday at 12:00, so three are late on Sunday afternoon.
    private void threeRentDaysBeforeSunday() {
        jdbc.update("update contracts set start_date = date '2026-11-18', end_date = date '2027-11-18' where driver_id = ?", linkedDriver);
    }

    private void payEveryDayAt(String localDateTime) {
        jdbc.update("insert into ledger_entries (id, country_id, contract_id, driver_id, entry_type, amount, method, reference, recorded_by, created_at) "
                + "select gen_random_uuid(), country_id, id, driver_id, 'PAYMENT', daily_rent * 3, 'CASH', 'free-sunday-arrears-test', 'test', ?::timestamptz "
                + "from contracts where driver_id = ?", at(localDateTime).toString(), linkedDriver);
    }

    @Test
    void aDriverSuspendedForThreeUnpaidDaysLosesTheFreeSundayAndGetsItBackOnceEverythingIsPaidAndRestored() {
        inGabon(() -> windows.update(DayOfWeek.SUNDAY, LocalTime.of(12, 0), LocalTime.of(22, 0)));
        FreeChargingAgreement agreement = inGabon(() -> agreements.create(linkedDriver, SUNDAY.minusDays(30), null, null));
        threeRentDaysBeforeSunday();

        assertEquals(EligibilityReason.UNPAID_DAYS, check(at("2026-11-22T12:30:00")).reason());

        arrears.evaluateAll(at("2026-11-22T12:30:00"));
        assertEquals("SUSPENDED", driverColumn("status"));
        assertEquals("ARREARS", driverColumn("suspension_cause"));
        Eligibility suspended = check(at("2026-11-22T12:31:00"));
        assertFalse(suspended.eligible());
        assertEquals(EligibilityReason.DRIVER_SUSPENDED, suspended.reason());

        payEveryDayAt("2026-11-22T12:40:00");
        assertEquals(EligibilityReason.DRIVER_SUSPENDED, check(at("2026-11-22T12:41:00")).reason());

        arrears.evaluateAll(at("2026-11-22T12:45:00"));
        assertEquals("ACTIVE", driverColumn("status"));
        assertEquals(1, auditRows("DRIVER_REACTIVATED"));

        Eligibility restored = check(at("2026-11-22T13:00:00"));
        assertTrue(restored.eligible());
        assertEquals(EligibilityReason.ELIGIBLE, restored.reason());
        assertEquals(agreement.getId(), restored.agreementId());
        assertEquals(SUNDAY, restored.sundayDate());
        assertEquals(at("2026-11-22T22:00:00"), restored.windowEndsAt());
    }
}
