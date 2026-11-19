package com.ndzela.admin.driver.domain;

public enum DriverStatus {
    ACTIVE,
    SUSPENDED,
    BLOCKED,
    INACTIVE,
    // Resigned: no car, no login, nothing owed, the whole history kept
    ARCHIVED;

    // No longer working for NDZela: the ones who resigned. An inactive driver is only paused (ADR-0046).
    public boolean isFormer() {
        return this == ARCHIVED;
    }
}
