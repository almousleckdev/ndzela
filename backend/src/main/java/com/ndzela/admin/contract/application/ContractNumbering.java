package com.ndzela.admin.contract.application;

import com.ndzela.admin.contract.domain.ContractCounter;
import com.ndzela.admin.contract.domain.ContractCounterRepository;
import com.ndzela.admin.country.domain.Country;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// The next contract number of a country: its prefix and a sequential counter, locked so two contracts never share one (the caller's transaction).
@Component
@RequiredArgsConstructor
class ContractNumbering {
    private final ContractCounterRepository counters;

    String next(Country country) {
        counters.ensure(country.getId());
        ContractCounter counter = counters.findByCountryId(country.getId()).orElseThrow();
        counter.setLastValue(counter.getLastValue() + 1);
        counters.save(counter);
        return country.getContractPrefix() + String.format("%08d", counter.getLastValue());
    }
}
