package com.ndzela.admin.driverapp.api;

import com.ndzela.admin.infrastructure.storage.FileContent;
import com.ndzela.admin.driverapp.api.dto.DriverAccountResponse;
import com.ndzela.admin.driverapp.api.dto.DriverContractResponse;
import com.ndzela.admin.driverapp.api.dto.DriverDocumentResponse;
import com.ndzela.admin.driverapp.api.dto.DriverMeResponse;
import com.ndzela.admin.driverapp.api.dto.DriverVehicleResponse;
import com.ndzela.admin.driverapp.api.dto.SendMessageRequest;
import com.ndzela.admin.driverapp.api.dto.SupportContactsResponse;
import com.ndzela.admin.driverapp.application.DriverAppService;
import com.ndzela.admin.driverapp.application.SupportService;
import com.ndzela.admin.payment.api.dto.OperatorResponse;
import com.ndzela.admin.payment.application.PaymentOperatorService;
import com.ndzela.admin.security.model.DriverPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/driver")
@RequiredArgsConstructor
public class DriverAppController {
    private final DriverAppService app;
    private final PaymentOperatorService operators;
    private final SupportService support;

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('driver:self')")
    public DriverMeResponse me(@AuthenticationPrincipal DriverPrincipal driver) {
        return app.me(driver.driverId());
    }

    @GetMapping("/me/photo")
    @PreAuthorize("hasAuthority('driver:self')")
    public ResponseEntity<Resource> photo(@AuthenticationPrincipal DriverPrincipal driver) {
        FileContent content = app.photo(driver.driverId());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType())).cacheControl(CacheControl.noStore()).body(content.resource());
    }

    @GetMapping("/vehicle")
    @PreAuthorize("hasAuthority('driver:contract')")
    public DriverVehicleResponse vehicle(@AuthenticationPrincipal DriverPrincipal driver) {
        return app.vehicle(driver.driverId());
    }

    @GetMapping("/contract")
    @PreAuthorize("hasAuthority('driver:contract')")
    public DriverContractResponse contract(@AuthenticationPrincipal DriverPrincipal driver) {
        return app.contract(driver.driverId());
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('driver:self')")
    public List<DriverDocumentResponse> documents(@AuthenticationPrincipal DriverPrincipal driver) {
        return app.documents(driver.driverId());
    }

    @PostMapping(value = "/documents/{kind}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('driver:self')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addDocument(@AuthenticationPrincipal DriverPrincipal driver, @PathVariable String kind, @RequestPart("file") MultipartFile file) {
        app.addDocument(driver.driverId(), kind, file);
    }

    @GetMapping("/documents/contract")
    @PreAuthorize("hasAuthority('driver:self')")
    public ResponseEntity<byte[]> contractPdf(@AuthenticationPrincipal DriverPrincipal driver) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contrat.pdf\"").body(app.contractPdf(driver.driverId()));
    }

    @GetMapping("/support")
    @PreAuthorize("hasAuthority('driver:self')")
    public SupportContactsResponse support(@AuthenticationPrincipal DriverPrincipal driver) {
        return support.contacts(driver.driverId());
    }

    @PostMapping("/support/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('driver:self')")
    public void sendMessage(@AuthenticationPrincipal DriverPrincipal driver, @Valid @RequestBody SendMessageRequest request) {
        support.send(driver.driverId(), request.message());
    }

    @PostMapping("/account/deletion-request")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('driver:self')")
    public void requestDeletion(@AuthenticationPrincipal DriverPrincipal driver) {
        support.requestDeletion(driver.driverId());
    }

    @GetMapping("/account")
    @PreAuthorize("hasAuthority('driver:self')")
    public DriverAccountResponse account(@AuthenticationPrincipal DriverPrincipal driver) {
        return app.account(driver.driverId());
    }

    @GetMapping("/payments/operators")
    @PreAuthorize("hasAuthority('driver:pay')")
    public List<OperatorResponse> paymentOperators() {
        return operators.listActive();
    }
}
