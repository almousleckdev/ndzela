package com.ndzela.admin.driver.domain;

public enum DriverStatus {
    ACTIVE,
    SUSPENDED,
    BLOCKED,
    INACTIVE,
    // Resigned: no car, no login, nothing owed, the whole history kept
    ARCHIVED;

    // No longer working for NDZela: the old riders and the ones who resigned
    public boolean isFormer() {
        return this == INACTIVE || this == ARCHIVED;
    }
}
