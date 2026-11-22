package com.ndzela.admin.freecharging.application;

import com.ndzela.admin.freecharging.domain.EligibilityReason;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// The agreement, the Sunday and the end of the window are only present when the driver is eligible. The window itself is present whenever the driver is known, so clients can say when the free charge is available.
public record Eligibility(boolean eligible, EligibilityReason reason, UUID agreementId, LocalDate sundayDate, Instant windowEndsAt, WindowView window) {
    public static Eligibility refused(EligibilityReason reason) {
        return new Eligibility(false, reason, null, null, null, null);
    }

    public static Eligibility refused(EligibilityReason reason, WindowView window) {
        return new Eligibility(false, reason, null, null, null, window);
    }

    public static Eligibility granted(UUID agreementId, LocalDate sundayDate, Instant windowEndsAt, WindowView window) {
        return new Eligibility(true, EligibilityReason.ELIGIBLE, agreementId, sundayDate, windowEndsAt, window);
    }
}
