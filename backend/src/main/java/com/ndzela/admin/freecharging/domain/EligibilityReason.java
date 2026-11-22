package com.ndzela.admin.freecharging.domain;

public enum EligibilityReason {
    ELIGIBLE,
    NO_AGREEMENT,
    AGREEMENT_SUSPENDED,
    AGREEMENT_NOT_STARTED,
    AGREEMENT_ENDED,
    OUTSIDE_WINDOW,
    UNPAID_DAYS,
    DRIVER_SUSPENDED,
    NOT_LINKED
}
