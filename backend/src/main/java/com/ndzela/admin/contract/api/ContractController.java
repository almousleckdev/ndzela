package com.ndzela.admin.contract.api;

import com.ndzela.admin.contract.api.dto.ContractPreviewRequest;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.api.dto.ContractTerms;
import com.ndzela.admin.contract.application.ContractPdfService;
import com.ndzela.admin.contract.application.ContractService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contracts;
    private final ContractPdfService pdfs;

    @GetMapping
    @PreAuthorize("hasAuthority('contracts:read')")
    public List<ContractResponse> ofDriver(@RequestParam UUID driverId) {
        return contracts.ofDriver(driverId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('contracts:read')")
    public ContractResponse get(@PathVariable UUID id) {
        return contracts.get(id);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('contracts:print')")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"contrat.pdf\"").body(pdfs.pdf(id));
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('drivers:create')")
    public ContractTerms preview(@Valid @RequestBody ContractPreviewRequest request) {
        return contracts.preview(request.vehicleId(), request.deposit());
    }
}
