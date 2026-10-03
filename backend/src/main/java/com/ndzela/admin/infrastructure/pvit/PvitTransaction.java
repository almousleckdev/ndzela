package com.ndzela.admin.infrastructure.pvit;

import java.math.BigDecimal;

// What PVit says about a transaction
public record PvitTransaction(PvitStatus status, String merchantReference, BigDecimal amount, BigDecimal fees, String operatorCode) {}
