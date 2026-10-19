package com.ndzela.admin.vehicle.api;

import com.ndzela.admin.vehicle.api.dto.VehicleImageResponse;
import com.ndzela.admin.vehicle.application.VehicleImageService;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

// The photos of a vehicle, uploaded after it exists (vehicle creation itself stays plain JSON).
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/images")
@RequiredArgsConstructor
public class VehicleImageController {
    private final VehicleImageService images;

    @GetMapping
    @PreAuthorize("hasAuthority('vehicles:read')")
    public List<VehicleImageResponse> list(@PathVariable UUID vehicleId) {
        return images.list(vehicleId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('vehicles:update')")
    public List<VehicleImageResponse> upload(@PathVariable UUID vehicleId, @RequestParam("files") List<MultipartFile> files) {
        return images.upload(vehicleId, files);
    }

    @GetMapping("/{imageId}")
    @PreAuthorize("hasAuthority('vehicles:read')")
    public ResponseEntity<Resource> file(@PathVariable UUID vehicleId, @PathVariable UUID imageId) {
        var content = images.read(vehicleId, imageId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType())).cacheControl(CacheControl.noStore()).body(content.resource());
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('vehicles:update')")
    public void delete(@PathVariable UUID vehicleId, @PathVariable UUID imageId) {
        images.delete(vehicleId, imageId);
    }
}
