package com.ndzela.admin.ocpp.application;

import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.country.application.DayRanges;
import com.ndzela.admin.ocpp.api.SessionKind;
import com.ndzela.admin.ocpp.api.SessionRow;
import com.ndzela.admin.ocpp.domain.ChargingSession;
import com.ndzela.admin.ocpp.domain.ChargingSessionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OcppSessionQueryService {
    private final ChargingSessionRepository sessions;
    private final DayRanges ranges;

    @Transactional(readOnly = true)
    public PageResponse<SessionRow> list(String status, String q, LocalDate from, LocalDate to, int page, int size) {
        return list(status, q, from, to, SessionKind.ALL, Set.of(), page, size);
    }

    /** `driverIdTags` are the idTags linked to a NDZela driver; they tell the driver sessions from the public ones. */
    @Transactional(readOnly = true)
    public PageResponse<SessionRow> list(String status, String q, LocalDate from, LocalDate to, SessionKind kind, Collection<String> driverIdTags, int page, int size) {
        Specification<ChargingSession> spec = ranges.<ChargingSession>within("startedAt", from, to).and(status(status)).and(search(q)).and(kind(kind, driverIdTags));
        return PageResponse.from(sessions.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Order.desc("startedAt").nullsLast(), Sort.Order.desc("createdAt")))), SessionRow::from);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<SessionRow> find(java.util.UUID id) {
        return sessions.findById(id).map(SessionRow::from);
    }

    private static Specification<ChargingSession> kind(SessionKind kind, Collection<String> driverIdTags) {
        return (root, query, cb) -> switch (kind) {
            case ALL -> cb.conjunction();
            case DRIVER -> driverIdTags.isEmpty() ? cb.disjunction() : root.get("idTag").in(driverIdTags);
            case PUBLIC -> driverIdTags.isEmpty() ? cb.conjunction() : cb.or(cb.isNull(root.get("idTag")), cb.not(root.get("idTag").in(driverIdTags)));
        };
    }

    private static Specification<ChargingSession> status(String status) {
        return (root, query, cb) -> status == null || status.isBlank() ? cb.conjunction() : cb.equal(root.get("status"), status.trim());
    }

    private static Specification<ChargingSession> search(String q) {
        if (q == null || q.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + q.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
        return (root, query, cb) -> cb.or(cb.like(cb.lower(root.get("transactionId")), pattern), cb.like(cb.lower(cb.coalesce(root.<String>get("chargePointId"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.<String>get("idTag"), "")), pattern));
    }
}
