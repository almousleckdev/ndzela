package com.ndzela.admin.country.api.dto;

import com.ndzela.admin.country.domain.Country;

import java.util.UUID;

public record CountryResponse(
        UUID id,
        String code,
        String name,
        String currencyCode,
        String phoneCode,
        String timezone,
        String defaultLanguage) {
    public static CountryResponse from(Country country) {
        return new CountryResponse(country.getId(), country.getCode(), country.getName(), country.getCurrencyCode(),
                country.getPhoneCode(), country.getTimezone(), country.getDefaultLanguage());
    }
}
