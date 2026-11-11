package com.ndzela.admin.charging;

import com.ndzela.admin.charging.application.HelpScore;
import com.ndzela.admin.charging.application.HelpScorer;
import com.ndzela.admin.charging.application.HelpSession;
import com.ndzela.admin.charging.application.HelpSignals;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Signs that one account charged two cars. They raise a case for staff; none proves anything alone.
class HelpSignalsTest {
    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-11-11T08:00:00Z");

    private static HelpSession session(int startHour, int endHour, String chargePoint, int connector, Integer socBefore, Integer socAfter, double kwh) {
        return new HelpSession(T0.plusHours(startHour), T0.plusHours(endHour), chargePoint, connector, socBefore == null ? null : BigDecimal.valueOf(socBefore),
                socAfter == null ? null : BigDecimal.valueOf(socAfter), BigDecimal.valueOf(kwh));
    }

    @Test
    void twoSessionsAtTwoConnectorsAtTheSameTimeOverlap() {
        assertTrue(HelpSignals.overlap(List.of(session(0, 3, "CP-1", 1, 20, 90, 20), session(1, 2, "CP-2", 1, 30, 60, 10))));
    }

    @Test
    void backToBackSessionsDoNotOverlap() {
        assertFalse(HelpSignals.overlap(List.of(session(0, 2, "CP-1", 1, 20, 90, 20), session(2, 4, "CP-2", 1, 30, 60, 10))));
    }

    @Test
    void twoOverlappingSessionsOnTheSameConnectorAreNotTwoCars() {
        assertFalse(HelpSignals.overlap(List.of(session(0, 3, "CP-1", 1, 20, 90, 20), session(1, 2, "CP-1", 1, 30, 60, 10))));
    }

    @Test
    void aSessionStillRunningOverlapsALaterOneAtAnotherConnector() {
        HelpSession running = new HelpSession(T0, null, "CP-1", 1, BigDecimal.TEN, null, BigDecimal.ONE);

        assertTrue(HelpSignals.overlap(List.of(running, session(2, 3, "CP-2", 1, 30, 60, 10))));
    }

    @Test
    void endAt95ThenStartAt30OneHourLaterBreaksTheChain() {
        assertTrue(HelpSignals.batteryChainBroken(List.of(session(0, 1, "CP-1", 1, 20, 95, 20), session(2, 3, "CP-2", 1, 30, 80, 15))));
    }

    @Test
    void endAt95ThenStartAt70AfterTwoHoursOfDrivingIsFine() {
        assertFalse(HelpSignals.batteryChainBroken(List.of(session(0, 1, "CP-1", 1, 20, 95, 20), session(3, 4, "CP-2", 1, 70, 90, 8))));
    }

    @Test
    void aBatteryThatRoseWithoutChargingBreaksTheChain() {
        assertTrue(HelpSignals.batteryChainBroken(List.of(session(0, 1, "CP-1", 1, 20, 40, 8), session(2, 3, "CP-2", 1, 80, 95, 6))));
    }

    @Test
    void aMissingBatteryValueNeverBreaksTheChain() {
        assertFalse(HelpSignals.batteryChainBroken(List.of(session(0, 1, "CP-1", 1, 20, null, 20), session(2, 3, "CP-2", 1, 30, 80, 15))));
    }

    @Test
    void energyDoubleTheNormalIsFlaggedButOneAndAHalfIsNot() {
        List<HelpSession> eighty = List.of(session(0, 1, "CP-1", 1, null, null, 40), session(5, 6, "CP-1", 1, null, null, 40));
        List<HelpSession> fifty = List.of(session(0, 1, "CP-1", 1, null, null, 25), session(5, 6, "CP-1", 1, null, null, 25));

        assertTrue(HelpSignals.energyAboveNormal(eighty, BigDecimal.valueOf(40)));
        assertFalse(HelpSignals.energyAboveNormal(fifty, BigDecimal.valueOf(40)));
        assertFalse(HelpSignals.energyAboveNormal(eighty, null));
    }

    @Test
    void theScoreAddsTheSignalsAndOnlyFlagsAboveTheThreshold() {
        List<HelpSession> quiet = List.of(session(0, 1, "CP-1", 1, 20, 90, 20));
        List<HelpSession> overlapping = List.of(session(0, 3, "CP-1", 1, 20, 90, 20), session(1, 2, "CP-2", 1, 30, 60, 10));
        List<HelpSession> everything = List.of(session(0, 3, "CP-1", 1, 20, 95, 60), session(1, 2, "CP-2", 1, 30, 60, 60), session(5, 6, "CP-3", 1, 20, 80, 15));

        HelpScore none = HelpScorer.score(quiet, BigDecimal.valueOf(40));
        HelpScore one = HelpScorer.score(overlapping, BigDecimal.valueOf(40));
        HelpScore all = HelpScorer.score(everything, BigDecimal.valueOf(40));

        assertEquals(0, none.score());
        assertFalse(none.flagged());
        assertTrue(none.reasons().isEmpty());
        assertEquals(List.of("OVERLAP"), one.reasons());
        assertTrue(one.flagged());
        assertEquals(100, all.score());
        assertEquals(List.of("OVERLAP", "BATTERY_CHAIN", "ENERGY"), all.reasons());
    }
}
