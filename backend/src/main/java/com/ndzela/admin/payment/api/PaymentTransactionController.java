package com.ndzela.admin.payment.api;

import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.payment.api.dto.TransactionRow;
import com.ndzela.admin.payment.application.PaymentTransactionService;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/payments/transactions")
@RequiredArgsConstructor
public class PaymentTransactionController {
    private final PaymentTransactionService transactions;

    @GetMapping
    @PreAuthorize("hasAuthority('payments:read')")
    public PageResponse<TransactionRow> list(@RequestParam(required = false) String status, @RequestParam(required = false) String method, @RequestParam(required = false) String q, @RequestParam(required = false) String scope, @RequestParam(required = false) java.util.UUID driverId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return transactions.list(status, method, q, scope, driverId, from, to, Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }
}
