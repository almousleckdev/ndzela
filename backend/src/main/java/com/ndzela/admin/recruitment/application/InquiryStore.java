package com.ndzela.admin.recruitment.application;

import com.ndzela.admin.common.text.Strings;
import com.ndzela.admin.notification.application.NotificationService;
import com.ndzela.admin.recruitment.api.dto.ApplicationRequest;
import com.ndzela.admin.recruitment.api.dto.ContactRequest;
import com.ndzela.admin.recruitment.domain.ApplicationStatus;
import com.ndzela.admin.recruitment.domain.ContactMessage;
import com.ndzela.admin.recruitment.domain.ContactMessageRepository;
import com.ndzela.admin.recruitment.domain.DriverApplication;
import com.ndzela.admin.recruitment.domain.DriverApplicationRepository;
import com.ndzela.admin.recruitment.domain.MessageStatus;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Saves what the website sends, in the website's country, and tells the staff who handle it.
@Service
@RequiredArgsConstructor
public class InquiryStore {
    private final DriverApplicationRepository applications;
    private final ContactMessageRepository messages;
    private final NotificationService notifications;

    @Transactional
    public void saveMessage(ContactRequest request) {
        messages.save(ContactMessage.builder().name(request.name().trim()).email(Strings.blankToNull(request.email())).phone(Strings.blankToNull(request.phone()))
                .subject(request.subject().trim()).message(request.message().trim()).status(MessageStatus.NEW).build());
        notifications.notifyHolders("inquiries:read", TenantHolder.requireCountry(), null, "CONTACT_MESSAGE", "Nouveau message du site", request.name().trim() + " : " + request.subject().trim());
    }

    @Transactional
    public void saveApplication(ApplicationRequest request, String photo, String licence, String id, String addressProof) {
        applications.save(DriverApplication.builder().firstName(request.firstName().trim()).lastName(request.lastName().trim()).phone(request.phone().trim())
                .email(Strings.blankToNull(request.email())).birthDate(request.birthDate()).city(Strings.blankToNull(request.city())).address(Strings.blankToNull(request.address()))
                .licenceNumber(Strings.blankToNull(request.licenceNumber())).message(Strings.blankToNull(request.message()))
                .photoFile(photo).licenceFile(licence).idFile(id).addressProofFile(addressProof).status(ApplicationStatus.NEW).build());
        notifications.notifyHolders("inquiries:read", TenantHolder.requireCountry(), null, "DRIVER_APPLICATION", "Nouvelle candidature chauffeur", request.firstName().trim() + " " + request.lastName().trim());
    }
}
