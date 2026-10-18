package com.ndzela.admin.payment;

import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.payment.application.PaymentWeek;
import com.ndzela.admin.payment.application.model.AccountState;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeekSettlementTest {
    private static final ZoneId ZONE = ZoneId.of("Africa/Libreville");

    private static Instant at(String local) {
        return LocalDateTime.parse(local).atZone(ZONE).toInstant();
    }

    private static OffsetDateTime sundayNoon(String date) {
        return LocalDateTime.parse(date + "T12:00:00").atZone(ZONE).toOffsetDateTime();
    }

    private static AccountState state(int unpaid, int awaiting) {
        return new AccountState(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, null, ArrearsLevel.NONE, List.of(), 0, 0, unpaid, awaiting, BigDecimal.ZERO, List.of());
    }

    @Test
    void theWeekClosesOnTheComingSundayAtNoon() {
        assertEquals(sundayNoon("2026-10-18"), PaymentWeek.closesAt(at("2026-10-14T10:00:00"), ZONE));
    }

    @Test
    void onSundayBeforeNoonTheWeekClosesTheSameDay() {
        assertEquals(sundayNoon("2026-10-18"), PaymentWeek.closesAt(at("2026-10-18T11:59:00"), ZONE));
    }

    @Test
    void fromSundayNoonTheNextWeekIsTheOneThatCloses() {
        assertEquals(sundayNoon("2026-10-25"), PaymentWeek.closesAt(at("2026-10-18T12:00:00"), ZONE));
        assertEquals(sundayNoon("2026-10-25"), PaymentWeek.closesAt(at("2026-10-18T12:01:00"), ZONE));
    }

    @Test
    void theWeekIsSettledOnlyWhenNothingIsUnpaidOrAwaiting() {
        assertTrue(PaymentWeek.settled(state(0, 0)));
        assertFalse(PaymentWeek.settled(state(1, 0)));
        assertFalse(PaymentWeek.settled(state(0, 1)));
    }
}
