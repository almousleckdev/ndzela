package com.ndzela.admin.audit.application;

import com.ndzela.admin.audit.domain.AuditLog;
import com.ndzela.admin.audit.domain.AuditLogRepository;
import com.ndzela.admin.common.web.RequestCorrelationFilter;
import com.ndzela.admin.country.domain.CountryRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/** Writes the append-only audit trail. */
@Service
@RequiredArgsConstructor
public class AuditService {
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILURE = "FAILURE";

    private final AuditLogRepository logs;
    private final CountryRepository countries;

    @Transactional
    public void record(String action, String targetType, String targetId, String targetLabel, UUID countryId, String details) {
        save(currentActor(), action, targetType, targetId, targetLabel, countryId, SUCCESS, null, details);
    }

    // Records a sign-in outcome in its own transaction, so a failed request still leaves its record.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSignIn(String email, boolean success, UUID countryId, String reason) {
        save(email, success ? "LOGIN_SUCCESS" : "LOGIN_FAILED", "USER", null, email, countryId, success ? SUCCESS : FAILURE, reason, null);
    }

    private void save(String actor, String action, String targetType, String targetId, String targetLabel, UUID countryId,
                      String result, String reason, String details) {
        logs.save(AuditLog.builder()
                .actor(actor)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .target(targetLabel)
                .result(result)
                .reason(reason)
                .details(details)
                .requestId(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY))
                .ipAddress(clientAddress())
                .country(countryId == null ? null : countries.getReferenceById(countryId))
                .build());
    }

    private static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() ? authentication.getName() : "system";
    }

    private static String clientAddress() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? ((HttpServletRequest) attributes.getRequest()).getRemoteAddr() : null;
    }
}
