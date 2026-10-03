package com.ndzela.admin.ocpp.api;

import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.ocpp.application.OcppSessionQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class OcppSessionController {
    private final OcppSessionQueryService sessions;

    @GetMapping("/api/v1/charging/sessions")
    @PreAuthorize("hasAuthority('charging:read')")
    public PageResponse<SessionRow> list(@RequestParam(required = false) String status, @RequestParam(required = false) String q,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return sessions.list(status, q, from, to, page, size);
    }
}
