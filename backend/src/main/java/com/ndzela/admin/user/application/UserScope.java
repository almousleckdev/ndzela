package com.ndzela.admin.user.application;

import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.tenant.TenantContext;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.user.domain.User;

import org.springframework.stereotype.Component;

/** Which staff accounts the current request may see. */
@Component
public class UserScope {

    public boolean canSee(User target) {
        TenantContext context = TenantHolder.current();
        return switch (context.mode()) {
            case ALL -> true;
            case COUNTRY -> target.getCountry() != null && target.getCountry().getId().equals(context.countryId());
            case NONE -> false;
        };
    }

    /** Answers "not found" for another country's account. */
    public User requireVisible(User target) {
        if (!canSee(target)) {
            throw new ResourceNotFoundException("User not found.");
        }
        return target;
    }
}
