package com.ndzela.admin.user.api.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

// A fleet car or a plate (not both), and whether the staff member charges for free.
public record StaffAccessRequest(UUID vehicleId, @Size(max = 20) String plate, boolean freeCharging) {}
