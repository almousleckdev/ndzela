package com.ndzela.admin.driver.application.model;

// A driver found by a search, with the car the driver holds now (null when none).
public record FoundDriver(DriverRef driver, String fleetNumber, String plate) {}
