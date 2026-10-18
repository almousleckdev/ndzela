package com.ndzela.admin.recruitment.application;

import com.ndzela.admin.country.application.CountryBrand;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.infrastructure.email.EmailProvider;
import com.ndzela.admin.recruitment.domain.ApplicationStatus;
import com.ndzela.admin.recruitment.domain.DriverApplication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

// Tells a candidate the answer to the application
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationNotifier {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm", Locale.FRENCH);

    private final EmailProvider email;
    private final CountryRepository countries;

    // Returns whether the e-mail actually went out: the caller must not tell staff the candidate was notified otherwise.
    public boolean send(DriverApplication application) {
        Country country = countries.findById(application.getCountryId()).orElseThrow();
        CountryBrand brand = CountryBrand.ofCountry(country.getCode());
        boolean accepted = application.getStatus() == ApplicationStatus.ACCEPTED;
        String when = accepted ? WHEN.format(application.getInterviewAt().atZoneSameInstant(ZoneId.of(country.getTimezone()))) : "";
        try {
            email.sendHtmlEmail(application.getEmail(), brand.name() + " : votre candidature", "application-decision-email",
                    Map.of("name", application.getFirstName(), "accepted", accepted, "when", when, "message", application.getCandidateMessage() == null ? "" : application.getCandidateMessage(),
                            "brand", brand.name(), "company", brand.company(), "color", brand.accentHex(), "logo", brand.logoDataUri()));
            return true;
        } catch (RuntimeException e) {
            log.warn("Could not send the application answer to {}", application.getId(), e);
            return false;
        }
    }
}
