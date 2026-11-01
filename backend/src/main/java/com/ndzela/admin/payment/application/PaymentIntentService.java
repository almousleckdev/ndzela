package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ProviderUnavailableException;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.country.application.CountryBrand;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.infrastructure.pvit.PvitClient;
import com.ndzela.admin.infrastructure.pvit.PvitException;
import com.ndzela.admin.infrastructure.pvit.PvitFees;
import com.ndzela.admin.infrastructure.pvit.PvitPaymentRequest;
import com.ndzela.admin.infrastructure.pvit.PvitPaymentStarted;
import com.ndzela.admin.payment.api.dto.PaymentQuote;
import com.ndzela.admin.payment.application.model.PayableDays;
import com.ndzela.admin.payment.application.model.PaymentDue;
import com.ndzela.admin.payment.application.model.PaymentOption;
import com.ndzela.admin.payment.domain.PaymentIntent;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Starts and quotes a driver's rent payment
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentIntentService {
    private final PaymentIntentStore store;
    private final PaymentOperatorService operators;
    private final AccountQueryService accountQueries;
    private final CountryRepository countries;
    private final PvitClient pvit;
    private final AppProperties props;

    // Deliberately not transactional: PVit is called between two short transactions.
    public PaymentIntent start(UUID driverId, String operatorCode, String rawPhone, Integer days) {
        operators.requireActive(operatorCode);
        Country country = country();
        String phone = ProviderPhone.forProvider(country.getPhoneCode(), rawPhone);
        PaymentDue due = dueOf(driverId, days);
        PaymentIntent intent = store.createPending(driverId, due.contractId(), operatorCode, phone, due.toCollect(), country.getCurrencyCode(), due.days());
        try {
            PvitPaymentStarted started = pvit.startPayment(new PvitPaymentRequest(intent.getReference(), intent.getAmount(), operatorCode, phone, "Loyer " + CountryBrand.ofCountry(country.getCode()).name()));
            store.markStarted(intent.getId(), started.providerReference());
        } catch (PvitException e) {
            log.warn("PVit refused or could not start payment {}: {}", intent.getReference(), e.getMessage());
            store.markFailed(intent.getId(), "The payment could not be started.");
            throw new ProviderUnavailableException("The payment service is not available right now. Try again in a moment.");
        }
        return store.find(intent.getId()).orElseThrow();
    }

    // What paying would cost
    public PaymentQuote quote(UUID driverId, String operatorCode, String rawPhone, Integer days) {
        operators.requireActive(operatorCode);
        Country country = country();
        PaymentDue due = dueOf(driverId, days);
        try {
            PvitFees fees = pvit.fees(operatorCode, due.toCollect());
            String holder = rawPhone == null || rawPhone.isBlank() ? null : pvit.accountHolder(operatorCode, ProviderPhone.forProvider(country.getPhoneCode(), rawPhone));
            return new PaymentQuote(due.days(), due.toCollect(), fees.fees(), country.getCurrencyCode(), holder, due.options());
        } catch (PvitException e) {
            throw new ProviderUnavailableException("The payment service is not available right now. Try again in a moment.");
        }
    }

    private PaymentDue dueOf(UUID driverId, Integer days) {
        PayableDays payable = accountQueries.payable(driverId);
        if (payable.contractId() == null) {
            throw new BusinessRuleViolationException("NO_ACTIVE_CONTRACT", "This driver has no active contract.");
        }
        PaymentOption chosen = PaymentSelection.choose(payable.options(), days);
        BigDecimal amount = chosen.amount().setScale(0, RoundingMode.HALF_UP);
        return new PaymentDue(payable.contractId(), props.pvit().sandbox() ? props.pvit().sandboxAmount() : amount, chosen.days(), payable.options());
    }

    // Who receives the money
    public String beneficiary() {
        String name = country().getLegalName();
        return name == null || name.isBlank() ? CountryBrand.ofCountry(country().getCode()).name() : name;
    }

    private Country country() {
        return countries.findById(TenantHolder.requireCountry()).orElseThrow();
    }

    public Optional<PaymentIntent> attemptOf(UUID driverId, UUID id) {
        return store.findOf(driverId, id);
    }

    public List<PaymentIntent> recentOf(UUID driverId) {
        return store.recentOf(driverId);
    }
}
