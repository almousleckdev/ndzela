package com.ndzela.admin.security;

import com.ndzela.admin.common.exception.CountryInactiveException;
import com.ndzela.admin.common.exception.InvalidCountryException;
import com.ndzela.admin.tenant.model.TenantContext;
import com.ndzela.admin.security.model.DriverPrincipal;
import com.ndzela.admin.tenant.TenantContextResolver;
import com.ndzela.admin.tenant.TenantHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

// Sets the TenantContext for the request, right after the JWT filter has authenticated the caller.
public class TenantContextFilter extends OncePerRequestFilter {
    public static final String COUNTRY_HEADER = "X-Country";

    private final TenantContextResolver resolver;
    private final HandlerExceptionResolver exceptionResolver;

    public TenantContextFilter(TenantContextResolver resolver, HandlerExceptionResolver exceptionResolver) {
        this.resolver = resolver;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            chain.doFilter(request, response);
            return;
        }

        TenantContext context;
        if (authentication.getPrincipal() instanceof DriverPrincipal driver) {
            context = TenantContext.country(driver.countryId());
        } else if (authentication.getPrincipal() instanceof SecurityUser principal) {
            try {
                context = resolver.resolve(principal.getUser(), request.getHeader(COUNTRY_HEADER));
            } catch (InvalidCountryException | CountryInactiveException e) {
                exceptionResolver.resolveException(request, response, null, e);
                return;
            }
        } else {
            chain.doFilter(request, response);
            return;
        }

        TenantHolder.set(context);
        try {
            chain.doFilter(request, response);
        } finally {
            TenantHolder.clear();
        }
    }
}
