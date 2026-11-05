package com.ndzela.admin.infrastructure.zella;

import java.util.List;

// A page of the payment feed: keep nextCursor exactly as received
public record ZellaFeedPage(List<ZellaFeedItem> items, String nextCursor, boolean hasMore) {}
