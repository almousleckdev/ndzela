package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.payment.application.model.ReceiptData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

// What the public page of a receipt shows: the payment and the days it settled, with the phone masked and no contact details of the driver
public record PublicReceiptResponse(
        String reference,
        OffsetDateTime paidAt,
        String operator,
        String maskedPhone,
        String payerName,
        String contractNumber,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal total,
        String currency,
        String beneficiaryName,
        boolean cancelled,
        List<PaidDayView> days,
        BigDecimal creditKept) {

    public static PublicReceiptResponse of(ReceiptData r) {
        BigDecimal fee = r.fee() == null ? BigDecimal.ZERO : r.fee().setScale(0, RoundingMode.CEILING);
        return new PublicReceiptResponse(r.reference(), r.paidAt().toOffsetDateTime(), r.operatorLabel(), r.maskedPhone(), r.payerName(), r.contractNumber(),
                r.amount(), fee, r.amount().add(fee), r.currency(), r.beneficiaryName(), r.cancelled(),
                r.days().stream().map(d -> new PaidDayView(d.day(), d.kind(), d.amount().setScale(0, RoundingMode.HALF_UP), d.onTime())).toList(), r.creditKept());
    }
}
