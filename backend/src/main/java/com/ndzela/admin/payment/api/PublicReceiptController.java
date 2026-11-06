package com.ndzela.admin.payment.api;

import com.ndzela.admin.payment.api.dto.PublicReceiptResponse;
import com.ndzela.admin.payment.application.PublicReceiptService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// The page the QR code of a receipt opens on the website
@RestController
@RequestMapping("/api/v1/public/receipts")
@RequiredArgsConstructor
public class PublicReceiptController {
    private final PublicReceiptService receipts;

    @GetMapping("/{id}")
    public PublicReceiptResponse get(@PathVariable UUID id, HttpServletRequest http) {
        return receipts.view(id, http.getRemoteAddr());
    }
}
