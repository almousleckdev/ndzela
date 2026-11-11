package com.ndzela.admin.infrastructure.zella;

import java.time.OffsetDateTime;

// One refused attempt to help a suspended driver. The other account is an irreversible reference, plus its customer id only when it is also a linked NDZela driver.
public record ZellaEvidenceItem(String eventId, String suspendedCustomerId, String kind, String receivingAccountRef, boolean receivingIsLinkedDriver,
                                String receivingCustomerId, long amountMinor, String currency, OffsetDateTime occurredAt) {}
