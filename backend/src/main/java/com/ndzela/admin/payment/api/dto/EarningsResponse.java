package com.ndzela.admin.payment.api.dto;

import java.math.BigDecimal;
import java.util.List;

// Everything collected over a period: rent payments not reversed, the provider fees, and what is left; per month, current and former drivers apart.
public record EarningsResponse(BigDecimal collected, BigDecimal fees, BigDecimal net, long payingDrivers, BigDecimal collectedCurrent, BigDecimal collectedFormer, List<MonthTotal> months) {

    // month is "yyyy-MM" in the country's time zone
    public record MonthTotal(String month, BigDecimal collected, BigDecimal fees, BigDecimal net) {}
}
