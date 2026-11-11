package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.SuspensionEvidence;
import com.ndzela.admin.charging.domain.SuspensionEvidenceRepository;
import com.ndzela.admin.infrastructure.zella.ZellaClient;
import com.ndzela.admin.infrastructure.zella.ZellaCursorInvalidException;
import com.ndzela.admin.infrastructure.zella.ZellaEvidenceItem;
import com.ndzela.admin.infrastructure.zella.ZellaEvidencePage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Reads what the Zella app refused for our suspended drivers, from a saved cursor; replaying a page stores nothing twice.
@Slf4j
@Component
@RequiredArgsConstructor
public class ZellaEvidenceSync {
    private static final int PAGE = 200;
    private static final int MAX_PAGES = 100;

    private final ZellaClient zella;
    private final ZellaFeedState state;
    private final IdTagRepository tags;
    private final SuspensionEvidenceRepository evidence;
    private final TransactionTemplate transaction;

    /** Returns how many cases were stored. */
    public int sync(UUID countryId) {
        String cursor = state.evidenceCursor(countryId);
        int stored = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            ZellaEvidencePage result;
            try {
                result = zella.evidence(cursor, PAGE);
            } catch (ZellaCursorInvalidException e) {
                if (cursor == null) {
                    throw e;
                }
                log.warn("Zella refused the saved evidence cursor, reading the evidence again from the start");
                state.saveEvidenceCursor(countryId, null);
                cursor = null;
                continue;
            }
            String next = result.nextCursor();
            stored += transaction.execute(status -> {
                int n = store(result.items());
                if (next != null) {
                    state.saveEvidenceCursor(countryId, next);
                }
                return n;
            });
            cursor = next != null ? next : cursor;
            if (!result.hasMore()) {
                break;
            }
        }
        return stored;
    }

    private int store(List<ZellaEvidenceItem> items) {
        int stored = 0;
        for (ZellaEvidenceItem item : items) {
            UUID suspended = tags.findByCustomerId(item.suspendedCustomerId()).map(IdTag::getDriverId).orElse(null);
            if (suspended == null || evidence.existsByEventId(item.eventId())) {
                continue;
            }
            UUID helper = item.receivingCustomerId() == null ? null : tags.findByCustomerId(item.receivingCustomerId()).map(IdTag::getDriverId).orElse(null);
            evidence.save(SuspensionEvidence.builder().eventId(item.eventId()).suspendedDriverId(suspended).receivingDriverId(helper).receivingRef(item.receivingAccountRef() == null ? "" : item.receivingAccountRef())
                    .kind(item.kind() == null ? "UNKNOWN" : item.kind()).amountMinor(item.amountMinor()).currency(item.currency()).occurredAt(item.occurredAt() == null ? OffsetDateTime.now() : item.occurredAt()).build());
            stored++;
        }
        return stored;
    }
}
