package com.ndzela.admin.payment.application;

import com.ndzela.admin.country.application.CountryBrand;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.infrastructure.email.EmailProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

// The branded e-mail a driver receives about their rent; a failure is logged and never stops the arrears run.
@Slf4j
@Component
@RequiredArgsConstructor
class ArrearsMailer {
    private final CountryRepository countries;
    private final EmailProvider email;

    void send(DriverRef driver, String subjectTail, String message, String amount) {
        CountryBrand brand = CountryBrand.ofCountry(countries.findById(driver.countryId()).map(Country::getCode).orElse(null));
        try {
            email.sendHtmlEmail(driver.email(), brand.name() + " : " + subjectTail, "arrears-email", Map.of("name", driver.firstName(), "message", message, "brand", brand.name(),
                    "company", brand.company(), "color", brand.accentHex(), "logo", brand.logoDataUri(), "amount", amount));
        } catch (RuntimeException e) {
            log.warn("Could not send the arrears email to driver {}", driver.id(), e);
        }
    }
}
