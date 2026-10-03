package com.ndzela.admin.recruitment.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// A message from the website's contact page.
public record ContactRequest(
        @NotBlank @Size(max = 100) String name,
        @Email @Size(max = 255) String email,
        @Pattern(regexp = "^$|^[+0-9 ().-]{6,30}$", message = "Invalid phone number.") String phone,
        @NotBlank @Size(max = 150) String subject,
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 200) String website) {}
