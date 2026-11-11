package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.ZellaCommand;
import com.ndzela.admin.charging.domain.ZellaCommandRepository;
import com.ndzela.admin.charging.domain.ZellaCommandStatus;
import com.ndzela.admin.charging.domain.ZellaCommandType;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.infrastructure.zella.ZellaClient;
import com.ndzela.admin.infrastructure.zella.ZellaException;
import com.ndzela.admin.infrastructure.zella.ZellaRateLimitedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

// Sends the queued orders to the Zella app one by one, each in its own transaction; a refused one waits 1, 2, 4, 8 then 15 minutes.
@Slf4j
@Component
@RequiredArgsConstructor
public class ZellaCommandSender {
    private static final int[] BACKOFF_MINUTES = {1, 2, 4, 8, 15};
    private static final int MAX_ERROR = 500;
    private static final String DEFAULT_CURRENCY = "XAF";

    private final ZellaCommandRepository commands;
    private final CountryRepository countries;
    private final ZellaClient zella;
    private final TransactionTemplate transaction;

    /** Returns how many orders Zella accepted. */
    public int sendDue(Instant now) {
        if (!zella.configured()) {
            return 0;
        }
        OffsetDateTime at = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
        List<UUID> due = commands.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAt(ZellaCommandStatus.PENDING, at).stream().map(ZellaCommand::getId).toList();
        int sent = 0;
        for (UUID id : due) {
            if (Boolean.TRUE.equals(transaction.execute(status -> deliver(id, at)))) {
                sent++;
            }
        }
        return sent;
    }

    private boolean deliver(UUID id, OffsetDateTime at) {
        ZellaCommand command = commands.findById(id).orElse(null);
        if (command == null || command.getStatus() != ZellaCommandStatus.PENDING) {
            return false;
        }
        try {
            if (command.getType() == ZellaCommandType.SUSPEND) {
                zella.suspend(command.getCustomerId(), command.getOwedMinor(), currencyOf(command), 0);
            } else {
                zella.restore(command.getCustomerId());
            }
            command.setStatus(ZellaCommandStatus.SENT);
            command.setSentAt(at);
            commands.save(command);
            return true;
        } catch (ZellaException e) {
            command.setAttempts(command.getAttempts() + 1);
            command.setNextAttemptAt(at.plus(waitAfter(command.getAttempts(), e)));
            command.setLastError(String.valueOf(e.getMessage()).substring(0, Math.min(MAX_ERROR, String.valueOf(e.getMessage()).length())));
            commands.save(command);
            log.warn("The Zella app refused the {} order for customer {} (attempt {}): {}", command.getType(), command.getCustomerId(), command.getAttempts(), e.getMessage());
            return false;
        }
    }

    private static Duration waitAfter(int attempts, ZellaException failure) {
        if (failure instanceof ZellaRateLimitedException limited) {
            return limited.retryAfter();
        }
        return Duration.ofMinutes(BACKOFF_MINUTES[Math.min(attempts, BACKOFF_MINUTES.length) - 1]);
    }

    private String currencyOf(ZellaCommand command) {
        return countries.findById(command.getCountryId()).map(Country::getCurrencyCode).orElse(DEFAULT_CURRENCY);
    }
}
