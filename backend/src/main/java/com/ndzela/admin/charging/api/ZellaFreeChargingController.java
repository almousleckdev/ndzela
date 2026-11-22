package com.ndzela.admin.charging.api;

import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.freecharging.api.dto.AgreementUpdateRequest;
import com.ndzela.admin.freecharging.api.dto.WindowRequest;
import com.ndzela.admin.freecharging.api.dto.ZellaAgreementCreateRequest;
import com.ndzela.admin.freecharging.application.AgreementView;
import com.ndzela.admin.freecharging.application.DriverOption;
import com.ndzela.admin.freecharging.application.Eligibility;
import com.ndzela.admin.freecharging.application.FreeChargingAdministration;
import com.ndzela.admin.freecharging.application.FreeChargingEligibility;
import com.ndzela.admin.freecharging.application.FreeChargingUse;
import com.ndzela.admin.freecharging.application.PartnerActor;
import com.ndzela.admin.freecharging.application.WindowView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

// The Zella app asks whether a customer may charge free now, and its dashboard manages the agreements (server to server, X-Api-Key; each management call names the person in X-Actor). The routes are public for the JWT filters: the key is checked in ZellaPartnerAccess.
@RestController
@RequestMapping("/api/v1/partners/zella")
@RequiredArgsConstructor
public class ZellaFreeChargingController {
    private static final String KEY = "X-Api-Key";
    private static final String ACTOR = "X-Actor";

    private final ZellaPartnerAccess access;
    private final FreeChargingEligibility eligibility;
    private final FreeChargingAdministration administration;

    @GetMapping("/customers/{customerId}/free-charging")
    public Eligibility eligibility(@PathVariable String customerId, @RequestHeader(value = KEY, required = false) String key) {
        return access.callForCustomer("free-read", customerId, key, () -> eligibility.check(customerId, Instant.now()));
    }

    @GetMapping("/free-charging/agreements")
    public List<AgreementView> list(@RequestHeader(value = KEY, required = false) String key) {
        return access.call("free-manage", key, administration::list);
    }

    @PostMapping("/free-charging/agreements")
    @ResponseStatus(HttpStatus.CREATED)
    public AgreementView create(@Valid @RequestBody ZellaAgreementCreateRequest request, @RequestHeader(value = KEY, required = false) String key,
                                @RequestHeader(value = ACTOR, required = false) String actor) {
        return manage(key, actor, () -> administration.create(request.driverId(), request.customerId(), request.startsOn(), request.endsOn(), request.conditions()));
    }

    @PutMapping("/free-charging/agreements/{id}")
    public AgreementView update(@PathVariable UUID id, @Valid @RequestBody AgreementUpdateRequest request, @RequestHeader(value = KEY, required = false) String key,
                                @RequestHeader(value = ACTOR, required = false) String actor) {
        return manage(key, actor, () -> administration.update(id, request.startsOn(), request.endsOn(), request.conditions()));
    }

    @PostMapping("/free-charging/agreements/{id}/suspend")
    public AgreementView suspend(@PathVariable UUID id, @RequestHeader(value = KEY, required = false) String key, @RequestHeader(value = ACTOR, required = false) String actor) {
        return manage(key, actor, () -> administration.suspend(id));
    }

    @PostMapping("/free-charging/agreements/{id}/resume")
    public AgreementView resume(@PathVariable UUID id, @RequestHeader(value = KEY, required = false) String key, @RequestHeader(value = ACTOR, required = false) String actor) {
        return manage(key, actor, () -> administration.resume(id));
    }

    @PostMapping("/free-charging/agreements/{id}/revoke")
    public AgreementView revoke(@PathVariable UUID id, @RequestHeader(value = KEY, required = false) String key, @RequestHeader(value = ACTOR, required = false) String actor) {
        return manage(key, actor, () -> administration.revoke(id));
    }

    @GetMapping("/free-charging/drivers")
    public List<DriverOption> drivers(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "20") int size,
                                      @RequestHeader(value = KEY, required = false) String key) {
        return access.call("free-manage", key, () -> administration.drivers(q, size));
    }

    @GetMapping("/free-charging/window")
    public WindowView window(@RequestHeader(value = KEY, required = false) String key) {
        return access.call("free-manage", key, administration::window);
    }

    @PutMapping("/free-charging/window")
    public WindowView changeWindow(@Valid @RequestBody WindowRequest request, @RequestHeader(value = KEY, required = false) String key,
                                   @RequestHeader(value = ACTOR, required = false) String actor) {
        return manage(key, actor, () -> administration.changeWindow(request.day(), request.from(), request.effectiveUntil()));
    }

    @GetMapping("/free-charging/usage")
    public PageResponse<FreeChargingUse> usage(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                               @RequestHeader(value = KEY, required = false) String key) {
        return access.call("free-manage", key, () -> administration.usage(page, size));
    }

    private <T> T manage(String key, String actor, Supplier<T> action) {
        return access.call("free-manage", key, () -> PartnerActor.runAs(actor, action));
    }
}
