package com.ndzela.admin.recruitment.api;

import com.ndzela.admin.recruitment.api.dto.ApplicationRequest;
import com.ndzela.admin.recruitment.api.dto.ContactRequest;
import com.ndzela.admin.recruitment.application.PublicSubmissionService;
import com.ndzela.admin.recruitment.application.model.ApplicationUploads;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// What the public website sends
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicSiteController {
    private final PublicSubmissionService submissions;

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void contact(@Valid @RequestBody ContactRequest request, HttpServletRequest http) {
        submissions.submitMessage(request, http.getRemoteAddr());
    }

    @PostMapping(value = "/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void apply(@Valid @RequestPart("data") ApplicationRequest request,
                      @RequestPart(value = "photo", required = false) MultipartFile photo,
                      @RequestPart(value = "licence", required = false) MultipartFile licence,
                      @RequestPart(value = "id", required = false) MultipartFile id,
                      @RequestPart(value = "addressProof", required = false) MultipartFile addressProof,
                      HttpServletRequest http) {
        submissions.submitApplication(request, new ApplicationUploads(photo, licence, id, addressProof), http.getRemoteAddr());
    }
}
