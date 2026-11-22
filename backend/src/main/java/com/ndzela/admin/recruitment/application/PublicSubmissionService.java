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
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

// What visitors of the public website send.
@Service
@RequiredArgsConstructor
public class PublicSubmissionService {
    private static final Duration SUBMISSION_WINDOW = Duration.ofHours(1);
    private static final int MIN_AGE = 30;
    private static final int MAX_AGE = 55;
    private static final int MIN_EXPERIENCE_YEARS = 5;

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
        if (missing(uploads.photo()) || missing(uploads.licence()) || missing(uploads.id()) || missing(uploads.seegReceipt())) {
            throw new BusinessRuleViolationException("DOCUMENTS_REQUIRED", "The photo, the driving licence, the identity document and the SEEG receipt are required.");
        }
        requireEligible(request);
        limiter.check("public-application", visitor, props.publicSite().maxSubmissionsPerHour(), SUBMISSION_WINDOW);
        List<String> stored = new ArrayList<>();
        String countryCode = props.publicSite().countryCode();
        try {
            String photo = keep(stored, storage.save(uploads.photo(), true, countryCode));
            String licence = keep(stored, storage.save(uploads.licence(), false, countryCode));
            String id = keep(stored, storage.save(uploads.id(), false, countryCode));
            String proof = missing(uploads.addressProof()) ? null : keep(stored, storage.save(uploads.addressProof(), false, countryCode));
            String seeg = keep(stored, storage.save(uploads.seegReceipt(), false, countryCode));
            TenantHolder.callAs(country(), () -> {
                store.saveApplication(request, photo, licence, id, proof, seeg);
                return null;
            });
        } catch (RuntimeException e) {
            stored.forEach(storage::delete);
            throw e;
        }
    }

    // The recruitment criteria: 30 to 55 years old and at least 5 years of driving.
    private static void requireEligible(ApplicationRequest request) {
        int age = Period.between(request.birthDate(), LocalDate.now()).getYears();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new BusinessRuleViolationException("AGE_NOT_ELIGIBLE", "Candidates must be between " + MIN_AGE + " and " + MAX_AGE + " years old.");
        }
        if (request.drivingExperienceYears() < MIN_EXPERIENCE_YEARS) {
            throw new BusinessRuleViolationException("EXPERIENCE_TOO_LOW", "Candidates need at least " + MIN_EXPERIENCE_YEARS + " years of driving experience.");
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
