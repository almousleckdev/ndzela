package com.ndzela.admin.driverapp.api;

import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.driverapp.api.dto.DriverPaymentResponse;
import com.ndzela.admin.driverapp.api.dto.StartPaymentRequest;
import com.ndzela.admin.payment.api.dto.PaymentQuote;
import com.ndzela.admin.payment.application.PaymentIntentService;
import com.ndzela.admin.payment.application.PaymentReceiptService;
import com.ndzela.admin.payment.application.model.PaymentReceiptFile;
import com.ndzela.admin.security.model.DriverPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/driver/payments")
@RequiredArgsConstructor
public class DriverPaymentController {
    private final PaymentIntentService payments;
    private final PaymentReceiptService receipts;

    @GetMapping("/quote")
    @PreAuthorize("hasAuthority('driver:pay')")
    public PaymentQuote quote(@AuthenticationPrincipal DriverPrincipal driver, @RequestParam String operator, @RequestParam(required = false) String phone,
                             @RequestParam(required = false) Integer days) {
        return payments.quote(driver.driverId(), operator, phone, days);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('driver:pay')")
    public DriverPaymentResponse start(@AuthenticationPrincipal DriverPrincipal driver, @Valid @RequestBody StartPaymentRequest request) {
        return DriverPaymentResponse.from(payments.start(driver.driverId(), request.operator(), request.phoneNumber(), request.days()), payments.beneficiary(), List.of());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('driver:pay')")
    public DriverPaymentResponse get(@AuthenticationPrincipal DriverPrincipal driver, @PathVariable UUID id) {
        return payments.attemptOf(driver.driverId(), id).map(intent -> DriverPaymentResponse.from(intent, payments.beneficiary(), receipts.daysOfAttempts(List.of(id)).getOrDefault(id, List.of()))).orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("hasAuthority('driver:pay')")
    public ResponseEntity<byte[]> receipt(@AuthenticationPrincipal DriverPrincipal driver, @PathVariable UUID id) {
        PaymentReceiptFile receipt = receipts.receiptOf(driver.driverId(), id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(receipt.filename()).build().toString()).body(receipt.pdf());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('driver:pay')")
    public List<DriverPaymentResponse> history(@AuthenticationPrincipal DriverPrincipal driver) {
        String beneficiary = payments.beneficiary();
        var attempts = payments.recentOf(driver.driverId());
        var days = receipts.daysOfAttempts(attempts.stream().map(intent -> intent.getId()).toList());
        return attempts.stream().map(intent -> DriverPaymentResponse.from(intent, beneficiary, days.getOrDefault(intent.getId(), List.of()))).toList();
    }
}
