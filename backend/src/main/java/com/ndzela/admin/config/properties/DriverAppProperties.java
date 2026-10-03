package com.ndzela.admin.config.properties;

// Rules of the driver app
public record DriverAppProperties(int lockAfterUnpaidDays) {
    public boolean locks(int unpaidDays) {
        return unpaidDays >= lockAfterUnpaidDays;
    }
}
