package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.payment.domain.PaymentOperator;

public record OperatorResponse(String code, String label) {
    public static OperatorResponse from(PaymentOperator operator) {
        return new OperatorResponse(operator.getCode(), operator.getLabel());
    }
}
