package com.ndzela.admin.contract.application.model;

import com.ndzela.admin.contract.domain.snapshot.ContractSnapshot;

import java.math.BigDecimal;
import java.util.UUID;

// What the driver feature hands over to open a contract
public record ContractSeed(UUID countryId, UUID driverId, UUID vehicleId, BigDecimal dailyRent, BigDecimal deposit, ContractSnapshot snapshot) {}
