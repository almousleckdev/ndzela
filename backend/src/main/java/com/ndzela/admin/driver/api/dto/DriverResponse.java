package com.ndzela.admin.driver.api.dto;

import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.country.api.dto.CountryResponse;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A driver for the admin: never includes the password or its hash. */
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
        OffsetDateTime createdAt
) {
    public static DriverResponse from(Driver d, VehicleResponse vehicle, boolean onLeave, ContractResponse contract) {
        return new DriverResponse(d.getId(), PersonalInfo.from(d), EmergencyContact.from(d),
                new DriverTrainingView(TrainingCenterResponse.from(d.getTrainingCenter()), d.getTrainingDate(), d.getCertificateNumber()),
                d.getStatus(), onLeave, d.getStatusReason(),
                new DriverFilesView(d.getPhotoFile() != null, d.getLicenceFile() != null, d.getIdFile() != null, d.getAddressProofFile() != null),
                d.isMustChangePassword(), vehicle, contract,
                new DriverArrearsView(d.getArrearsLevel(), d.getOverdueAmount().setScale(0, java.math.RoundingMode.HALF_UP), d.getOverdueSince()),
                d.getCountry() == null ? null : CountryResponse.from(d.getCountry()), d.getCreatedAt());
    }
}
