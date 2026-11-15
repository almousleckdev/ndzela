package com.ndzela.admin.payment.api;

import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.payment.api.dto.EarningsDriverRow;
import com.ndzela.admin.payment.api.dto.EarningsResponse;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.payment.application.EarningsExportService;
import com.ndzela.admin.payment.application.EarningsService;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// "Total encaissé": everything collected since the start (super admin, admin, comptable).
@RestController
@RequiredArgsConstructor
public class EarningsController {
    private final EarningsService earnings;
    private final EarningsExportService exports;

    @GetMapping("/api/v1/payments/earnings")
    @PreAuthorize("hasAuthority('earnings:read')")
    public EarningsResponse summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return earnings.summary(from, to);
    }

    @GetMapping("/api/v1/payments/earnings/drivers")
    @PreAuthorize("hasAuthority('earnings:read')")
    public PageResponse<EarningsDriverRow> drivers(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                   @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return earnings.drivers(q, from, to, sort, page, size);
    }

    @GetMapping("/api/v1/payments/earnings/export")
    @PreAuthorize("hasAuthority('earnings:read')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(defaultValue = "XLSX") ExportFormat format) {
        return exports.export(from, to, format).response();
    }
}
