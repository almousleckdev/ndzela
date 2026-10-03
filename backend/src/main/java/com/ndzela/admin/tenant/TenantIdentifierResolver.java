package com.ndzela.admin.tenant;

import com.ndzela.admin.tenant.model.TenantContext;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

// Gives Hibernate the session's country
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {
    static final UUID ALL = new UUID(0L, 0L);
    static final UUID NONE = new UUID(-1L, -1L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        TenantContext context = TenantHolder.current();
        return switch (context.mode()) {
            case COUNTRY -> context.countryId();
            case ALL -> ALL;
            case NONE -> NONE;
        };
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        return ALL.equals(tenantId);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
