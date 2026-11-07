package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.vehicle.application.VehicleService;
import com.ndzela.admin.country.application.CountryBrand;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.payment.api.dto.OperatorResponse;
import com.ndzela.admin.payment.api.dto.PaidDayView;
import com.ndzela.admin.payment.api.dto.PublicReceiptResponse;
import com.ndzela.admin.payment.application.model.PaidDay;
import com.ndzela.admin.payment.application.model.PaymentReceiptFile;
import com.ndzela.admin.payment.application.model.ReceiptData;
import com.ndzela.admin.payment.domain.LedgerEntry;
import com.ndzela.admin.payment.domain.LedgerEntryDay;
import com.ndzela.admin.payment.domain.LedgerEntryDayRepository;
import com.ndzela.admin.payment.domain.LedgerEntryRepository;
import com.ndzela.admin.payment.domain.LedgerEntryType;
import com.ndzela.admin.payment.domain.PaymentIntent;
import com.ndzela.admin.payment.domain.PaymentIntentStatus;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The receipt of a confirmed payment, drawn on the server so it cannot be altered on the phone.
@Service
@RequiredArgsConstructor
public class PaymentReceiptService {
    private final PaymentIntentStore store;
    private final LedgerEntryRepository ledger;
    private final LedgerEntryDayRepository entryDays;
    private final DriverArrearsService driverArrears;
    private final ContractService contracts;
    private final VehicleService vehicles;
    private final CountryRepository countries;
    private final PaymentOperatorService operators;

    @Value("${app.public-site.url:https://www.ndzela.com}")
    private String siteUrl;

    // A driver's own payment started from the app
    @Transactional(readOnly = true)
    public PaymentReceiptFile receiptOf(UUID driverId, UUID intentId) {
        PaymentIntent intent = store.findOf(driverId, intentId).orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        if (intent.getStatus() != PaymentIntentStatus.SUCCESS) {
            throw new BusinessRuleViolationException("RECEIPT_NOT_AVAILABLE", "A receipt exists only for a confirmed payment.");
        }
        LedgerEntry entry = ledger.findByRequestId(intentId).orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        return build(entry, intent);
    }

    // Any payment of the caller's country, for staff: by its ledger entry, or by the app attempt that made it
    @Transactional(readOnly = true)
    public PaymentReceiptFile receiptOfEntry(UUID id) {
        LedgerEntry entry = ledger.findById(id).filter(e -> e.getType() == LedgerEntryType.PAYMENT).or(() -> ledger.findByRequestId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        PaymentIntent intent = entry.getRequestId() == null ? null : store.find(entry.getRequestId()).orElse(null);
        return build(entry, intent);
    }

    // What the receipt's QR code shows to anyone who scans it: a payment of the caller's country, by its ledger entry id
    @Transactional(readOnly = true)
    public PublicReceiptResponse publicViewOf(UUID entryId) {
        LedgerEntry entry = ledger.findById(entryId).filter(e -> e.getType() == LedgerEntryType.PAYMENT)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        PaymentIntent intent = entry.getRequestId() == null ? null : store.find(entry.getRequestId()).orElse(null);
        return PublicReceiptResponse.of(dataOf(entry, intent));
    }

    // The days each app attempt settled, by attempt id (empty for an attempt not paid)
    @Transactional(readOnly = true)
    public Map<UUID, List<PaidDayView>> daysOfAttempts(Collection<UUID> intentIds) {
        if (intentIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> attemptOfEntry = new HashMap<>();
        ledger.findAllByRequestIdIn(intentIds).forEach(entry -> attemptOfEntry.put(entry.getId(), entry.getRequestId()));
        Map<UUID, List<PaidDayView>> days = new HashMap<>();
        entryDays.findAllByLedgerEntryIdIn(attemptOfEntry.keySet()).stream()
                .sorted(Comparator.comparing(LedgerEntryDay::getBusinessDay).thenComparing(LedgerEntryDay::getKind))
                .forEach(d -> days.computeIfAbsent(attemptOfEntry.get(d.getLedgerEntryId()), id -> new ArrayList<>())
                        .add(new PaidDayView(d.getBusinessDay(), d.getKind(), d.getAmount().setScale(0, RoundingMode.HALF_UP), d.isPaidOnTime())));
        return days;
    }

    private PaymentReceiptFile build(LedgerEntry entry, PaymentIntent intent) {
        ReceiptData data = dataOf(entry, intent);
        return new PaymentReceiptFile("recu-" + data.reference() + ".pdf", PaymentReceiptPdf.render(data));
    }

    private ReceiptData dataOf(LedgerEntry entry, PaymentIntent intent) {
        Country country = countries.findById(TenantHolder.requireCountry()).orElseThrow();
        ZoneId zone = ZoneId.of(country.getTimezone());
        DriverRef driver = driverArrears.get(entry.getDriverId());
        List<PaidDay> days = entryDays.findAllByLedgerEntryIdOrderByBusinessDayAscKindAsc(entry.getId()).stream()
                .map(d -> new PaidDay(d.getBusinessDay(), d.getKind(), d.getAmount(), d.isPaidOnTime())).toList();
        BigDecimal settled = days.stream().map(PaidDay::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        ContractResponse contract = contracts.get(entry.getContractId());
        String reference = intent != null ? intent.getReference() : entry.getReference();
        return new ReceiptData(
                reference, intent != null ? intent.getProviderReference() : null,
                entry.getCreatedAt().atZoneSameInstant(zone),
                operatorLabel(intent != null ? intent.getOperatorCode() : entry.getMethod().name()),
                ReceiptTicketStyle.maskPhone(intent != null ? intent.getCustomerPhone() : null),
                driver.firstName() + " " + driver.lastName(), contract.contractNumber(), vehicles.get(contract.vehicleId()).fleetNumber(),
                entry.getAmount(), intent != null ? intent.getFeeAmount() : null, country.getCurrencyCode(),
                orDefault(country.getLegalName(), CountryBrand.ofCountry(country.getCode()).name()), country.getHeadOffice(),
                country.getLegalPhone(), country.getLegalEmail(), CountryBrand.ofCountry(country.getCode()),
                days, entry.getAmount().subtract(settled).max(BigDecimal.ZERO), ledger.existsByReversesEntryId(entry.getId()),
                siteUrl.replaceAll("/+$", "") + "/recu/" + entry.getId());
    }

    private String operatorLabel(String code) {
        return operators.listActive().stream().filter(operator -> operator.code().equals(code)).map(OperatorResponse::label).findFirst().orElse(code);
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
