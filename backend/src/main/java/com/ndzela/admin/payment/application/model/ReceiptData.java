package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.country.application.CountryBrand;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

// Everything printed on a payment receipt, gathered before the PDF is drawn.
public record ReceiptData(
        String reference,
        String providerReference,
        ZonedDateTime paidAt,
        String operatorLabel,
        String maskedPhone,
        String payerName,
        String contractNumber,
        String vehicleNumber,
        BigDecimal amount,
        BigDecimal fee,
        String currency,
        String beneficiaryName,
        String beneficiaryAddress,
        String beneficiaryPhone,
        String beneficiaryEmail,
        CountryBrand brand,
        List<PaidDay> days,
        BigDecimal creditKept,
        boolean cancelled,
        String verifyUrl) {}
