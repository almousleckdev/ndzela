package com.ndzela.admin.driver.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrainingCenterRepository extends JpaRepository<TrainingCenter, UUID> {
    boolean existsByCountryIdAndNameIgnoreCaseAndIdNot(UUID countryId, String name, UUID id);
}
