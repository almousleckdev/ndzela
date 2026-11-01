package com.ndzela.admin.payment;

import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.domain.ContractStatus;
import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.payment.api.dto.ContractProgress;
import com.ndzela.admin.payment.application.ContractProgressCalculator;
import com.ndzela.admin.payment.application.model.AccountState;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractProgressCalculatorTest {
    private static final LocalDate START = LocalDate.of(2026, 10, 15);

    private static ContractResponse contract() {
        return new ContractResponse(UUID.randomUUID(), "NDZ-1", ContractStatus.ACTIVE, UUID.randomUUID(), UUID.randomUUID(), START, LocalDate.of(2030, 10, 10), 1248,
                new BigDecimal("23000"), new BigDecimal("500000"), new BigDecimal("28704000"), 0, null, null, null, null);
    }

    private static AccountState state(int onTime, int late, int unpaid, String paid, String waived) {
        return new AccountState(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, null, ArrearsLevel.NONE, List.of(), onTime, late, unpaid, 0, BigDecimal.ZERO, List.of(),
                new BigDecimal(paid), new BigDecimal(waived));
    }

    @Test
    void countsTheRentDaysFromTheFirstDayWithoutSundays() {
        // Oct 16 (Fri) to Oct 24 (Sat), 2026: Fri, Sat, Mon..Sat = 8 rent days, the two Sundays (18, 25) excluded
        ContractProgress progress = ContractProgressCalculator.of(contract(), state(0, 0, 0, "0", "0"), LocalDate.of(2026, 10, 24));

        assertEquals(1248, progress.totalDays());
        assertEquals(8, progress.elapsedDays());
        assertEquals(1240, progress.daysLeft());
    }

    @Test
    void theMoneyReceivedReducesWhatRemainsOfTheTotalPrice() {
        ContractProgress progress = ContractProgressCalculator.of(contract(), state(5, 1, 2, "138000", "23000"), LocalDate.of(2026, 10, 24));

        assertEquals(6, progress.paidDays());
        assertEquals(2, progress.unpaidDays());
        assertEquals(0, new BigDecimal("138000").compareTo(progress.paidAmount()));
        assertEquals(0, new BigDecimal("28543000").compareTo(progress.remainingAmount()));
        assertEquals(0, new BigDecimal("500000").compareTo(progress.deposit()));
    }

    @Test
    void nothingIsLeftToPayOnceTheTotalPriceIsReached() {
        ContractProgress progress = ContractProgressCalculator.of(contract(), state(1248, 0, 0, "28704000", "0"), LocalDate.of(2030, 10, 10));

        assertEquals(0, progress.daysLeft());
        assertEquals(0, BigDecimal.ZERO.compareTo(progress.remainingAmount()));
    }

    @Test
    void daysNeverGoPastTheContractEnd() {
        ContractProgress progress = ContractProgressCalculator.of(contract(), state(0, 0, 0, "0", "0"), LocalDate.of(2035, 1, 1));

        assertEquals(1248, progress.elapsedDays());
        assertEquals(0, progress.daysLeft());
    }
}
