package com.ndzela.admin.driver.application;

import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverPause;
import com.ndzela.admin.driver.domain.DriverPauseRepository;
import com.ndzela.admin.driver.domain.PauseCause;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// A driver who is inactive or without a car keeps the contract; no rent is charged from the day the pause starts until the day it ends.
@Service
@RequiredArgsConstructor
public class DriverPauseService {
    private static final LocalDate OPEN_END = LocalDate.of(9999, 12, 31);

    private final DriverPauseRepository pauses;
    private final CountryService countryService;

    @Transactional(propagation = Propagation.MANDATORY)
    public void open(Driver driver, PauseCause cause, String reason) {
        if (pauses.findByDriverIdAndCauseAndEndedOnIsNull(driver.getId(), cause).isEmpty()) {
            pauses.save(DriverPause.builder().driverId(driver.getId()).cause(cause).startedOn(today(driver)).reason(reason == null ? null : reason.trim()).build());
        }
    }

    // A pause lifted the same day it began never happened.
    @Transactional(propagation = Propagation.MANDATORY)
    public void closeIfOpen(Driver driver, PauseCause cause) {
        pauses.findByDriverIdAndCauseAndEndedOnIsNull(driver.getId(), cause).ifPresent(pause -> {
            LocalDate today = today(driver);
            if (today.isAfter(pause.getStartedOn())) {
                pause.setEndedOn(today);
                pauses.save(pause);
            } else {
                pauses.delete(pause);
            }
        });
    }

    // The days without rent, both ends included.
    @Transactional(readOnly = true)
    public List<LocalDate[]> ranges(UUID driverId) {
        return pauses.findAllByDriverId(driverId).stream()
                .map(p -> new LocalDate[]{p.getStartedOn(), p.getEndedOn() == null ? OPEN_END : p.getEndedOn().minusDays(1)}).toList();
    }

    private LocalDate today(Driver driver) {
        return LocalDate.now(countryService.zoneOf(driver.getCountryId()));
    }
}
