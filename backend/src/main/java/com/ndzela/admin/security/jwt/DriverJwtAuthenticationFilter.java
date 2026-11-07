package com.ndzela.admin.security.jwt;

import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.security.model.DriverPrincipal;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Authenticates driver-app requests (under /api/v1/driver) from a driver token
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverJwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String DRIVER_PATH = "/api/v1/driver/";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final DriverRepository drivers;
    private final AppProperties props;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith(DRIVER_PATH);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(header.substring(BEARER_PREFIX.length()));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            DriverClaims claims = jwtService.parseDriverToken(token);
            Driver driver = TenantHolder.callAs(TenantContext.country(claims.countryId()), () -> drivers.findById(claims.driverId()).orElse(null));
            if (driver == null || !driver.getCountry().isActive() || !canUseTheApp(driver.getStatus())) {
                return;
            }
            if (!java.util.Objects.equals(driver.getSessionId(), claims.sessionId())) {
                return;
            }
            DriverPrincipal principal = new DriverPrincipal(driver.getId(), claims.countryId(), driver.getStatus(), driver.isMustChangePassword(),
                    driver.getFirstName() + " " + driver.getLastName());
            boolean locked = props.driverApp().locks(driver.getUnpaidDays());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authoritiesOf(principal, locked)));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected driver token: {}", e.getMessage());
        }
    }

    private static boolean canUseTheApp(DriverStatus status) {
        return status == DriverStatus.ACTIVE || status == DriverStatus.SUSPENDED;
    }

    // A first login only allows changing the password; a suspended driver, or one with too many unpaid days
    private static List<GrantedAuthority> authoritiesOf(DriverPrincipal principal, boolean locked) {
        List<GrantedAuthority> authorities = new ArrayList<>(List.of(new SimpleGrantedAuthority("driver:session")));
        if (!principal.mustChangePassword()) {
            authorities.add(new SimpleGrantedAuthority("driver:self"));
            authorities.add(new SimpleGrantedAuthority("driver:pay"));
            if (principal.status() == DriverStatus.ACTIVE && !locked) {
                authorities.add(new SimpleGrantedAuthority("driver:contract"));
            }
        }
        return authorities;
    }
}
