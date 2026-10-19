package com.ndzela.admin.payment.api;

import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.common.web.dto.ReasonRequest;
import com.ndzela.admin.payment.api.dto.AccountDetail;
import com.ndzela.admin.payment.api.dto.AccountRow;
import com.ndzela.admin.payment.api.dto.EntryResponse;
import com.ndzela.admin.payment.api.dto.PaymentResult;
import com.ndzela.admin.payment.api.dto.RecordPaymentRequest;
import com.ndzela.admin.payment.api.dto.PaymentSummary;
import com.ndzela.admin.payment.api.dto.WaiverRequest;
import com.ndzela.admin.payment.api.dto.OperatorResponse;
import com.ndzela.admin.payment.application.PaymentOperatorService;
import com.ndzela.admin.payment.application.PaymentQueryService;
import com.ndzela.admin.payment.application.PaymentReceiptService;
import com.ndzela.admin.payment.application.model.PaymentReceiptFile;
import com.ndzela.admin.payment.application.PaymentService;
import com.ndzela.admin.payment.domain.LedgerEntryType;
import com.ndzela.admin.payment.domain.PaymentMethod;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService payments;
    private final PaymentQueryService queries;
    private final PaymentOperatorService operators;
    private final PaymentReceiptService receipts;

    // The mobile-money operators of the caller's country
    @GetMapping("/api/v1/payments/operators")
    @PreAuthorize("hasAuthority('payments:read')")
    public List<OperatorResponse> operators() {
        return operators.listActive();
    }

    @GetMapping("/api/v1/payments/accounts")
    @PreAuthorize("hasAuthority('payments:read')")
    public PageResponse<AccountRow> accounts(
            @RequestParam(defaultValue = "ALL") String filter, @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidTo, @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return queries.accounts(filter, q, lateFrom, lateTo, paidFrom, paidTo, sort, page, size);
    }

    @GetMapping("/api/v1/payments/accounts/{driverId}")
    @PreAuthorize("hasAuthority('payments:read')")
    public AccountDetail account(@PathVariable UUID driverId) {
        return queries.account(driverId);
    }

    @GetMapping("/api/v1/payments/accounts/export")
    @PreAuthorize("hasAuthority('payments:export')")
    public ResponseEntity<byte[]> exportAccounts(
            @RequestParam(defaultValue = "ALL") String filter, @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidTo, @RequestParam(defaultValue = "XLSX") ExportFormat format) {
        return queries.exportAccounts(filter, q, lateFrom, lateTo, paidFrom, paidTo, format).response();
    }

    @GetMapping("/api/v1/payments/summary")
    @PreAuthorize("hasAuthority('payments:read')")
    public PaymentSummary summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return queries.summary(from, to);
    }

    @GetMapping("/api/v1/payments/history")
    @PreAuthorize("hasAuthority('payments:read')")
    public PageResponse<EntryResponse> history(
            @RequestParam(required = false) UUID driverId, @RequestParam(required = false) LedgerEntryType type,
            @RequestParam(required = false) PaymentMethod method, @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String scope, @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return queries.history(driverId, type, method, q, from, to, scope, sort, page, size);
    }

    @GetMapping("/api/v1/payments/export")
    @PreAuthorize("hasAuthority('payments:export')")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) UUID driverId, @RequestParam(required = false) LedgerEntryType type,
            @RequestParam(required = false) PaymentMethod method, @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam(defaultValue = "XLSX") ExportFormat format) {
        return queries.export(driverId, type, method, q, from, to, format).response();
    }

    @GetMapping("/api/v1/payments/{id}/receipt")
    @PreAuthorize("hasAuthority('payments:read')")
    public ResponseEntity<byte[]> receipt(@PathVariable UUID id) {
        PaymentReceiptFile receipt = receipts.receiptOfEntry(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(receipt.filename()).build().toString()).body(receipt.pdf());
    }

    @PostMapping("/api/v1/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('payments:create')")
    public PaymentResult record(@Valid @RequestBody RecordPaymentRequest request) {
        return payments.record(request);
    }

    @PostMapping("/api/v1/payments/{id}/reverse")
    @PreAuthorize("hasAuthority('payments:reverse')")
    public PaymentResult reverse(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return payments.reverse(id, request.reason());
    }

    @PostMapping("/api/v1/arrears/waivers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('arrears:manage')")
    public PaymentResult waive(@Valid @RequestBody WaiverRequest request) {
        return payments.waive(request);
    }
}
