package com.ndzela.admin.freecharging.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.security.Actor;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.config.properties.FreeChargingProperties;
import com.ndzela.admin.freecharging.domain.FreeChargingWindowSetting;
import com.ndzela.admin.freecharging.domain.FreeChargingWindowSettingRepository;
import com.ndzela.admin.freecharging.domain.WindowSetting;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

// The free window of the current country: the setting staff chose, or the default (Sunday, the whole day) while none was saved.
@Service
@RequiredArgsConstructor
public class FreeChargingWindowService {
    private final FreeChargingWindowSettingRepository settings;
    private final AppProperties app;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public WindowSetting current() {
        if (!TenantHolder.current().isCountry()) {
            return defaults();
        }
        return settings.findFirstBy().map(FreeChargingWindowSetting::toWindow).orElseGet(this::defaults);
    }

    @Transactional
    public WindowSetting update(DayOfWeek day, LocalTime from, LocalTime until) {
        WindowSetting next = validated(day, from, until);
        UUID countryId = TenantHolder.requireCountry();
        WindowSetting previous = current();
        FreeChargingWindowSetting row = settings.findFirstBy().orElseGet(FreeChargingWindowSetting::new);
        row.setDayOfWeek(next.day());
        row.setFromTime(next.from());
        row.setUntilTime(next.until());
        row.setUpdatedBy(Actor.name());
        settings.saveAndFlush(row);
        audit.record("FREE_WINDOW_CHANGED", "COUNTRY", countryId.toString(), "Free charging window", countryId,
                "from " + describe(previous) + " to " + describe(next));
        return next;
    }

    private WindowSetting defaults() {
        FreeChargingProperties properties = app.freeCharging();
        return new WindowSetting(properties.day(), properties.from(), properties.until());
    }

    private static WindowSetting validated(DayOfWeek day, LocalTime from, LocalTime until) {
        try {
            return new WindowSetting(day, from, until);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleViolationException("FREE_WINDOW_INVALID", e.getMessage());
        }
    }

    public static String describe(WindowSetting window) {
        return window.day() + " " + window.from() + "-" + window.displayedUntil();
    }
}
