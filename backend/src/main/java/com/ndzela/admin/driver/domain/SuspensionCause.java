package com.ndzela.admin.driver.domain;

// ARREARS suspensions are lifted automatically once everything is paid; MANUAL ones only by a person.
public enum SuspensionCause {
    ARREARS,
    MANUAL
}
