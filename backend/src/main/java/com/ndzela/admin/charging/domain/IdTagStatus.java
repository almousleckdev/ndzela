package com.ndzela.admin.charging.domain;

// LINKED: sessions of the idTag are the driver's. SUGGESTED: only one of e-mail and phone matched, staff decide. UNLINKED: staff refused it, never linked again by itself.
public enum IdTagStatus {
    LINKED, SUGGESTED, UNLINKED
}
