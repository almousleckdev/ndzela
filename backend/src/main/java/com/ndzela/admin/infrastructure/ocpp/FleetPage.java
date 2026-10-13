package com.ndzela.admin.infrastructure.ocpp;

import java.util.List;

// A page of the sync: keep nextCursor exactly as received, even when there are no items
public record FleetPage(List<FleetSession> items, String nextCursor, boolean hasMore) {}
