package com.ndzela.admin.payment.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AccountCharge(LocalDate day, Instant due, BigDecimal amount, boolean penalty) {}
