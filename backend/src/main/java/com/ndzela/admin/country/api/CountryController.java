package com.ndzela.admin.country.api;

import com.ndzela.admin.country.api.dto.CountryResponse;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.security.CurrentActor;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countries;

    @GetMapping
    @PreAuthorize("hasAuthority('countries:read')")
    public List<CountryResponse> list(Authentication authentication) {
        return countries.list(CurrentActor.from(authentication));
    }
}
