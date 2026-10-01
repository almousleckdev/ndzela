package com.ndzela.admin.driver.application.model;

import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverRef(UUID id, UUID countryId, String firstName, String lastName, String email, String phone, DriverStatus status,
                        ArrearsLevel level, BigDecimal overdueAmount, OffsetDateTime overdueSince) {
    public String fullName() {
        return lastName + " " + firstName;
    }

    public static DriverRef from(Driver d) {
        return new DriverRef(d.getId(), d.getCountryId(), d.getFirstName(), d.getLastName(), d.getEmail(), d.getPhonePrimary(), d.getStatus(),
                d.getArrearsLevel(), d.getOverdueAmount(), d.getOverdueSince());
    }
}
