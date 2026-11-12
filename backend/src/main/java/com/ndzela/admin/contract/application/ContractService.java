package com.ndzela.admin.contract.application;

import com.ndzela.admin.common.security.Actor;
import com.ndzela.admin.contract.domain.snapshot.SnapshotBailleur;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.api.dto.ContractTerms;
import com.ndzela.admin.contract.application.model.ContractSeed;
import com.ndzela.admin.contract.domain.Contract;
import com.ndzela.admin.contract.domain.ContractRepository;
import com.ndzela.admin.contract.domain.snapshot.ContractSnapshot;
import com.ndzela.admin.contract.domain.ContractStatus;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.vehicle.application.VehicleService;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService {
    private final ContractRepository contracts;
    private final ContractNumbering numbering;
    private final CountryRepository countries;
    private final VehicleService vehicleService;
    private final ObjectMapper mapper;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public ContractTerms preview(UUID vehicleId, BigDecimal deposit) {
        UUID countryId = TenantHolder.requireCountry();
        requireTemplate(country(countryId));
        return ContractTerms.of(today(countryId), vehicleService.get(vehicleId).dailyRent(), deposit);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ContractResponse open(ContractSeed seed) {
        Country country = country(seed.countryId());
        requireTemplate(country);
        ContractTerms terms = ContractTerms.of(today(seed.countryId()), seed.dailyRent(), seed.deposit());
        Contract contract = Contract.builder()
                .contractNumber(numbering.next(country))
                .driverId(seed.driverId())
                .vehicleId(seed.vehicleId())
                .status(ContractStatus.ACTIVE)
                .templateVersion(country.getContractTemplate())
                .startDate(terms.startDate()).endDate(terms.endDate()).billableDays(terms.billableDays())
                .dailyRent(terms.dailyRent()).deposit(terms.deposit()).totalPrice(terms.totalPrice())
                .snapshot(write(seed.snapshot().withBailleur(bailleur(country))))
                .createdBy(Actor.name())
                .build();
        try {
            contracts.saveAndFlush(contract);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException("CONTRACT_ALREADY_ACTIVE", "This driver or this vehicle already has an active contract.");
        }
        audit.record("CONTRACT_CREATED", "CONTRACT", contract.getId().toString(), contract.getContractNumber(), seed.countryId(),
                "deposit " + terms.deposit().toPlainString() + ", rent " + terms.dailyRent().toPlainString());
        return ContractResponse.from(contract);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ContractResponse amend(UUID contractId, BigDecimal newDeposit, ContractSnapshot current) {
        Contract contract = loadActive(contractId);
        BigDecimal deposit = ContractTerms.of(contract.getStartDate(), contract.getDailyRent(), newDeposit).deposit();
        BigDecimal previous = contract.getDeposit();
        contract.setSnapshot(write(current.withBailleur(readSnapshot(contract).bailleur())));
        contract.setDeposit(deposit);
        touch(contract);
        audit.record("CONTRACT_UPDATED", "CONTRACT", contract.getId().toString(), contract.getContractNumber(), contract.getCountryId(),
                "revision " + contract.getRevision() + ", deposit " + previous.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() + " -> " + deposit.toPlainString());
        return ContractResponse.from(contract);
    }

    // The same contract moves to another car; the rent, the dates and the number stay as signed.
    @Transactional(propagation = Propagation.MANDATORY)
    public ContractResponse changeVehicle(UUID contractId, UUID vehicleId, ContractSnapshot current) {
        Contract contract = loadActive(contractId);
        UUID previous = contract.getVehicleId();
        contract.setVehicleId(vehicleId);
        contract.setSnapshot(write(current.withBailleur(readSnapshot(contract).bailleur())));
        touch(contract);
        audit.record("CONTRACT_VEHICLE_CHANGED", "CONTRACT", contract.getId().toString(), contract.getContractNumber(), contract.getCountryId(),
                "revision " + contract.getRevision() + ", vehicle " + previous + " -> " + vehicleId);
        return ContractResponse.from(contract);
    }

    // Moves the start date; the end date follows (1 248 billable days). The caller checks that nothing is recorded yet.
    @Transactional(propagation = Propagation.MANDATORY)
    public ContractResponse reschedule(UUID contractId, LocalDate newStart) {
        Contract contract = loadActive(contractId);
        LocalDate previous = contract.getStartDate();
        ContractTerms terms = ContractTerms.of(newStart, contract.getDailyRent(), contract.getDeposit());
        contract.setStartDate(terms.startDate());
        contract.setEndDate(terms.endDate());
        touch(contract);
        audit.record("CONTRACT_RESCHEDULED", "CONTRACT", contract.getId().toString(), contract.getContractNumber(), contract.getCountryId(),
                "revision " + contract.getRevision() + ", start " + previous + " -> " + newStart);
        return ContractResponse.from(contract);
    }

    private Contract loadActive(UUID contractId) {
        Contract contract = contracts.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not found."));
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessRuleViolationException("CONTRACT_NOT_ACTIVE", "Only an active contract can be updated.");
        }
        return contract;
    }

    private void touch(Contract contract) {
        contract.setRevision(contract.getRevision() + 1);
        contract.setUpdatedAt(OffsetDateTime.now());
        contract.setUpdatedBy(Actor.name());
        contracts.save(contract);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteOfDriver(UUID driverId) {
        remove(contracts.findAllByDriverId(driverId));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteOfVehicle(UUID vehicleId) {
        remove(contracts.findAllByVehicleId(vehicleId));
    }

    private void remove(List<Contract> found) {
        for (Contract contract : found) {
            audit.record("CONTRACT_DELETED", "CONTRACT", contract.getId().toString(), contract.getContractNumber(), contract.getCountryId(), "status " + contract.getStatus());
        }
        contracts.deleteAll(found);
        contracts.flush();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void terminateActive(UUID driverId, String reason) {
        contracts.findByDriverIdAndStatus(driverId, ContractStatus.ACTIVE).ifPresent(contract -> {
            contract.setStatus(ContractStatus.TERMINATED);
            contract.setEndedAt(OffsetDateTime.now());
            contract.setEndReason(reason);
            contracts.save(contract);
            audit.record("CONTRACT_TERMINATED", "CONTRACT", contract.getId().toString(), contract.getContractNumber(), contract.getCountryId(), "reason: " + reason);
        });
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> ofDriver(UUID driverId) {
        return contracts.findAllByDriverIdOrderByCreatedAtDescIdDesc(driverId).stream().map(ContractResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> activeIds(int page, int size) {
        return contracts.findAllByStatus(ContractStatus.ACTIVE, org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt", "id")))
                .map(Contract::getId).getContent();
    }

    @Transactional(readOnly = true)
    public ContractResponse get(UUID id) {
        return ContractResponse.from(load(id));
    }

    @Transactional(readOnly = true)
    public Map<UUID, ContractResponse> activeOf(Collection<UUID> driverIds) {
        if (driverIds.isEmpty()) {
            return Map.of();
        }
        return contracts.findAllByDriverIdInAndStatus(driverIds, ContractStatus.ACTIVE).stream()
                .collect(Collectors.toMap(Contract::getDriverId, ContractResponse::from, (a, b) -> a));
    }

    Contract load(UUID id) {
        return contracts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Contract not found."));
    }

    ContractSnapshot readSnapshot(Contract contract) {
        try {
            return mapper.readValue(contract.getSnapshot(), ContractSnapshot.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unreadable contract snapshot " + contract.getId(), e);
        }
    }

    private String write(ContractSnapshot snapshot) {
        try {
            return mapper.writeValueAsString(snapshot);
        } catch (JacksonException e) {
            throw new IllegalStateException(e);
        }
    }

    private static SnapshotBailleur bailleur(Country c) {
        return new SnapshotBailleur(c.getLegalName(), c.getLegalForm(), c.getHeadOffice(), c.getRegistryNumber(), c.getLegalPhone(), c.getLegalEmail(), c.getSigningCity());
    }

    private Country country(UUID id) {
        return countries.findById(id).orElseThrow();
    }

    private static void requireTemplate(Country country) {
        if (country.getContractTemplate() == null || country.getContractPrefix() == null) {
            throw new BusinessRuleViolationException("CONTRACT_TEMPLATE_UNAVAILABLE", "There is no contract template for this country yet.");
        }
    }

    private LocalDate today(UUID countryId) {
        return LocalDate.now(ZoneId.of(country(countryId).getTimezone()));
    }
}
