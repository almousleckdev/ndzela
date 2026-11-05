package com.ndzela.admin.security;

import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.security.jwt.DriverJwtAuthenticationFilter;
import com.ndzela.admin.security.jwt.JwtAuthenticationFilter;
import com.ndzela.admin.tenant.TenantContextResolver;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final DriverJwtAuthenticationFilter driverJwtAuthFilter;
    private final AppProperties props;
    private final TenantContextResolver tenantContextResolver;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/api/v1/driver/auth/login", "/api/v1/driver/auth/refresh", "/api/v1/driver/auth/forgot-password", "/api/v1/driver/auth/reset-password", "/api/v1/driver/auth/verify-reset-code", "/api/callback", "/api/v1/public/**", "/api/v1/webhooks/ocpp", "/v3/api-docs/**", "/swagger-ui/**", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((req, res, ex) -> exceptionResolver.resolveException(req, res, null, ex))
                        .accessDeniedHandler((req, res, ex) -> exceptionResolver.resolveException(req, res, null, ex)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(driverJwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // Not a bean: it must run after authentication, inside the security chain.
                .addFilterAfter(new TenantContextFilter(tenantContextResolver, exceptionResolver), DriverJwtAuthenticationFilter.class);
        return http.build();
    }

    // Two tiers. The web admin may call everything, with its cookie. The public website (same site as the API, so the browser would send it the refresh cookie)
    // may only post its forms, never with credentials: a flaw in the website must not be able to reach a signed-in session.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration website = new CorsConfiguration();
        website.setAllowedOrigins(props.publicSite().allowedOrigins());
        website.setAllowedMethods(List.of("POST", "OPTIONS"));
        website.setAllowedHeaders(List.of("Content-Type", "X-Request-ID"));
        website.setAllowCredentials(false);

        CorsConfiguration admin = new CorsConfiguration();
        admin.setAllowedOrigins(props.corsAllowedOrigins());
        admin.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        admin.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-ID", TenantContextFilter.COUNTRY_HEADER));
        admin.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // The first matching pattern wins: the narrow one goes first
        source.registerCorsConfiguration("/api/v1/public/**", website);
        source.registerCorsConfiguration("/**", admin);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
