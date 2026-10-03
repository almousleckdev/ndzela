package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.text.Strings;
import com.ndzela.admin.common.security.Actor;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.payment.api.dto.DeclareIncidentRequest;
import com.ndzela.admin.payment.api.dto.IncidentResponse;
import com.ndzela.admin.payment.domain.DayIncident;
import com.ndzela.admin.payment.domain.DayIncidentKind;
import com.ndzela.admin.payment.domain.DayIncidentRepository;
import com.ndzela.admin.payment.domain.IncidentCause;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Half days and stops
@Service
@RequiredArgsConstructor
public class DayIncidentService {
    static final int HALF_DAY_FROM_HOURS = 4;
    static final int STOP_AFTER_HOURS = 8;

    private final DayIncidentRepository incidents;
    private final DriverArrearsService driverArrears;
    private final ContractService contractService;
    private final AccountService accounts;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<IncidentResponse> list(UUID driverId) {
        return incidents.findAllByDriverIdOrderByDayDescCreatedAtDesc(driverId).stream().map(IncidentResponse::from).toList();
    }

    @Transactional
    public IncidentResponse declare(UUID driverId, DeclareIncidentRequest request) {
        DriverRef driver = driverArrears.lock(driverId);
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        if (contract == null) {
            throw new BusinessRuleViolationException("NO_ACTIVE_CONTRACT", "This driver has no active contract.");
        }
        LocalDate today = LocalDate.now(accounts.zoneOf(driver.countryId()));
        validate(request, contract, today);
        if (incidents.findByContractIdAndDayAndCancelledAtIsNull(contract.id(), request.day()).isPresent()) {
            throw new BusinessRuleViolationException("INCIDENT_ALREADY_DECLARED", "An incident is already declared for this day.");
        }
        DayIncident saved = incidents.saveAndFlush(DayIncident.builder().driverId(driverId).contractId(contract.id()).day(request.day()).kind(request.kind()).cause(request.cause())
                .hours(request.hours()).note(Strings.blankToNull(request.note())).createdBy(Actor.name()).build());
        accounts.refresh(driver, contract, Instant.now());
        audit.record("DAY_INCIDENT_DECLARED", "DRIVER", driverId.toString(), driver.fullName(), driver.countryId(),
                request.kind() + " on " + request.day() + (request.hours() == null ? "" : ", " + request.hours() + " h") + (saved.getNote() == null ? "" : ", " + saved.getNote()));
        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentResponse cancel(UUID driverId, UUID incidentId) {
        DriverRef driver = driverArrears.lock(driverId);
        DayIncident incident = incidents.findByIdAndDriverId(incidentId, driverId).orElseThrow(() -> new ResourceNotFoundException("Incident not found."));
        if (!incident.isLive()) {
            throw new BusinessRuleViolationException("INCIDENT_NOT_LIVE", "This incident is already cancelled.");
        }
        incident.setCancelledAt(OffsetDateTime.now());
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        if (contract != null) {
            accounts.refresh(driver, contract, Instant.now());
        }
        audit.record("DAY_INCIDENT_CANCELLED", "DRIVER", driverId.toString(), driver.fullName(), driver.countryId(), incident.getKind() + " on " + incident.getDay());
        return IncidentResponse.from(incident);
    }

    private static void validate(DeclareIncidentRequest request, ContractResponse contract, LocalDate today) {
        LocalDate last = contract.endDate().isBefore(today) ? contract.endDate() : today;
        if (!request.day().isAfter(contract.startDate()) || request.day().isAfter(last)) {
            throw new BusinessRuleViolationException("DAY_OUT_OF_CONTRACT", "The day must be after the contract starts and not in the future.");
        }
        if (request.day().getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BusinessRuleViolationException("DAY_NOT_BILLABLE", "Sunday is a rest day: no rent is due.");
        }
        if (request.cause() == IncidentCause.ELECTRICITY) {
            validateElectricity(request);
        } else if (request.note() == null || request.note().isBlank()) {
            throw new BusinessRuleViolationException("INCIDENT_NOTE_REQUIRED", "A note is required when the cause is not an electricity cut.");
        }
    }

    // 4 to 8 hours is a half day; more than 8 hours is a stop. The length must agree with the kind chosen.
    private static void validateElectricity(DeclareIncidentRequest request) {
        Integer hours = request.hours();
        boolean consistent = hours != null && (request.kind() == DayIncidentKind.HALF_DAY ? hours >= HALF_DAY_FROM_HOURS && hours <= STOP_AFTER_HOURS : hours > STOP_AFTER_HOURS);
        if (!consistent) {
            throw new BusinessRuleViolationException("INCIDENT_HOURS_INVALID", "An electricity cut of " + HALF_DAY_FROM_HOURS + " to " + STOP_AFTER_HOURS
                    + " hours is a half day; more than " + STOP_AFTER_HOURS + " hours is a stop.");
        }
    }
}
