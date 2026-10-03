package com.ndzela.admin.infrastructure.pvit;

public record PvitPaymentStarted(String providerReference, PvitStatus status, String message) {}
