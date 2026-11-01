package com.ndzela.admin.payment.application;

import com.ndzela.admin.payment.application.model.AccountCharge;
import com.ndzela.admin.payment.application.model.AccountDay;
import com.ndzela.admin.payment.application.model.AccountEntry;
import com.ndzela.admin.payment.application.model.AccountIncident;
import com.ndzela.admin.payment.application.model.AccountInput;
import com.ndzela.admin.payment.application.model.AccountLeave;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.domain.DayStatus;
import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.payment.domain.DayIncidentKind;
import com.ndzela.admin.payment.domain.LedgerEntryType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// The state of one contract's account, computed from its terms.
public final class AccountCalculator {
    static final LocalTime DEADLINE = LocalTime.NOON;
    static final Duration URGENT = Duration.ofHours(48);

    private AccountCalculator() {}

    public static AccountState compute(AccountInput in) {
        Set<UUID> reversed = new HashSet<>();
        in.entries().stream().filter(e -> e.type() == LedgerEntryType.REVERSAL && e.reverses() != null).forEach(e -> reversed.add(e.reverses()));
        List<AccountEntry> credits = in.entries().stream().filter(e -> (e.type() == LedgerEntryType.PAYMENT || e.type() == LedgerEntryType.WAIVER) && !reversed.contains(e.id()))
                .sorted(Comparator.comparing(AccountEntry::at)).toList();

        List<AccountCharge> charges = new ArrayList<>();
        List<AccountCharge> awaiting = new ArrayList<>();
        LocalDate today = in.now().atZone(in.zone()).toLocalDate();
        LocalDate last = min(in.contractEnd(), in.endedOn() == null ? today : in.endedOn(), today);
        for (LocalDate day = in.contractStart().plusDays(1); !day.isAfter(last); day = day.plusDays(1)) {
            if (day.getDayOfWeek() == DayOfWeek.SUNDAY || onLeave(in.leaves(), day)) {
                continue;
            }
            BigDecimal rent = rentOf(day, in);
            if (rent.signum() == 0) {
                continue;
            }
            Instant due = dueAt(day, in.zone());
            if (!due.isAfter(in.now())) {
                charges.add(new AccountCharge(day, due, rent, false));
            } else if (day.isBefore(today)) {
                awaiting.add(new AccountCharge(day, due, rent, false)); // the day has ended, its deadline has not come
            }
        }
        for (AccountEntry e : in.entries()) {
            if (e.type() == LedgerEntryType.PENALTY && e.businessDay() != null) {
                charges.add(new AccountCharge(e.businessDay(), dueAt(e.businessDay(), in.zone()).plus(URGENT), e.amount(), true));
            }
        }
        // A day's penalty is settled right after its rent, so paying a day pays its penalty too.
        charges.sort(Comparator.comparing(AccountCharge::day).thenComparing(AccountCharge::penalty));

        BigDecimal totalCredit = credits.stream().map(AccountEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<AccountDay> days = new ArrayList<>();
        List<LocalDate> penaltyDays = new ArrayList<>();
        List<AccountCharge> unpaidCharges = new ArrayList<>();
        Set<LocalDate> penalised = new HashSet<>();
        in.entries().stream().filter(e -> e.type() == LedgerEntryType.PENALTY).forEach(e -> penalised.add(e.businessDay()));
        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal covered = BigDecimal.ZERO;
        BigDecimal overdue = BigDecimal.ZERO;
        Instant oldest = null;
        int onTime = 0;
        int late = 0;
        int unpaid = 0;
        for (AccountCharge charge : charges) {
            cumulative = cumulative.add(charge.amount());
            Instant settled = settlementTime(credits, cumulative);
            if (settled != null) {
                covered = cumulative;
            } else {
                overdue = overdue.add(charge.amount());
                unpaidCharges.add(charge);
                oldest = oldest == null || charge.due().isBefore(oldest) ? charge.due() : oldest;
            }
            if (charge.penalty()) {
                continue;
            }
            DayStatus status = settled == null ? DayStatus.UNPAID : settled.isAfter(charge.due()) ? DayStatus.PAID_LATE : DayStatus.PAID_ON_TIME;
            days.add(new AccountDay(charge.day(), charge.due(), charge.amount(), status, settled));
            switch (status) {
                case PAID_ON_TIME -> onTime++;
                case PAID_LATE -> late++;
                case UNPAID -> {
                    unpaid++;
                    if (!in.now().isBefore(charge.due().plus(URGENT)) && !penalised.contains(charge.day())) {
                        penaltyDays.add(charge.day());
                    }
                }
            }
        }
        // Rent that is owed but not late yet comes after everything already due: credits left over cover it first.
        int awaitingDays = 0;
        BigDecimal awaitingAmount = BigDecimal.ZERO;
        BigDecimal ahead = cumulative;
        for (AccountCharge charge : awaiting) {
            ahead = ahead.add(charge.amount());
            if (settlementTime(credits, ahead) == null) {
                awaitingDays++;
                awaitingAmount = awaitingAmount.add(charge.amount());
            }
        }
        return new AccountState(days, overdue, totalCredit.subtract(covered).max(BigDecimal.ZERO), oldest, levelOf(unpaid), penaltyDays, onTime, late, unpaid,
                awaitingDays, awaitingAmount, List.copyOf(unpaidCharges), creditsOf(credits, LedgerEntryType.PAYMENT), creditsOf(credits, LedgerEntryType.WAIVER));
    }

    private static BigDecimal creditsOf(List<AccountEntry> credits, LedgerEntryType type) {
        return credits.stream().filter(e -> e.type() == type).map(AccountEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Like the previous platform's screen: one unpaid day is a reminder, two are urgent, three or more are critical (suspension).
    static ArrearsLevel levelOf(int unpaidDays) {
        if (unpaidDays <= 0) return ArrearsLevel.NONE;
        if (unpaidDays == 1) return ArrearsLevel.YELLOW;
        if (unpaidDays == 2) return ArrearsLevel.DARK_YELLOW;
        return ArrearsLevel.RED;
    }

    // A stop means no rent that day, a half day means half of it; any other day is the full rent.
    private static BigDecimal rentOf(LocalDate day, AccountInput in) {
        for (AccountIncident incident : in.incidents()) {
            if (incident.day().equals(day)) {
                return incident.kind() == DayIncidentKind.STOP ? BigDecimal.ZERO : in.dailyRent().divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP);
            }
        }
        return in.dailyRent();
    }

    static Instant dueAt(LocalDate day, ZoneId zone) {
        return day.plusDays(1).atTime(DEADLINE).atZone(zone).toInstant();
    }

    // When the credits first covered the given cumulative charges, or null when they never did.
    private static Instant settlementTime(List<AccountEntry> credits, BigDecimal cumulativeCharge) {
        BigDecimal running = BigDecimal.ZERO;
        for (AccountEntry credit : credits) {
            running = running.add(credit.amount());
            if (running.compareTo(cumulativeCharge) >= 0) {
                return credit.at();
            }
        }
        return null;
    }

    private static boolean onLeave(List<AccountLeave> leaves, LocalDate day) {
        return leaves.stream().anyMatch(l -> !day.isBefore(l.from()) && !day.isAfter(l.to()));
    }

    private static LocalDate min(LocalDate a, LocalDate b, LocalDate c) {
        return a.isBefore(b) ? (a.isBefore(c) ? a : c) : (b.isBefore(c) ? b : c);
    }
}
