package com.ndzela.admin.country.api;

import com.ndzela.admin.country.api.dto.CountryResponse;
import com.ndzela.admin.country.application.CountryService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public List<CountryResponse> list() {
        return countries.list();
    }
}
