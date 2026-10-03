package com.ndzela.admin.payment.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.country.application.CountryBrand;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.infrastructure.email.EmailProvider;
import com.ndzela.admin.notification.application.NotificationService;
import com.ndzela.admin.payment.domain.DayStatus;
import com.ndzela.admin.payment.application.model.AccountDay;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.domain.ArrearsEventRepository;
import com.ndzela.admin.payment.domain.LedgerEntry;
import com.ndzela.admin.payment.domain.LedgerEntryRepository;
import com.ndzela.admin.payment.domain.LedgerEntryType;
import com.ndzela.admin.vehicle.application.VehicleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

// Brings one contract's arrears up to date
@Slf4j
@Service
@RequiredArgsConstructor
public class ArrearsEvaluator {
    static final String SUSPENSION_REASON = "Impayés : loyer non réglé depuis plus de 72 heures (suspension automatique)";
    private static final EnumSet<ArrearsLevel> STEPS = EnumSet.of(ArrearsLevel.YELLOW, ArrearsLevel.DARK_YELLOW, ArrearsLevel.RED);

    private final ContractService contractService;
    private final DriverArrearsService driverArrears;
    private final AccountService accounts;
    private final LedgerEntryRepository ledger;
    private final ArrearsEventRepository events;
    private final CountryRepository countries;
    private final VehicleService vehicleService;
    private final NotificationService notifications;
    private final EmailProvider email;
    private final AuditService audit;

    @Transactional
    public void evaluate(UUID contractId, Instant now) {
        ContractResponse contract = contractService.get(contractId);
        DriverRef driver = driverArrears.lock(contract.driverId());
        AccountState state = accounts.state(driver.countryId(), contract, now);

        BigDecimal penalty = countries.findById(driver.countryId()).map(Country::getLatePenalty).orElse(BigDecimal.ZERO);
        if (penalty.signum() > 0 && !state.penaltyDays().isEmpty()) {
            state.penaltyDays().forEach(day -> ledger.save(LedgerEntry.builder().contractId(contractId).driverId(driver.id()).type(LedgerEntryType.PENALTY).amount(penalty).businessDay(day)
                    .recordedBy("system").createdAt(OffsetDateTime.ofInstant(now, java.time.ZoneOffset.UTC)).build()));
            ledger.flush();
            state = accounts.state(driver.countryId(), contract, now);
        }
        var zone = accounts.zoneOf(driver.countryId());
        driverArrears.apply(driver.id(), state.level(), state.overdueAmount(), state.oldestUnpaidDueAt() == null ? null : state.oldestUnpaidDueAt().atZone(zone).toOffsetDateTime(), state.unpaid(), state.paidOnTime() + state.paidLate());

        ArrearsLevel highestNew = ArrearsLevel.NONE;
        for (AccountDay day : state.days()) {
            if (day.status() != DayStatus.UNPAID) {
                continue;
            }
            ArrearsLevel reached = AccountCalculator.levelOf(day.dueAt(), now);
            for (ArrearsLevel step : STEPS) {
                if (step.compareTo(reached) <= 0 && events.recordOnce(driver.countryId(), contractId, driver.id(), day.day(), step.name()) == 1) {
                    audit.record("ARREARS_LEVEL_REACHED", "DRIVER", driver.id().toString(), driver.fullName(), driver.countryId(), step + ", rent of " + day.day());
                    if (step.compareTo(highestNew) > 0) {
                        highestNew = step;
                    }
                }
            }
        }
        if (highestNew != ArrearsLevel.NONE) {
            act(driver, contract, state, highestNew);
        }
    }

    private void act(DriverRef driver, ContractResponse contract, AccountState state, ArrearsLevel level) {
        sendEmail(driver, state, level);
        if (level == ArrearsLevel.RED) {
            boolean suspended = driverArrears.suspendForArrears(driver.id(), SUSPENSION_REASON);
            String plate = vehicleService.responsesByIds(java.util.List.of(contract.vehicleId())).values().stream().findFirst().map(v -> v.fleetNumber() + " " + v.plate()).orElse("");
            notifications.notifyHolders("arrears:manage", driver.countryId(), null, "ARREARS_RED", "Impayés de plus de 72 heures",
                    driver.fullName() + " : " + state.overdueAmount().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() + " FCFA impayés"
                            + (suspended ? ", chauffeur suspendu automatiquement" : "") + ". Bloquez le véhicule " + plate + " dans l'outil GPS.");
        }
    }

    private void sendEmail(DriverRef driver, AccountState state, ArrearsLevel level) {
        CountryBrand brand = CountryBrand.ofCountry(countries.findById(driver.countryId()).map(Country::getCode).orElse(null));
        String subject = brand.name() + " : " + switch (level) {
            case YELLOW -> "rappel de paiement de votre loyer";
            case DARK_YELLOW -> "rappel urgent, loyer impayé";
            default -> "votre compte est suspendu pour impayés";
        };
        String message = switch (level) {
            case YELLOW -> "Votre loyer n'a pas été réglé avant l'échéance. Merci de régulariser dès aujourd'hui.";
            case DARK_YELLOW -> "Votre loyer est impayé depuis plus de 48 heures. Une pénalité peut s'appliquer. Régularisez immédiatement.";
            default -> "Votre loyer est impayé depuis plus de 72 heures : votre compte est suspendu et votre véhicule peut être bloqué. Régularisez l'intégralité pour le débloquer.";
        };
        try {
            email.sendHtmlEmail(driver.email(), subject, "arrears-email", Map.of("name", driver.firstName(), "message", message, "brand", brand.name(), "company", brand.company(), "color", brand.accentHex(),
                    "amount", state.overdueAmount().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() + " FCFA"));
        } catch (RuntimeException e) {
            log.warn("Could not send the arrears email to driver {}", driver.id(), e);
        }
    }
}
