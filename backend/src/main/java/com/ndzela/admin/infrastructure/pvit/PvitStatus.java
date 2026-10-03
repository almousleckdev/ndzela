package com.ndzela.admin.infrastructure.pvit;

public enum PvitStatus {
    SUCCESS,
    FAILED,
    PENDING;

    // Anything PVit says that is not clearly a final answer keeps the payment waiting.
    public static PvitStatus from(String value) {
        if (value == null) {
            return PENDING;
        }
        return switch (value.trim().toUpperCase()) {
            case "SUCCESS", "SUCCESSFUL", "COMPLETED" -> SUCCESS;
            case "FAILED", "FAILURE", "CANCELLED", "REJECTED", "EXPIRED" -> FAILED;
            default -> PENDING;
        };
    }
}
