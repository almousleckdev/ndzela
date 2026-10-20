package com.ndzela.admin.payment;

import com.ndzela.admin.payment.application.AccountCalculator;
import com.ndzela.admin.payment.application.model.AccountEntry;
import com.ndzela.admin.payment.application.model.AccountIncident;
import com.ndzela.admin.payment.application.model.AccountInput;
import com.ndzela.admin.payment.application.model.AccountLeave;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.domain.LedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

// Shared setup and helpers.
abstract class AbstractAccountCalculatorTest {
    protected static final ZoneId ZONE = ZoneId.of("Africa/Libreville");
    protected static final BigDecimal RENT = new BigDecimal("23000");
    protected static final LocalDate START = LocalDate.of(2026, 9, 30);

    protected static Instant at(String local) {
        return LocalDateTime.parse(local).atZone(ZONE).toInstant();
    }

    protected static AccountEntry payment(String local, long amount) {
        return new AccountEntry(UUID.randomUUID(), LedgerEntryType.PAYMENT, BigDecimal.valueOf(amount), at(local), null, null);
    }

    protected static AccountState stateAt(String now, List<AccountLeave> leaves, List<AccountEntry> entries) {
        return AccountCalculator.compute(new AccountInput(START, START.plusDays(2000), null, RENT, leaves, List.of(), entries, ZONE, at(now)));
    }

    protected static AccountState stateWith(String now, List<AccountIncident> incidents, AccountEntry... entries) {
        return AccountCalculator.compute(new AccountInput(START, START.plusDays(2000), null, RENT, List.of(), incidents, List.of(entries), ZONE, at(now)));
    }

    protected static AccountState stateAt(String now, AccountEntry... entries) {
        return stateAt(now, List.of(), List.of(entries));
    }
}
