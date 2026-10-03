package com.ndzela.admin.common.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The mandatory explanation of a restrictive action (suspend, block, remove...).
public record ReasonRequest(@NotBlank @Size(max = 255) String reason) {}
