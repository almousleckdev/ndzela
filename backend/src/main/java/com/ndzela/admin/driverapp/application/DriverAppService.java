package com.ndzela.admin.driverapp.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractPdfService;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.driver.application.DriverFileService;
import com.ndzela.admin.infrastructure.storage.FileContent;
import com.ndzela.admin.driver.domain.DriverFileKind;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driverapp.api.dto.DriverAccountResponse;
import com.ndzela.admin.driverapp.api.dto.DriverContractResponse;
import com.ndzela.admin.driverapp.api.dto.DriverDocumentResponse;
import com.ndzela.admin.driverapp.api.dto.DriverMeResponse;
import com.ndzela.admin.driverapp.api.dto.DriverVehicleResponse;
import com.ndzela.admin.payment.api.dto.AccountDetail;
import com.ndzela.admin.payment.application.AccountQueryService;
import com.ndzela.admin.vehicle.application.VehicleService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.Period;
import java.util.List;
import java.util.UUID;

// What a signed-in driver sees of their own data; the driver always comes from the token
@Service
@RequiredArgsConstructor
public class DriverAppService {
    private final DriverRepository drivers;
    private final ContractService contracts;
    private final ContractPdfService contractPdfs;
    private final VehicleService vehicles;
    private final AccountQueryService accounts;
    private final DriverFileService files;
    private final AppProperties props;

    @Transactional(readOnly = true)
    public DriverMeResponse me(UUID driverId) {
        return DriverMeResponse.from(drivers.findById(driverId).orElseThrow());
    }

    // The driver's own profile picture; not found when none was uploaded.
    @Transactional(readOnly = true)
    public FileContent photo(UUID driverId) {
        return files.read(driverId, DriverFileKind.PHOTO);
    }

    @Transactional(readOnly = true)
    public DriverVehicleResponse vehicle(UUID driverId) {
        ContractResponse contract = activeContract(driverId);
        return DriverVehicleResponse.from(vehicles.responsesByIds(List.of(contract.vehicleId())).get(contract.vehicleId()));
    }

    @Transactional(readOnly = true)
    public DriverContractResponse contract(UUID driverId) {
        ContractResponse contract = activeContract(driverId);
        Driver driver = drivers.findById(driverId).orElseThrow();
        int months = (int) Period.between(contract.startDate(), contract.endDate().plusDays(1)).toTotalMonths();
        return new DriverContractResponse(contract.contractNumber(), contract.startDate(), contract.endDate(), months, contract.dailyRent(), driver.getCountry().getCurrencyCode(),
                accounts.progress(driverId));
    }

    @Transactional(readOnly = true)
    public List<DriverDocumentResponse> documents(UUID driverId) {
        Driver driver = drivers.findById(driverId).orElseThrow();
        return List.of(
                new DriverDocumentResponse("CONTRACT", !contracts.ofDriver(driverId).isEmpty()),
                available("LICENCE", DriverFileKind.LICENCE, driver),
                available("ID", DriverFileKind.ID, driver),
                available("ADDRESS_PROOF", DriverFileKind.ADDRESS_PROOF, driver),
                available("RESIDENCE_CERTIFICATE", DriverFileKind.RESIDENCE_CERTIFICATE, driver));
    }

    // The driver's own contract (the active one, else the latest) as a PDF.
    @Transactional
    public byte[] contractPdf(UUID driverId) {
        var own = contracts.ofDriver(driverId);
        if (own.isEmpty()) {
            throw new BusinessRuleViolationException("NO_CONTRACT", "This driver has no contract.");
        }
        var chosen = own.stream().filter(c -> "ACTIVE".equals(c.status().name())).findFirst().orElse(own.get(0));
        return contractPdfs.pdf(chosen.id());
    }

    @Transactional(readOnly = true)
    public DriverAccountResponse account(UUID driverId) {
        Driver driver = drivers.findById(driverId).orElseThrow();
        AccountDetail account = accounts.account(driverId);
        return new DriverAccountResponse(account.unpaid(), account.overdueAmount().setScale(0, RoundingMode.HALF_UP), driver.getCountry().getCurrencyCode(), props.driverApp().locks(account.unpaid()),
                driver.getStatus() == com.ndzela.admin.driver.domain.DriverStatus.SUSPENDED, account.weekClosesAt(), account.weekSettled(), accounts.payable(driverId).options());
    }

    private ContractResponse activeContract(UUID driverId) {
        ContractResponse contract = contracts.activeOf(List.of(driverId)).get(driverId);
        if (contract == null) {
            throw new BusinessRuleViolationException("NO_ACTIVE_CONTRACT", "This driver has no active contract.");
        }
        return contract;
    }

    private static DriverDocumentResponse available(String kind, DriverFileKind fileKind, Driver driver) {
        String file = fileKind.fileOf(driver);
        return new DriverDocumentResponse(kind, file != null && !file.isBlank());
    }
}
