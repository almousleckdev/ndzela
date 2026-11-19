package com.ndzela.admin.driver.domain;

public enum PauseCause {
    // Set inactive by staff (accident, holiday)
    INACTIVE,
    // The car was taken; the contract waits for the next one
    NO_CAR
}
