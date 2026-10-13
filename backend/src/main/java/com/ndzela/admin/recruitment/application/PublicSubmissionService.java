package com.ndzela.admin.recruitment.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.ratelimit.RateLimiter;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.infrastructure.storage.DocumentStorage;
import com.ndzela.admin.recruitment.api.dto.ApplicationRequest;
import com.ndzela.admin.recruitment.api.dto.ContactRequest;
import com.ndzela.admin.recruitment.application.model.ApplicationUploads;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// What visitors of the public website send.
@Service
@RequiredArgsConstructor
public class PublicSubmissionService {
    private static final Duration SUBMISSION_WINDOW = Duration.ofHours(1);

    private final AppProperties props;
    private final CountryRepository countries;
    private final RateLimiter limiter;
    private final InquiryStore store;
    private final DocumentStorage storage;

    public void submitMessage(ContactRequest request, String visitor) {
        if (isRobot(request.website())) {
            return;
        }
        limiter.check("public-contact", visitor, props.publicSite().maxSubmissionsPerHour(), SUBMISSION_WINDOW);
        TenantHolder.callAs(country(), () -> {
            store.saveMessage(request);
            return null;
        });
    }

    public void submitApplication(ApplicationRequest request, ApplicationUploads uploads, String visitor) {
        if (isRobot(request.website())) {
            return;
        }
        if (missing(uploads.photo()) || missing(uploads.licence()) || missing(uploads.id())) {
            throw new BusinessRuleViolationException("DOCUMENTS_REQUIRED", "The photo, the driving licence and the identity document are required.");
        }
        limiter.check("public-application", visitor, props.publicSite().maxSubmissionsPerHour(), SUBMISSION_WINDOW);
        List<String> stored = new ArrayList<>();
        String countryCode = props.publicSite().countryCode();
        try {
            String photo = keep(stored, storage.save(uploads.photo(), true, countryCode));
            String licence = keep(stored, storage.save(uploads.licence(), false, countryCode));
            String id = keep(stored, storage.save(uploads.id(), false, countryCode));
            String proof = missing(uploads.addressProof()) ? null : keep(stored, storage.save(uploads.addressProof(), false, countryCode));
            TenantHolder.callAs(country(), () -> {
                store.saveApplication(request, photo, licence, id, proof);
                return null;
            });
        } catch (RuntimeException e) {
            stored.forEach(storage::delete);
            throw e;
        }
    }

    private TenantContext country() {
        Country country = countries.findByCode(props.publicSite().countryCode())
                .orElseThrow(() -> new IllegalStateException("The public website's country does not exist: " + props.publicSite().countryCode()));
        return TenantContext.country(country.getId());
    }

    private static boolean isRobot(String honeypot) {
        return honeypot != null && !honeypot.isBlank();
    }

    private static boolean missing(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private static String keep(List<String> stored, String name) {
        stored.add(name);
        return name;
    }
}
