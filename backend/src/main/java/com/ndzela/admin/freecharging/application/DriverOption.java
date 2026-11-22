package com.ndzela.admin.freecharging.application;

import com.ndzela.admin.driver.domain.DriverStatus;

import java.util.UUID;

// A driver of the fleet as the Gratuité pages list it: the car the driver holds, the linked Zella customer (null when not linked) and the agreement if there is one (id and status, null when none).
public record DriverOption(UUID driverId, String customerId, String fullName, String phone, String email, String fleetNumber, String plate,
                           DriverStatus status, boolean linked, boolean inProgram, UUID agreementId, String agreementStatus) {}
