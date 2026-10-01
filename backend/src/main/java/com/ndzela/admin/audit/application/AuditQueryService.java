package com.ndzela.admin.audit.application;

import com.ndzela.admin.audit.api.dto.AuditLogResponse;
import com.ndzela.admin.audit.domain.AuditLog;
import com.ndzela.admin.audit.domain.AuditLogRepository;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.common.web.SortParam;
import com.ndzela.admin.tenant.model.TenantContext;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditQueryService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORTS = Map.of("createdAt", "createdAt", "actor", "actor", "action", "action", "result", "result", "target", "target");

    private final AuditLogRepository logs;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String action, String actor, String targetType, LocalDate from, LocalDate to, String sort, int page, int size) {
        Specification<AuditLog> spec = scope(TenantHolder.current());
        if (hasText(action)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action.trim()));
        }
        if (hasText(targetType)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetType"), targetType.trim()));
        }
        if (hasText(actor)) {
            String pattern = "%" + actor.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("actor")), pattern, '\\'));
        }
        if (from != null) {
            var start = from.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), start));
        }
        if (to != null) {
            var end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), end));
        }
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), SortParam.parse(sort, SORTS, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return PageResponse.from(logs.findAll(spec, pageable), AuditLogResponse::from);
    }

    private static Specification<AuditLog> scope(TenantContext context) {
        return switch (context.mode()) {
            case ALL -> (root, query, cb) -> cb.conjunction();
            case COUNTRY -> (root, query, cb) -> cb.equal(root.get("country").get("id"), context.countryId());
            case NONE -> (root, query, cb) -> cb.disjunction();
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
