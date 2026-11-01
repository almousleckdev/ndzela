package com.ndzela.admin.payment.domain;

public enum LedgerEntryType {
    PAYMENT("Paiement"),
    REVERSAL("Annulation"),
    WAIVER("Délai accordé"),
    PENALTY("Pénalité"),
    FEE("Frais opérateur");

    private final String label;

    LedgerEntryType(String label) {
        this.label = label;
    }

    // The name shown in exports
    public String label() {
        return label;
    }
}
