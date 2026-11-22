package com.ndzela.admin.freecharging.application;

import com.ndzela.admin.driver.domain.DriverStatus;

import java.util.UUID;

// A driver offered for the free program: the car the driver holds, the linked Zella customer (null when not linked) and whether an agreement already exists.
public record DriverOption(UUID driverId, String customerId, String fullName, String phone, String email, String fleetNumber, String plate,
                           DriverStatus status, boolean linked, boolean inProgram) {}
