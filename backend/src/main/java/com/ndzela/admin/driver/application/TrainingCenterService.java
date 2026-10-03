package com.ndzela.admin.driver.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.driver.api.dto.TrainingCenterRequest;
import com.ndzela.admin.driver.api.dto.TrainingCenterResponse;
import com.ndzela.admin.driver.domain.TrainingCenter;
import com.ndzela.admin.driver.domain.TrainingCenterRepository;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// The managed list of training centres, per country.
@Service
@RequiredArgsConstructor
public class TrainingCenterService {
    private final TrainingCenterRepository centers;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<TrainingCenterResponse> list() {
        return centers.findAll(Sort.by("name")).stream().map(TrainingCenterResponse::from).toList();
    }

    @Transactional
    public TrainingCenterResponse create(TrainingCenterRequest request) {
        UUID countryId = TenantHolder.requireCountry();
        String name = request.name().trim();
        assertNameFree(countryId, name, new UUID(0L, 0L));
        TrainingCenter saved = saveChecked(TrainingCenter.builder().name(name).build());
        record("TRAINING_CENTER_CREATED", saved);
        return TrainingCenterResponse.from(saved);
    }

    @Transactional
    public TrainingCenterResponse update(UUID id, TrainingCenterRequest request) {
        TrainingCenter center = load(id);
        String name = request.name().trim();
        assertNameFree(center.getCountryId(), name, id);
        center.setName(name);
        TrainingCenter saved = saveChecked(center);
        record("TRAINING_CENTER_UPDATED", saved);
        return TrainingCenterResponse.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        TrainingCenter center = load(id);
        record("TRAINING_CENTER_DELETED", center);
        try {
            centers.delete(center);
            centers.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException("TRAINING_CENTER_IN_USE", "This training centre is used by drivers.");
        }
    }

    // The unique index is the last line of defence against two requests racing.
    private TrainingCenter saveChecked(TrainingCenter center) {
        try {
            return centers.saveAndFlush(center);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException("TRAINING_CENTER_NAME_TAKEN", "A training centre with this name already exists.");
        }
    }

    private TrainingCenter load(UUID id) {
        return centers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Training centre not found."));
    }

    private void assertNameFree(UUID countryId, String name, UUID selfId) {
        if (centers.existsByCountryIdAndNameIgnoreCaseAndIdNot(countryId, name, selfId)) {
            throw new BusinessRuleViolationException("TRAINING_CENTER_NAME_TAKEN", "A training centre with this name already exists.");
        }
    }

    private void record(String action, TrainingCenter center) {
        audit.record(action, "TRAINING_CENTER", center.getId().toString(), center.getName(), center.getCountryId(), null);
    }
}
