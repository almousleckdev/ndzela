package com.ndzela.admin.driver.api.dto;

import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.country.api.dto.CountryResponse;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

// A driver for the admin
public record DriverResponse(
        UUID id,
        PersonalInfo personal,
        EmergencyContact emergency,
        DriverTrainingView training,
        DriverStatus status,
        boolean onLeave,
        String statusReason,
        DriverFilesView files,
        boolean mustChangePassword,
        VehicleResponse vehicle,
        ContractResponse contract,
        DriverArrearsView arrears,
        CountryResponse country,
        // Locked out of logging in by repeated failed attempts (drivers:unlock clears it); separate from `status`.
        boolean loginLocked,
        OffsetDateTime createdAt
) {
    public static DriverResponse from(Driver d, VehicleResponse vehicle, boolean onLeave, ContractResponse contract) {
        return new DriverResponse(d.getId(), PersonalInfo.from(d), EmergencyContact.from(d),
                new DriverTrainingView(d.getTrainingCenter() == null ? null : TrainingCenterResponse.from(d.getTrainingCenter()), d.getTrainingDate(), d.getCertificateNumber()),
                d.getStatus(), onLeave, d.getStatusReason(),
                new DriverFilesView(d.getPhotoFile() != null, d.getLicenceFile() != null, d.getIdFile() != null, d.getAddressProofFile() != null, d.getResidenceCertificateFile() != null),
                d.isMustChangePassword(), vehicle, contract,
                new DriverArrearsView(d.getArrearsLevel(), d.getOverdueAmount().setScale(0, java.math.RoundingMode.HALF_UP), d.getOverdueSince(), d.getUnpaidDays(), d.getPaidDays(), daysLeft(d, contract)),
                d.getCountry() == null ? null : CountryResponse.from(d.getCountry()),
                d.getLoginLockedUntil() != null && d.getLoginLockedUntil().isAfter(OffsetDateTime.now()), d.getCreatedAt());
    }

    // The rent days left until the contract ends (Sundays are rest days), counted in the country's own time.
    private static int daysLeft(Driver d, ContractResponse contract) {
        if (contract == null) {
            return 0;
        }
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of(d.getCountry().getTimezone()));
        java.time.LocalDate until = today.isBefore(contract.endDate()) ? today : contract.endDate();
        return Math.max(0, contract.billableDays() - com.ndzela.admin.contract.domain.BillingCalendar.billableDaysBetween(contract.startDate(), until));
    }
}
