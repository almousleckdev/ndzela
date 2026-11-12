package com.ndzela.admin.charging.api;

import com.ndzela.admin.charging.api.dto.PatternCase;
import com.ndzela.admin.charging.api.dto.SuspensionCase;
import com.ndzela.admin.charging.application.PatternCaseService;
import com.ndzela.admin.charging.application.SuspensionCaseService;
import com.ndzela.admin.common.web.dto.PageResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SuspensionCaseController {
    private final SuspensionCaseService cases;
    private final PatternCaseService patterns;

    @GetMapping("/api/v1/charging/suspension-cases")
    @PreAuthorize("hasAuthority('arrears:manage')")
    public PageResponse<SuspensionCase> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return cases.list(page, size);
    }

    @GetMapping("/api/v1/charging/suspension-cases/patterns")
    @PreAuthorize("hasAuthority('arrears:manage')")
    public List<PatternCase> patterns() {
        return patterns.list();
    }
}
