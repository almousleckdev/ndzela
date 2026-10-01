package com.ndzela.admin.driver.api;

import com.ndzela.admin.driver.api.dto.TrainingCenterRequest;
import com.ndzela.admin.driver.api.dto.TrainingCenterResponse;
import com.ndzela.admin.driver.application.TrainingCenterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/training-centers")
@RequiredArgsConstructor
public class TrainingCenterController {
    private final TrainingCenterService centers;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('drivers:read', 'training_centers:manage')")
    public List<TrainingCenterResponse> list() {
        return centers.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('training_centers:manage')")
    public TrainingCenterResponse create(@Valid @RequestBody TrainingCenterRequest request) {
        return centers.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('training_centers:manage')")
    public TrainingCenterResponse update(@PathVariable UUID id, @Valid @RequestBody TrainingCenterRequest request) {
        return centers.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('training_centers:manage')")
    public void delete(@PathVariable UUID id) {
        centers.delete(id);
    }
}
