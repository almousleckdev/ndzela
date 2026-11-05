package com.ndzela.admin.driver.application;

import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.application.model.HeldVehicle;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.driver.domain.VehicleAssignmentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

// What other features may ask about drivers: who still has to be matched with a charging account, and which car a driver held when.
@Service
@RequiredArgsConstructor
public class DriverDirectory {
    private final DriverRepository drivers;
    private final VehicleAssignmentRepository assignments;

    /** Drivers still working (not archived) who are not in `excluded`, oldest first. */
    @Transactional(readOnly = true)
    public List<DriverRef> candidates(Collection<UUID> excluded, int limit) {
        Specification<Driver> spec = (root, query, cb) -> cb.notEqual(root.get("status"), DriverStatus.INACTIVE);
        if (!excluded.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.not(root.get("id").in(excluded)));
        }
        return drivers.findAll(spec, PageRequest.of(0, limit, org.springframework.data.domain.Sort.by("createdAt", "id"))).stream().map(DriverRef::from).toList();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<DriverRef> find(UUID id) {
        return drivers.findById(id).map(DriverRef::from);
    }

    @Transactional(readOnly = true)
    public List<HeldVehicle> held(Collection<UUID> driverIds) {
        if (driverIds.isEmpty()) {
            return List.of();
        }
        return assignments.findAllByDriverIdIn(driverIds).stream().map(a -> new HeldVehicle(a.getDriverId(), a.getVehicleId(), a.getAssignedAt(), a.getEndedAt())).toList();
    }
}
