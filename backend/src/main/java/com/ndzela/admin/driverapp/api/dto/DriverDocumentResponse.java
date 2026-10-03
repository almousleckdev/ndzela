package com.ndzela.admin.driverapp.api.dto;

// One of the driver's documents; `kind` is CONTRACT, LICENCE, ID or ADDRESS_PROOF.
public record DriverDocumentResponse(String kind, boolean available) {}
