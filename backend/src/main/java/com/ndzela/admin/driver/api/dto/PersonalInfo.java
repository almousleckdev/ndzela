package com.ndzela.admin.driver.api.dto;

import com.ndzela.admin.driver.domain.Civility;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.IdType;
import com.ndzela.admin.driver.domain.LicenceCategory;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

// Step 1 of the wizard
public record PersonalInfo(
        @NotNull Civility civility,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 100) String firstName,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Size(max = 100) String birthPlace,
        @NotBlank @Size(max = 60) String nationality,
        @NotNull IdType idType,
        @NotBlank @Size(max = 50) String idNumber,
        @NotNull @PastOrPresent LocalDate idIssueDate,
        @NotBlank @Size(max = 100) String idIssuePlace,
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Size(max = 100) String district,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Pattern(regexp = "^\\+[1-9][0-9 ]{7,19}$", message = "Phone must be international, e.g. +241 62821624") String phonePrimary,
        @Pattern(regexp = "^\\+[1-9][0-9 ]{7,19}$", message = "Phone must be international, e.g. +241 62821624") String phoneSecondary,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 50) String licenceNumber,
        @NotNull @PastOrPresent LocalDate licenceIssueDate,
        @NotNull LicenceCategory licenceCategory
) {
    public static PersonalInfo from(Driver d) {
        return new PersonalInfo(d.getCivility(), d.getLastName(), d.getFirstName(), d.getBirthDate(), d.getBirthPlace(), d.getNationality(),
                d.getIdType(), d.getIdNumber(), d.getIdIssueDate(), d.getIdIssuePlace(), d.getAddress(), d.getDistrict(), d.getCity(),
                d.getPhonePrimary(), d.getPhoneSecondary(), d.getEmail(), d.getLicenceNumber(), d.getLicenceIssueDate(), d.getLicenceCategory());
    }
}
