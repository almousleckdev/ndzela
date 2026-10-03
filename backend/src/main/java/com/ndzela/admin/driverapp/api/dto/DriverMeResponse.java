package com.ndzela.admin.driverapp.api.dto;

import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.payment.application.ProviderPhone;

import java.time.LocalDate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public record DriverMeResponse(String firstName, String lastName, String phone, String localPhone, String email, LocalDate birthDate, String address, String countryCode) {
    public static DriverMeResponse from(Driver driver) {
        String address = Stream.of(driver.getAddress(), driver.getDistrict(), driver.getCity()).filter(part -> part != null && !part.isBlank()).collect(joining(", "));
        return new DriverMeResponse(driver.getFirstName(), driver.getLastName(), driver.getPhonePrimary(), localPhone(driver), driver.getEmail(), driver.getBirthDate(), address, driver.getCountry().getCode());
    }

    // The number as it is dialled inside the country (074111111)
    private static String localPhone(Driver driver) {
        try {
            return ProviderPhone.forProvider(driver.getCountry().getPhoneCode(), driver.getPhonePrimary());
        } catch (RuntimeException e) {
            return driver.getPhonePrimary();
        }
    }
}
