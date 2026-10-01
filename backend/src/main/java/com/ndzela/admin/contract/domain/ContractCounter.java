package com.ndzela.admin.contract.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "contract_counters")
@Getter
@Setter
@NoArgsConstructor
public class ContractCounter {
    @Id
    @Column(name = "country_id")
    private UUID countryId;

    @Column(name = "last_value", nullable = false)
    private long lastValue;
}
