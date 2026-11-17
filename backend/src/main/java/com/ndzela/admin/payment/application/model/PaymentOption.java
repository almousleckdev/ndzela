package com.ndzela.admin.payment.application.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// What paying `days` late rent days costs; `chosen` names them when staff picked which ones (empty: the oldest ones)
public record PaymentOption(int days, BigDecimal amount, @JsonIgnore List<LocalDate> chosen) {
    public PaymentOption(int days, BigDecimal amount) {
        this(days, amount, List.of());
    }
}
