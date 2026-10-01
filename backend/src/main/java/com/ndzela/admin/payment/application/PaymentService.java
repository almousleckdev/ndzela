package com.ndzela.admin.payment.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.payment.api.dto.PaymentResult;
import com.ndzela.admin.payment.api.dto.RecordPaymentRequest;
import com.ndzela.admin.payment.api.dto.WaiverRequest;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.domain.LedgerEntry;
import com.ndzela.admin.payment.domain.LedgerEntryRepository;
import com.ndzela.admin.payment.domain.LedgerEntryType;
import com.ndzela.admin.payment.domain.PaymentMethod;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Records what drivers pay and the corrections (reversals, waivers). */
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final LedgerEntryRepository ledger;
    private final DriverArrearsService driverArrears;
    private final ContractService contractService;
    private final AccountService accounts;
    private final PaymentQueryService queries;
    private final AuditService audit;

    @Transactional
    public PaymentResult record(RecordPaymentRequest request) {
        DriverRef driver = driverArrears.lock(request.driverId());
        var existing = ledger.findByRequestId(request.requestId());
        if (existing.isPresent()) {
            return result(existing.get(), driver, activeContract(driver));
        }
        if (request.method() != PaymentMethod.CASH && (request.reference() == null || request.reference().isBlank())) {
            throw new BusinessRuleViolationException("PAYMENT_REFERENCE_REQUIRED", "A reference is required unless the payment is in cash.");
        }
        ContractResponse contract = activeContract(driver);
        LedgerEntry entry = save(entry(driver, contract, LedgerEntryType.PAYMENT, request.amount())
                .method(request.method()).reference(blankToNull(request.reference())).requestId(request.requestId()).build());
        audit.record("PAYMENT_RECORDED", "DRIVER", driver.id().toString(), driver.fullName(), driver.countryId(),
                request.amount().toPlainString() + " " + request.method() + (entry.getReference() == null ? "" : ", ref " + entry.getReference()));
        return result(entry, driver, contract);
    }

    @Transactional
    public PaymentResult reverse(UUID entryId, String reason) {
        LedgerEntry original = ledger.findById(entryId).orElseThrow(() -> new ResourceNotFoundException("Ledger entry not found."));
        DriverRef driver = driverArrears.lock(original.getDriverId());
        if (original.getType() != LedgerEntryType.PAYMENT && original.getType() != LedgerEntryType.WAIVER) {
            throw new BusinessRuleViolationException("ENTRY_NOT_REVERSIBLE", "Only a payment or a waiver can be reversed.");
        }
        if (ledger.existsByReversesEntryId(entryId)) {
            throw new BusinessRuleViolationException("ENTRY_ALREADY_REVERSED", "This entry has already been reversed.");
        }
        ContractResponse contract = contractService.get(original.getContractId());
        LedgerEntry reversal = save(LedgerEntry.builder().contractId(contract.id()).driverId(driver.id()).type(LedgerEntryType.REVERSAL).amount(original.getAmount())
                .reversesEntryId(entryId).reason(reason.trim()).recordedBy(actor()).createdAt(OffsetDateTime.now()).build());
        audit.record("PAYMENT_REVERSED", "DRIVER", driver.id().toString(), driver.fullName(), driver.countryId(), original.getAmount().toPlainString() + ", reason: " + reason.trim());
        return result(reversal, driver, contract.status().name().equals("ACTIVE") ? contract : null);
    }

    @Transactional
    public PaymentResult waive(WaiverRequest request) {
        DriverRef driver = driverArrears.lock(request.driverId());
        ContractResponse contract = activeContract(driver);
        LedgerEntry entry = save(entry(driver, contract, LedgerEntryType.WAIVER, request.amount()).reason(request.reason().trim()).build());
        audit.record("ARREARS_WAIVED", "DRIVER", driver.id().toString(), driver.fullName(), driver.countryId(), request.amount().toPlainString() + ", reason: " + request.reason().trim());
        return result(entry, driver, contract);
    }

    private LedgerEntry.LedgerEntryBuilder entry(DriverRef driver, ContractResponse contract, LedgerEntryType type, BigDecimal amount) {
        TenantHolder.requireCountry();
        return LedgerEntry.builder().contractId(contract.id()).driverId(driver.id()).type(type).amount(amount).recordedBy(actor()).createdAt(OffsetDateTime.now());
    }

    private LedgerEntry save(LedgerEntry entry) {
        try {
            return ledger.saveAndFlush(entry);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException("ENTRY_ALREADY_REVERSED", "This entry has already been reversed.");
        }
    }

    private ContractResponse activeContract(DriverRef driver) {
        ContractResponse contract = contractService.activeOf(List.of(driver.id())).get(driver.id());
        if (contract == null) {
            throw new BusinessRuleViolationException("NO_ACTIVE_CONTRACT", "This driver has no active contract.");
        }
        return contract;
    }

    private PaymentResult result(LedgerEntry entry, DriverRef driver, ContractResponse contract) {
        if (contract == null) {
            return new PaymentResult(queries.toResponse(entry, driver), driver.level(), driver.overdueAmount(), BigDecimal.ZERO);
        }
        AccountState state = accounts.refresh(driver, contract, Instant.now());
        return new PaymentResult(queries.toResponse(entry, driver), state.level(), state.overdueAmount(), state.credit());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
