package com.ndzela.admin.payment.application;

import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.infrastructure.pvit.PvitClient;
import com.ndzela.admin.infrastructure.pvit.PvitException;
import com.ndzela.admin.infrastructure.pvit.PvitStatus;
import com.ndzela.admin.infrastructure.pvit.PvitTransaction;
import com.ndzela.admin.payment.domain.IntentRef;
import com.ndzela.admin.payment.domain.PaymentIntent;
import com.ndzela.admin.payment.domain.PaymentIntentRepository;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

// Closes payment attempts.
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSettlementService {
    private static final Duration LATE_CONFIRMATION_WINDOW = Duration.ofHours(6);
    private static final Pattern PROVIDER_REFERENCE = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    private final PaymentIntentStore store;
    private final PaymentIntentRepository intents;
    private final PvitClient pvit;
    private final AppProperties props;

    /** From PVit's callback: the attempt is found by our reference; an unknown one is ignored. */
    public void settleByReference(String reference, String callbackProviderReference) {
        if (reference == null || reference.isBlank()) {
            return;
        }
        intents.findRefByReference(reference).ifPresentOrElse(ref -> settleIn(ref, callbackProviderReference),
                () -> log.warn("PVit callback for an unknown reference {}", reference));
    }

    // The safety net
    public int recheckPending() {
        OffsetDateTime now = OffsetDateTime.now();
        List<IntentRef> open = intents.findOpenRefs(now.minus(props.pvit().recheckAfter()), now.minus(LATE_CONFIRMATION_WINDOW));
        open.forEach(ref -> settleIn(ref, null));
        return open.size();
    }

    private void settleIn(IntentRef ref, String callbackProviderReference) {
        try {
            TenantHolder.runAs(TenantContext.country(ref.getCountryId()), () -> settle(ref.getId(), callbackProviderReference));
        } catch (RuntimeException e) {
            log.error("Could not settle payment attempt {}: {}", ref.getId(), e.getMessage(), e);
        }
    }

    private void settle(UUID id, String callbackProviderReference) {
        PaymentIntent intent = store.find(id).orElse(null);
        if (intent == null || !intent.isOpen()) {
            return;
        }
        PvitTransaction transaction;
        try {
            transaction = pvit.transaction(intent.getOperatorCode(), intent.getReference());
        } catch (PvitException e) {
            log.warn("Could not read payment {} from PVit: {}", intent.getReference(), e.getMessage());
            if (e.isNotFound()) {
                expireIfOld(intent);
            }
            return;
        }
        if (!intent.getReference().equals(transaction.merchantReference())) {
            log.warn("PVit answered for another reference ({}), ignoring payment {}", transaction.merchantReference(), intent.getReference());
            return;
        }
        if (transaction.status() == PvitStatus.SUCCESS && transaction.amount() != null && transaction.amount().signum() > 0) {
            if (transaction.amount().compareTo(intent.getAmount()) < 0) {
                log.warn("PVit confirmed {} for payment {} which asked {}", transaction.amount(), intent.getReference(), intent.getAmount());
            }
            store.complete(id, providerReferenceOf(intent, callbackProviderReference), transaction.amount(), transaction.fees());
        } else if (transaction.status() == PvitStatus.FAILED) {
            store.markFailed(id, "Refused or cancelled on the phone.");
        } else {
            expireIfOld(intent);
        }
    }

    // What the ledger shows as the payment's reference: PVit's transaction id when known, otherwise ours.
    // The callback is not authenticated, so what it carries is used only when it looks like a transaction id.
    private static String providerReferenceOf(PaymentIntent intent, String callbackProviderReference) {
        if (intent.getProviderReference() != null) {
            return intent.getProviderReference();
        }
        boolean usable = callbackProviderReference != null && PROVIDER_REFERENCE.matcher(callbackProviderReference).matches();
        return usable ? callbackProviderReference : intent.getReference();
    }

    private void expireIfOld(PaymentIntent intent) {
        if (intent.isPending() && intent.getCreatedAt().plus(props.pvit().expireAfter()).isBefore(OffsetDateTime.now())) {
            store.expire(intent.getId());
        }
    }
}
