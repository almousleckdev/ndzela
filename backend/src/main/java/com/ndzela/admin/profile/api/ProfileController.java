package com.ndzela.admin.profile.api;

import com.ndzela.admin.common.web.dto.MessageResponse;
import com.ndzela.admin.profile.api.dto.ChangePasswordRequest;
import com.ndzela.admin.profile.api.dto.ProfileResponse;
import com.ndzela.admin.profile.api.dto.ResetMfaRequest;
import com.ndzela.admin.profile.api.dto.UpdateProfileRequest;
import com.ndzela.admin.profile.application.ProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping("/me")
    public ProfileResponse getProfile(Authentication authentication) {
        return profileService.getProfile(authentication.getName());
    }

    @PutMapping("/me")
    public MessageResponse updateProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        profileService.updateProfile(authentication.getName(), request);
        return new MessageResponse("Profile updated successfully.");
    }

    @PutMapping("/password")
    public MessageResponse changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(authentication.getName(), request);
        return new MessageResponse("Password changed successfully. Please sign in again.");
    }

    @PostMapping("/mfa/reset")
    public MessageResponse resetMfa(Authentication authentication, @Valid @RequestBody ResetMfaRequest request) {
        profileService.resetMfa(authentication.getName(), request);
        return new MessageResponse("Authenticator reset. Please sign in again.");
    }

    @PostMapping(path = "/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public java.util.Map<String, String> uploadPicture(Authentication authentication, @RequestParam("file") MultipartFile file) {
        return java.util.Map.of("url", profileService.updatePicture(authentication.getName(), file));
    }

    @DeleteMapping("/picture")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePicture(Authentication authentication) {
        profileService.removePicture(authentication.getName());
    }
}
