package com.ndzela.admin.infrastructure.zella;

import java.util.List;

public record ZellaEvidencePage(List<ZellaEvidenceItem> items, String nextCursor, boolean hasMore) {}
