package com.ndzela.admin.country.application;

import com.ndzela.admin.common.export.ExportHeader;
import com.ndzela.admin.common.export.ExportTable;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// The top of every export: the country's brand (name and logo), the dates covered, and the export time in the country's own time zone
@Component
@RequiredArgsConstructor
public class ExportBranding {
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Africa/Libreville");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final CountryRepository countries;

    public ExportHeader header(LocalDate from, LocalDate to) {
        CountryBrand brand = CountryBrand.ofCountry(country() == null ? null : country().getCode());
        return new ExportHeader(brand.name(), brand.logo(), period(from, to), STAMP.format(LocalDateTime.now(zone())));
    }

    // The day a moment falls on in the country's time zone (a recharge at 00:30 in Libreville is not yesterday's)
    public LocalDate day(OffsetDateTime moment) {
        return moment.atZoneSameInstant(zone()).toLocalDate();
    }

    public String time(OffsetDateTime moment) {
        return TIME.format(moment.atZoneSameInstant(zone()));
    }

    public String dateTime(OffsetDateTime moment) {
        return moment == null ? "" : STAMP.format(moment.atZoneSameInstant(zone())).replace(" à ", " ");
    }

    static String period(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return "toutes les dates";
        }
        if (from != null && from.equals(to)) {
            return "le " + DATE.format(from);
        }
        if (from == null) {
            return "jusqu'au " + DATE.format(to);
        }
        return to == null ? "depuis le " + DATE.format(from) : "du " + DATE.format(from) + " au " + DATE.format(to);
    }

    private ZoneId zone() {
        Country country = country();
        return country == null ? DEFAULT_ZONE : ZoneId.of(country.getTimezone());
    }

    private Country country() {
        var id = TenantHolder.current().countryId();
        return id == null ? null : countries.findById(id).orElse(null);
    }
}
