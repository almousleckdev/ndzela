package com.ndzela.admin.contract.domain.snapshot;

import com.ndzela.admin.contract.domain.snapshot.SnapshotBailleur;
import com.ndzela.admin.contract.domain.snapshot.SnapshotDocuments;
import com.ndzela.admin.contract.domain.snapshot.SnapshotEmergency;
import com.ndzela.admin.contract.domain.snapshot.SnapshotLicence;
import com.ndzela.admin.contract.domain.snapshot.SnapshotLocataire;
import com.ndzela.admin.contract.domain.snapshot.SnapshotTraining;
import com.ndzela.admin.contract.domain.snapshot.SnapshotVehicle;

public record ContractSnapshot(SnapshotBailleur bailleur, SnapshotLocataire locataire, SnapshotLicence licence, SnapshotEmergency emergency, SnapshotVehicle vehicle, SnapshotTraining training, SnapshotDocuments documents) {
    public ContractSnapshot withBailleur(SnapshotBailleur value) {
        return new ContractSnapshot(value, locataire, licence, emergency, vehicle, training, documents);
    }
}
