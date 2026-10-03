package com.ndzela.admin.charging.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.charging.api.dto.StationRequest;
import com.ndzela.admin.charging.api.dto.StationResponse;
import com.ndzela.admin.charging.domain.ChargingEntryRepository;
import com.ndzela.admin.charging.domain.Station;
import com.ndzela.admin.charging.domain.StationRepository;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// The managed list of charging stations, per country.
@Service
@RequiredArgsConstructor
public class StationService {
    private final StationRepository stations;
    private final ChargingEntryRepository entries;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<StationResponse> list() {
        return stations.findAll(Sort.by("name")).stream().map(StationService::response).toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> namesByIds(Collection<UUID> ids) {
        return stations.findAllById(ids).stream().collect(Collectors.toMap(Station::getId, Station::getName));
    }

    @Transactional(readOnly = true)
    public Station get(UUID id) {
        return stations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Station not found."));
    }

    @Transactional
    public StationResponse create(StationRequest request) {
        UUID countryId = TenantHolder.requireCountry();
        String name = request.name().trim();
        assertNameFree(countryId, name, new UUID(0L, 0L));
        Station saved = saveChecked(Station.builder().name(name).build());
        record("STATION_CREATED", saved);
        return response(saved);
    }

    @Transactional
    public StationResponse update(UUID id, StationRequest request) {
        Station station = get(id);
        String name = request.name().trim();
        assertNameFree(station.getCountryId(), name, id);
        station.setName(name);
        Station saved = saveChecked(station);
        record("STATION_UPDATED", saved);
        return response(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Station station = get(id);
        if (entries.existsByStationId(id)) {
            throw new BusinessRuleViolationException("STATION_IN_USE", "This station has charging records and cannot be deleted.");
        }
        record("STATION_DELETED", station);
        stations.delete(station);
    }

    private Station saveChecked(Station station) {
        try {
            return stations.saveAndFlush(station);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException("STATION_NAME_TAKEN", "A station with this name already exists.");
        }
    }

    private void assertNameFree(UUID countryId, String name, UUID selfId) {
        if (stations.existsByCountryIdAndNameIgnoreCaseAndIdNot(countryId, name, selfId)) {
            throw new BusinessRuleViolationException("STATION_NAME_TAKEN", "A station with this name already exists.");
        }
    }

    private void record(String action, Station station) {
        audit.record(action, "STATION", station.getId().toString(), station.getName(), station.getCountryId(), null);
    }

    private static StationResponse response(Station station) {
        return new StationResponse(station.getId(), station.getName());
    }
}
