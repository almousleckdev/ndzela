package com.ndzela.admin.vehicle.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.infrastructure.storage.DocumentStorage;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.vehicle.api.dto.VehicleImageResponse;
import com.ndzela.admin.vehicle.domain.Vehicle;
import com.ndzela.admin.vehicle.domain.VehicleImage;
import com.ndzela.admin.vehicle.domain.VehicleImageRepository;
import com.ndzela.admin.vehicle.domain.VehicleRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

// One or more photos per vehicle: private files, served through the backend (ADR-0016), like driver documents.
@Service
@RequiredArgsConstructor
public class VehicleImageService {
    private static final int MAX_IMAGES_PER_VEHICLE = 8;

    public record FileContent(Resource resource, String contentType) {}

    private final VehicleRepository vehicles;
    private final VehicleImageRepository images;
    private final DocumentStorage storage;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<VehicleImageResponse> list(UUID vehicleId) {
        vehicle(vehicleId);
        return images.findByVehicleIdOrderByCreatedAt(vehicleId).stream().map(VehicleImageResponse::from).toList();
    }

    @Transactional
    public List<VehicleImageResponse> upload(UUID vehicleId, List<MultipartFile> files) {
        Vehicle v = vehicle(vehicleId);
        long existing = images.countByVehicleId(vehicleId);
        if (existing + files.size() > MAX_IMAGES_PER_VEHICLE) {
            throw new BusinessRuleViolationException("TOO_MANY_IMAGES", "A vehicle may have at most " + MAX_IMAGES_PER_VEHICLE + " photos.");
        }
        String countryCode = v.getCountry().getCode();
        List<VehicleImage> saved = files.stream().map(file -> images.save(VehicleImage.builder().vehicleId(vehicleId).fileName(storage.save(file, true, countryCode)).build())).toList();
        audit.record("VEHICLE_IMAGE_ADDED", "VEHICLE", vehicleId.toString(), v.getFleetNumber(), TenantHolder.current().countryId(), saved.size() + " photo(s)");
        return images.findByVehicleIdOrderByCreatedAt(vehicleId).stream().map(VehicleImageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FileContent read(UUID vehicleId, UUID imageId) {
        VehicleImage image = imageOf(vehicleId, imageId);
        Resource resource = storage.load(image.getFileName());
        if (resource == null) {
            throw new ResourceNotFoundException("Image not found.");
        }
        return new FileContent(resource, DocumentStorage.contentType(image.getFileName()));
    }

    @Transactional
    public void delete(UUID vehicleId, UUID imageId) {
        Vehicle v = vehicle(vehicleId);
        VehicleImage image = images.findByIdAndVehicleId(imageId, vehicleId).orElseThrow(() -> new ResourceNotFoundException("Image not found."));
        storage.delete(image.getFileName());
        images.delete(image);
        audit.record("VEHICLE_IMAGE_DELETED", "VEHICLE", vehicleId.toString(), v.getFleetNumber(), TenantHolder.current().countryId(), null);
    }

    private Vehicle vehicle(UUID vehicleId) {
        return vehicles.findById(vehicleId).orElseThrow(() -> new ResourceNotFoundException("Vehicle not found."));
    }

    // Tenant-scoped through the vehicle lookup, and the image must actually belong to that vehicle (no IDOR by guessing an image id).
    private VehicleImage imageOf(UUID vehicleId, UUID imageId) {
        vehicle(vehicleId);
        return images.findByIdAndVehicleId(imageId, vehicleId).orElseThrow(() -> new ResourceNotFoundException("Image not found."));
    }
}
