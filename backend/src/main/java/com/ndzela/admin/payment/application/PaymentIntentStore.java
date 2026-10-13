package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.payment.api.dto.RecordPaymentRequest;
import com.ndzela.admin.payment.domain.LedgerEntryRepository;
import com.ndzela.admin.payment.domain.PaymentIntent;
import com.ndzela.admin.payment.domain.PaymentIntentRepository;
import com.ndzela.admin.payment.domain.PaymentIntentStatus;
import com.ndzela.admin.payment.domain.PaymentMethod;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// The database side of payment attempts; each method is one short transaction
@Service
@RequiredArgsConstructor
public class PaymentIntentStore {
    private static final String REFERENCE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REFERENCE_LENGTH = 12;
    // Shown as "Saisi par" for payments recorded from PVit's confirmation rather than typed by staff.
    static final String AUTOMATIC = "PVit (automatique)";

    private final PaymentIntentRepository intents;
    private final PaymentService payments;
    private final LedgerEntryRepository ledger;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public PaymentIntent createPending(UUID driverId, UUID contractId, String operatorCode, String phone, BigDecimal amount, String currency) {
        intents.findFirstByDriverIdAndStatus(driverId, PaymentIntentStatus.PENDING).ifPresent(existing -> {
            if (existing.getCreatedAt().plus(props.pvit().expireAfter()).isBefore(OffsetDateTime.now())) {
                close(existing, PaymentIntentStatus.EXPIRED, "No answer from the operator.");
            } else {
                throw inProgress();
            }
        });
        try {
            return intents.saveAndFlush(PaymentIntent.builder().driverId(driverId).contractId(contractId).reference(newReference()).operatorCode(operatorCode)
                    .customerPhone(phone).amount(amount).currencyCode(currency).status(PaymentIntentStatus.PENDING).build());
        } catch (DataIntegrityViolationException e) {
            throw inProgress();
        }
    }

    @Transactional
    public void markStarted(UUID id, String providerReference) {
        intents.findById(id).filter(PaymentIntent::isPending).ifPresent(intent -> intent.setProviderReference(providerReference));
    }

    @Transactional
    public void markFailed(UUID id, String reason) {
        intents.findForUpdate(id).filter(PaymentIntent::isPending).ifPresent(intent -> close(intent, PaymentIntentStatus.FAILED, reason));
    }

    @Transactional
    public void expire(UUID id) {
        intents.findForUpdate(id).filter(PaymentIntent::isPending).ifPresent(intent -> close(intent, PaymentIntentStatus.EXPIRED, "No answer from the operator."));
    }

    // Records what PVit confirmed as a payment (and its fee as a line of its own), once
    @Transactional
    public boolean complete(UUID id, String providerReference, BigDecimal confirmedAmount, BigDecimal fee) {
        PaymentIntent intent = intents.findForUpdate(id).orElseThrow();
        if (!intent.isOpen()) {
            return false;
        }
        BigDecimal paid = confirmedAmount.setScale(0, RoundingMode.HALF_UP);
        PaymentMethod method = methodOf(intent.getOperatorCode());
        // A reference already taken must not stop money PVit holds from being recorded: ours is unique.
        String reference = ledger.existsActivePayment(method, providerReference) ? intent.getReference() : providerReference;
        payments.record(new RecordPaymentRequest(intent.getDriverId(), paid, method, reference, intent.getId()), AUTOMATIC);
        if (fee != null && fee.signum() > 0) {
            payments.recordProviderFee(intent.getDriverId(), intent.getContractId(), fee, method, reference,
                    UUID.nameUUIDFromBytes(("fee:" + intent.getId()).getBytes(StandardCharsets.UTF_8)));
        }
        intent.setProviderReference(reference);
        intent.setFeeAmount(fee);
        intent.setStatus(PaymentIntentStatus.SUCCESS);
        intent.setFailureReason(null);
        intent.setCompletedAt(OffsetDateTime.now());
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<PaymentIntent> find(UUID id) {
        return intents.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentIntent> findOf(UUID driverId, UUID id) {
        return intents.findByIdAndDriverId(id, driverId);
    }

    @Transactional(readOnly = true)
    public List<PaymentIntent> recentOf(UUID driverId) {
        return intents.findTop50ByDriverIdOrderByCreatedAtDesc(driverId);
    }

    private static void close(PaymentIntent intent, PaymentIntentStatus status, String reason) {
        intent.setStatus(status);
        intent.setFailureReason(reason);
        intent.setCompletedAt(OffsetDateTime.now());
    }

    private static BusinessRuleViolationException inProgress() {
        return new BusinessRuleViolationException("PAYMENT_IN_PROGRESS", "A payment is already waiting for confirmation.");
    }

    private static PaymentMethod methodOf(String operatorCode) {
        try {
            return PaymentMethod.valueOf(operatorCode);
        } catch (IllegalArgumentException e) {
            return PaymentMethod.OTHER;
        }
    }

    private String newReference() {
        StringBuilder reference = new StringBuilder("NDZ");
        for (int i = 0; i < REFERENCE_LENGTH; i++) {
            reference.append(REFERENCE_ALPHABET.charAt(random.nextInt(REFERENCE_ALPHABET.length())));
        }
        return reference.toString();
    }
}
