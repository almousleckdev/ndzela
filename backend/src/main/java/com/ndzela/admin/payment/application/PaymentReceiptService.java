package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.country.application.CountryBrand;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.payment.api.dto.OperatorResponse;
import com.ndzela.admin.payment.application.model.PaymentReceiptFile;
import com.ndzela.admin.payment.application.model.ReceiptData;
import com.ndzela.admin.payment.domain.PaymentIntent;
import com.ndzela.admin.payment.domain.PaymentIntentStatus;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.UUID;

// The receipt of a confirmed payment, drawn on the server so it cannot be altered on the phone.
@Service
@RequiredArgsConstructor
public class PaymentReceiptService {
    private final PaymentIntentStore store;
    private final DriverArrearsService driverArrears;
    private final ContractService contracts;
    private final CountryRepository countries;
    private final PaymentOperatorService operators;

    @Transactional(readOnly = true)
    public PaymentReceiptFile receiptOf(UUID driverId, UUID intentId) {
        PaymentIntent intent = store.findOf(driverId, intentId).orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        if (intent.getStatus() != PaymentIntentStatus.SUCCESS) {
            throw new BusinessRuleViolationException("RECEIPT_NOT_AVAILABLE", "A receipt exists only for a confirmed payment.");
        }
        Country country = countries.findById(TenantHolder.requireCountry()).orElseThrow();
        ZoneId zone = ZoneId.of(country.getTimezone());
        DriverRef driver = driverArrears.get(driverId);
        ReceiptData data = new ReceiptData(
                intent.getReference(), intent.getProviderReference(),
                intent.getCreatedAt().atZoneSameInstant(zone),
                (intent.getCompletedAt() == null ? intent.getCreatedAt() : intent.getCompletedAt()).atZoneSameInstant(zone),
                operatorLabel(intent.getOperatorCode()), intent.getCustomerPhone(),
                driver.firstName() + " " + driver.lastName(), contracts.get(intent.getContractId()).contractNumber(),
                intent.getAmount(), intent.getFeeAmount(), intent.getCurrencyCode(),
                orDefault(country.getLegalName(), CountryBrand.ofCountry(country.getCode()).name()), country.getLegalForm(), country.getHeadOffice(), country.getRegistryNumber(),
                country.getLegalPhone(), country.getLegalEmail(), CountryBrand.ofCountry(country.getCode()));
        return new PaymentReceiptFile("recu-" + intent.getReference() + ".pdf", PaymentReceiptPdf.render(data));
    }

    private String operatorLabel(String code) {
        return operators.listActive().stream().filter(operator -> operator.code().equals(code)).map(OperatorResponse::label).findFirst().orElse(code);
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
