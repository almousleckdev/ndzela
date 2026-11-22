package com.ndzela.admin.recruitment.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// A driver application from the website.
public record ApplicationRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Pattern(regexp = "^[+0-9 ().-]{6,30}$", message = "Invalid phone number.") String phone,
        @Email @Size(max = 255) String email,
        @NotNull @Past LocalDate birthDate,
        @NotNull @Min(0) @Max(60) Integer drivingExperienceYears,
        @Size(max = 100) String city,
        @Size(max = 255) String address,
        @Size(max = 50) String licenceNumber,
        @Size(max = 2000) String message,
        @Size(max = 200) String website) {}
