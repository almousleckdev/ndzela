package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.country.application.CountryBrand;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

// Everything printed on a payment receipt, gathered before the PDF is drawn.
public record ReceiptData(
        String reference,
        String providerReference,
        ZonedDateTime startedAt,
        ZonedDateTime paidAt,
        String operatorLabel,
        String customerPhone,
        String payerName,
        String contractNumber,
        BigDecimal amount,
        BigDecimal fee,
        String currency,
        String beneficiaryName,
        String beneficiaryForm,
        String beneficiaryAddress,
        String beneficiaryRegistry,
        String beneficiaryPhone,
        String beneficiaryEmail,
        CountryBrand brand) {}
