package com.ndzela.admin.payment.domain;

import java.util.UUID;

// A payment attempt found across countries (by PVit's callback or the recheck job)
public interface IntentRef {
    UUID getId();

    UUID getCountryId();
}
