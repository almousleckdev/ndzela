package com.ndzela.admin.driver.api.dto;

import java.time.LocalDate;

public record DriverTrainingView(TrainingCenterResponse trainingCenter, LocalDate trainingDate, String certificateNumber) {}
