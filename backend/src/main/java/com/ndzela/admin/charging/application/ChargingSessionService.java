package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.ChargingEntryRepository;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.ocpp.api.SessionRow;
import com.ndzela.admin.ocpp.application.OcppSessionQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The network's sessions as staff see them, each marked with the recharge that recorded its payment, if any
@Service
@RequiredArgsConstructor
public class ChargingSessionService {
    private final OcppSessionQueryService sessions;
    private final ChargingEntryRepository entries;

    @Transactional(readOnly = true)
    public PageResponse<SessionRow> list(String status, String q, LocalDate from, LocalDate to, int page, int size) {
        PageResponse<SessionRow> found = sessions.list(status, q, from, to, page, size);
        Map<UUID, UUID> paid = new HashMap<>();
        if (!found.items().isEmpty()) {
            entries.activeRechargesOfSessions(found.items().stream().map(SessionRow::id).toList()).forEach(row -> paid.put((UUID) row[0], (UUID) row[1]));
        }
        List<SessionRow> marked = found.items().stream().map(row -> paid.containsKey(row.id()) ? row.withRecharge(paid.get(row.id())) : row).toList();
        return new PageResponse<>(marked, found.page(), found.size(), found.total());
    }
}
