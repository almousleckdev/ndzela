package com.ndzela.admin.payment.application;

import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.country.api.dto.CountryResponse;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.tenant.model.TenantContext;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Runs the arrears evaluation over every active contract of every active country.
@Slf4j
@Service
@RequiredArgsConstructor
public class ArrearsService {
    private static final int PAGE = 100;

    private final CountryService countryService;
    private final ContractService contractService;
    private final ArrearsEvaluator evaluator;

    public int evaluateAll(Instant now) {
        int evaluated = 0;
        for (CountryResponse country : countryService.list()) {
            evaluated += TenantHolder.callAs(TenantContext.country(country.id()), () -> evaluateCountry(now));
        }
        return evaluated;
    }

    private int evaluateCountry(Instant now) {
        int count = 0;
        for (int page = 0; ; page++) {
            List<UUID> ids = contractService.activeIds(page, PAGE);
            for (UUID id : ids) {
                try {
                    evaluator.evaluate(id, now);
                    count++;
                } catch (RuntimeException e) {
                    log.error("Arrears evaluation failed for contract {}", id, e);
                }
            }
            if (ids.size() < PAGE) {
                return count;
            }
        }
    }
}
