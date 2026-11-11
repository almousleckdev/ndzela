package com.ndzela.admin.infrastructure.zella;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

// Reads the evidence feed of the Zella app; an item without its id or its suspended customer is skipped.
final class ZellaEvidenceParser {
    private ZellaEvidenceParser() {}

    static ZellaEvidencePage parse(JsonNode json) {
        List<ZellaEvidenceItem> items = new ArrayList<>();
        for (JsonNode i : json.path("items")) {
            String eventId = ZellaClient.text(i, "eventId");
            String suspended = ZellaClient.text(i, "suspendedCustomerId");
            if (eventId == null || suspended == null) {
                continue;
            }
            Long amount = ZellaClient.number(i, "amountMinor");
            items.add(new ZellaEvidenceItem(eventId, suspended, ZellaClient.text(i, "kind"), ZellaClient.text(i, "receivingAccountRef"),
                    i.path("receivingIsLinkedDriver").asBoolean(false), ZellaClient.text(i, "receivingCustomerId"), amount == null ? 0 : amount,
                    ZellaClient.text(i, "currency"), ZellaClient.time(i, "occurredAt")));
        }
        return new ZellaEvidencePage(items, ZellaClient.text(json, "nextCursor"), json.path("hasMore").asBoolean(false));
    }
}
