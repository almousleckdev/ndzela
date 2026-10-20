package com.ndzela.admin.driver.api;

import com.ndzela.admin.driver.api.dto.DriverResponse;
import com.ndzela.admin.driver.application.DriverQueryService;
import com.ndzela.admin.driver.application.VehicleRemovalService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleDriverController {
    private final DriverQueryService drivers;
    private final VehicleRemovalService removal;

    @GetMapping("/{vehicleId}/driver")
    @PreAuthorize("hasAuthority('vehicles:read') and hasAuthority('drivers:read')")
    public ResponseEntity<DriverResponse> current(@PathVariable UUID vehicleId) {
        return drivers.driverOfVehicle(vehicleId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/{vehicleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('vehicles:delete')")
    public void delete(@PathVariable UUID vehicleId) {
        removal.delete(vehicleId);
    }
}
