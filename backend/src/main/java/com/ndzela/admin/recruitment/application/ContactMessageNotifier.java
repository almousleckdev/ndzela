package com.ndzela.admin.recruitment.application;

import com.ndzela.admin.country.application.CountryBrand;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.infrastructure.email.EmailProvider;
import com.ndzela.admin.recruitment.domain.ContactMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

// Sends a staff reply to a website visitor's contact message by e-mail.
@Slf4j
@Component
@RequiredArgsConstructor
public class ContactMessageNotifier {
    private final EmailProvider email;
    private final CountryRepository countries;

    // Returns whether the e-mail actually went out.
    public boolean send(ContactMessage message, String reply) {
        Country country = countries.findById(message.getCountryId()).orElseThrow();
        CountryBrand brand = CountryBrand.ofCountry(country.getCode());
        try {
            email.sendHtmlEmail(message.getEmail(), brand.name() + " : réponse à votre message", "contact-reply-email",
                    Map.of("name", message.getName(), "subject", message.getSubject(), "reply", reply,
                            "brand", brand.name(), "company", brand.company(), "color", brand.accentHex()));
            return true;
        } catch (RuntimeException e) {
            log.warn("Could not send the reply to contact message {}", message.getId(), e);
            return false;
        }
    }
}
