package com.ndzela.admin.user.application;

import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.tenant.model.TenantContext;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.user.domain.User;

import org.springframework.stereotype.Component;

// Which staff accounts the current request may see.
@Component
public class UserScope {
    public boolean canSee(User target) {
        TenantContext context = TenantHolder.current();
        return switch (context.mode()) {
            case ALL -> true;
            case COUNTRY -> target.getCountries().stream().anyMatch(country -> country.getId().equals(context.countryId()));
            case NONE -> false;
        };
    }

    public User requireVisible(User target) {
        if (!canSee(target)) {
            throw new ResourceNotFoundException("User not found.");
        }
        return target;
    }
}
