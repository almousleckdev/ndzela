package com.ndzela.admin.driver.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

// A new email and/or a new primary phone; what is left out stays as it is.
public record ChangeContactRequest(
        @Email(message = "Email is not valid") @Size(max = 160, message = "Email is too long") String email,
        @Size(max = 32, message = "Phone is too long") String phone
) {
    @JsonIgnore
    @AssertTrue(message = "Give a new email or a new phone")
    public boolean isAnythingToChange() {
        return (email != null && !email.isBlank()) || (phone != null && !phone.isBlank());
    }
}
