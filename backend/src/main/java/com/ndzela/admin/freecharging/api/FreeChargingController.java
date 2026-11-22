package com.ndzela.admin.freecharging.api;

import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.freecharging.api.dto.AgreementCreateRequest;
import com.ndzela.admin.freecharging.api.dto.AgreementUpdateRequest;
import com.ndzela.admin.freecharging.api.dto.WindowRequest;
import com.ndzela.admin.freecharging.application.AgreementView;
import com.ndzela.admin.freecharging.application.DriverOption;
import com.ndzela.admin.freecharging.application.FreeChargingAdministration;
import com.ndzela.admin.freecharging.application.FreeChargingUse;
import com.ndzela.admin.freecharging.application.WindowView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Staff manage the drivers who charge free on Sundays.
@RestController
@RequestMapping("/api/v1/free-charging")
@RequiredArgsConstructor
public class FreeChargingController {
    private final FreeChargingAdministration administration;

    @GetMapping("/agreements")
    @PreAuthorize("hasAuthority('free_charging:read')")
    public List<AgreementView> list() {
        return administration.list();
    }

    @PostMapping("/agreements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('free_charging:manage')")
    public AgreementView create(@Valid @RequestBody AgreementCreateRequest request) {
        return administration.create(request.driverId(), request.startsOn(), request.endsOn(), request.conditions());
    }

    @PutMapping("/agreements/{id}")
    @PreAuthorize("hasAuthority('free_charging:manage')")
    public AgreementView update(@PathVariable UUID id, @Valid @RequestBody AgreementUpdateRequest request) {
        return administration.update(id, request.startsOn(), request.endsOn(), request.conditions());
    }

    @PostMapping("/agreements/{id}/suspend")
    @PreAuthorize("hasAuthority('free_charging:manage')")
    public AgreementView suspend(@PathVariable UUID id) {
        return administration.suspend(id);
    }

    @PostMapping("/agreements/{id}/resume")
    @PreAuthorize("hasAuthority('free_charging:manage')")
    public AgreementView resume(@PathVariable UUID id) {
        return administration.resume(id);
    }

    @PostMapping("/agreements/{id}/revoke")
    @PreAuthorize("hasAuthority('free_charging:manage')")
    public AgreementView revoke(@PathVariable UUID id) {
        return administration.revoke(id);
    }

    @GetMapping("/drivers")
    @PreAuthorize("hasAuthority('free_charging:manage')")
    public PageResponse<DriverOption> drivers(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "ALL") String membership,
                                              @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return administration.drivers(q, membership, page, size);
    }

    @GetMapping("/window")
    @PreAuthorize("hasAuthority('free_charging:read')")
    public WindowView window() {
        return administration.window();
    }

    @PutMapping("/window")
    @PreAuthorize("hasAuthority('free_charging:manage')")
    public WindowView changeWindow(@Valid @RequestBody WindowRequest request) {
        return administration.changeWindow(request.day(), request.from(), request.effectiveUntil());
    }

    @GetMapping("/usage")
    @PreAuthorize("hasAuthority('free_charging:read')")
    public PageResponse<FreeChargingUse> usage(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return administration.usage(page, size);
    }
}
