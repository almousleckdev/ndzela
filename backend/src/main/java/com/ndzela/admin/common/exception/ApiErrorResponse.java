package com.ndzela.admin.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private OffsetDateTime timestamp;
    private int status;
    private String code;
    private String message;
    private String path;
    private String requestId;
    private List<ValidationError> errors;
}
