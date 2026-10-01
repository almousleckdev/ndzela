package com.ndzela.admin.driver.api.dto;

/** The numbers above the drivers table; active excludes the drivers currently on leave. */
public record DriverStats(long total, long active, long onLeave, long suspended, long blocked, long inactive, long assigned, long withoutVehicle, long late) {}
