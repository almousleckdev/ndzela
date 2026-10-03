package com.ndzela.admin.recruitment.api;

import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.recruitment.api.dto.ApplicationResponse;
import com.ndzela.admin.recruitment.api.dto.ApplicationStatusRequest;
import com.ndzela.admin.recruitment.api.dto.MessageResponse;
import com.ndzela.admin.recruitment.application.InquiryService;
import com.ndzela.admin.recruitment.domain.ApplicationStatus;
import com.ndzela.admin.recruitment.domain.MessageStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

// Staff side of the public website
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InquiryController {
    private final InquiryService inquiries;

    @GetMapping("/applications")
    @PreAuthorize("hasAuthority('inquiries:read')")
    public PageResponse<ApplicationResponse> applications(@RequestParam(required = false) ApplicationStatus status, @RequestParam(required = false) String q,
                                                          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return inquiries.listApplications(status, q, page, size);
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAuthority('inquiries:read')")
    public ApplicationResponse application(@PathVariable UUID id) {
        return inquiries.getApplication(id);
    }

    @GetMapping("/applications/{id}/files/{kind}")
    @PreAuthorize("hasAuthority('inquiries:read')")
    public ResponseEntity<Resource> file(@PathVariable UUID id, @PathVariable String kind) {
        var content = inquiries.applicationFile(id, kind);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType())).cacheControl(CacheControl.noStore()).body(content.resource());
    }

    @PostMapping("/applications/{id}/status")
    @PreAuthorize("hasAuthority('inquiries:manage')")
    public ApplicationResponse updateApplication(@PathVariable UUID id, @Valid @RequestBody ApplicationStatusRequest request) {
        return inquiries.updateApplication(id, request);
    }

    @GetMapping("/contact-messages")
    @PreAuthorize("hasAuthority('inquiries:read')")
    public PageResponse<MessageResponse> messages(@RequestParam(required = false) MessageStatus status, @RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return inquiries.listMessages(status, q, page, size);
    }

    @PostMapping("/contact-messages/{id}/handle")
    @PreAuthorize("hasAuthority('inquiries:manage')")
    public MessageResponse handle(@PathVariable UUID id) {
        return inquiries.handleMessage(id);
    }

    @GetMapping("/inquiries/new-count")
    @PreAuthorize("hasAuthority('inquiries:read')")
    public Map<String, Long> newCount() {
        return Map.of("count", inquiries.newCount());
    }
}
