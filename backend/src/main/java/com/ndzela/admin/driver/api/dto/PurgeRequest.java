package com.ndzela.admin.driver.api.dto;

import jakarta.validation.constraints.AssertTrue;

// The caller must have ticked "I am sure": the money is erased with the driver.
public record PurgeRequest(@AssertTrue boolean confirm) {}
