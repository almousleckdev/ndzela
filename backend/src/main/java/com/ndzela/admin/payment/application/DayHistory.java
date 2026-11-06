package com.ndzela.admin.payment.application;

import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.application.model.DayHistoryEntry;
import com.ndzela.admin.payment.application.model.DayHistoryStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// The rent days of an account from `since` on, oldest first, each paid, unpaid or awaiting its deadline
public final class DayHistory {
    private DayHistory() {}

    public static List<DayHistoryEntry> of(AccountState state, LocalDate since) {
        List<DayHistoryEntry> entries = new ArrayList<>();
        state.days().forEach(d -> entries.add(new DayHistoryEntry(d.day(), d.amount(), DayHistoryStatus.valueOf(d.status().name()), d.settledAt())));
        state.awaitingCharges().forEach(c -> entries.add(new DayHistoryEntry(c.day(), c.amount(), DayHistoryStatus.AWAITING, null)));
        return entries.stream().filter(e -> !e.day().isBefore(since)).sorted(Comparator.comparing(DayHistoryEntry::day)).toList();
    }
}
