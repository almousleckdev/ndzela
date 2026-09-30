package com.ndzela.admin.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

/** Base class of every entity that belongs to one country (vehicles, drivers, contracts, payments...). */
@MappedSuperclass
@EntityListeners(TenantOwnedListener.class)
@Getter
public abstract class TenantOwned {

    @TenantId
    @Column(name = "country_id", nullable = false, updatable = false)
    private UUID countryId;
}
