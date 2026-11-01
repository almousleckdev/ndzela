package com.ndzela.admin.recruitment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageReplyRequest(
        @NotBlank(message = "The reply is required") @Size(max = 2000, message = "Max 2000 characters") String message
) {}
