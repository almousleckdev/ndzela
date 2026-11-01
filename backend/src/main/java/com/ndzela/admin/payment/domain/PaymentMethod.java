package com.ndzela.admin.payment.domain;

public enum PaymentMethod {
    CASH("Espèces"),
    AIRTEL_MONEY("Airtel Money"),
    MOOV_MONEY("Moov Money"),
    ORANGE_MONEY("Orange Money"),
    OTHER("Autre");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    // The name shown in exports
    public String label() {
        return label;
    }
}
