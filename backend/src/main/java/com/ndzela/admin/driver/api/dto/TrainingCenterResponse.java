package com.ndzela.admin.driver.api.dto;

import com.ndzela.admin.driver.domain.TrainingCenter;

import java.util.UUID;

public record TrainingCenterResponse(UUID id, String name) {
    public static TrainingCenterResponse from(TrainingCenter center) {
        return new TrainingCenterResponse(center.getId(), center.getName());
    }
}
