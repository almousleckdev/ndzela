package com.ndzela.admin.charging.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StationRepository extends JpaRepository<Station, UUID> {
    boolean existsByCountryIdAndNameIgnoreCaseAndIdNot(UUID countryId, String name, UUID id);
}
