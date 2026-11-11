package com.ndzela.admin.charging.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// A refused attempt to help a suspended driver. The helper is a driver only when the other account is a linked NDZela driver; otherwise just the reference Zella gave.
// Only a linked helper is scored (we see nothing of the charges of anyone else); the reasons show when the score reaches the flag threshold.
public record SuspensionCase(UUID id, OffsetDateTime occurredAt, String kind, long amountMinor, String currency, CaseDriver suspended, CaseDriver helper, String helperReference,
                             Integer score, List<String> reasons, boolean flagged) {
    public record CaseDriver(UUID id, String name, String vehicle) {}
}
