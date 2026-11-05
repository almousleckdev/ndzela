package com.ndzela.admin.driver.application;

import com.ndzela.admin.contract.domain.snapshot.SnapshotLocataire;
import com.ndzela.admin.contract.domain.snapshot.SnapshotLicence;
import com.ndzela.admin.contract.domain.snapshot.SnapshotEmergency;
import com.ndzela.admin.contract.domain.snapshot.SnapshotVehicle;
import com.ndzela.admin.contract.domain.snapshot.SnapshotTraining;
import com.ndzela.admin.contract.domain.snapshot.SnapshotDocuments;
import com.ndzela.admin.contract.domain.snapshot.ContractSnapshot;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;

// Freezes the driver, the car and the training as a contract prints them.
final class DriverContractSnapshots {
    private DriverContractSnapshots() {}

    static ContractSnapshot of(Driver d, VehicleResponse v) {
        return new ContractSnapshot(null,
                new SnapshotLocataire(d.getCivility().name(), d.getLastName(), d.getFirstName(), d.getBirthDate(), d.getBirthPlace(), d.getNationality(), d.getIdType().name(),
                        d.getIdNumber(), d.getIdIssueDate(), d.getIdIssuePlace(), d.getAddress(), d.getDistrict(), d.getCity(), d.getPhonePrimary(), d.getPhoneSecondary(), d.getEmail()),
                new SnapshotLicence(d.getLicenceNumber(), d.getLicenceCategory().name(), d.getLicenceIssueDate()),
                new SnapshotEmergency(d.getEmergencyLastName(), d.getEmergencyFirstName(), d.getEmergencyPhone(), d.getEmergencyRelationship().name(), d.getEmergencyAddress()),
                new SnapshotVehicle(v.brand(), v.model(), v.fleetNumber(), v.plate(), v.color(), v.vin(), v.year()),
                new SnapshotTraining(d.getTrainingCenter() == null ? null : d.getTrainingCenter().getName(), d.getTrainingDate(), d.getCertificateNumber()),
                new SnapshotDocuments(d.getLicenceFile() != null, d.getIdFile() != null, d.getAddressProofFile() != null, d.getResidenceCertificateFile() != null));
    }
}
