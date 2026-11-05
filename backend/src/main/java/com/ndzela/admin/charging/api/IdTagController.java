package com.ndzela.admin.charging.api;

import com.ndzela.admin.charging.api.dto.IdTagRow;
import com.ndzela.admin.charging.application.IdTagLinkService;
import com.ndzela.admin.charging.application.IdTagQueryService;
import com.ndzela.admin.charging.application.IdTagZellaSync;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.common.web.dto.ReasonRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Which driver is behind each charging account (idTag): review the suggestions, confirm, unlink.
@RestController
@RequestMapping("/api/v1/charging/id-tags")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('charging:link')")
public class IdTagController {
    private final IdTagQueryService queries;
    private final IdTagLinkService links;
    private final IdTagZellaSync zella;

    @GetMapping
    public List<IdTagRow> list(@RequestParam(defaultValue = "SUGGESTED") IdTagStatus status) {
        return queries.list(status);
    }

    @PostMapping("/{id}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable UUID id, Authentication authentication) {
        links.confirm(id, authentication.getName());
        zella.reconcile();
    }

    @PostMapping("/{id}/unlink")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request, Authentication authentication) {
        links.unlink(id, request.reason(), authentication.getName());
        zella.reconcile();
    }

    /** Asks the Zella app now about the drivers not matched yet. */
    @PostMapping("/lookup")
    public Map<String, Integer> lookup() {
        return Map.of("stored", links.lookupMissing());
    }
}
