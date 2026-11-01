package com.ndzela.admin.bootstrap;

import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.user.domain.Role;
import com.ndzela.admin.user.domain.RoleRepository;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    @Override
    @Transactional
    public void run(String... args) {
        String email = props.bootstrap().initialAdminEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            return;
        }
        String password = props.bootstrap().initialAdminPassword();
        if (password == null || password.isBlank()) {
            log.warn("No user {} and INITIAL_ADMIN_PASSWORD is not set: skipping SUPER_ADMIN creation.", email);
            return;
        }
        Role role = roleRepository.findByName("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role missing: did the Flyway migrations run?"));

        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .status(UserStatus.MFA_SETUP)
                .firstName("Super")
                .lastName("Admin")
                .global(true)
                .roles(new HashSet<>(Set.of(role)))
                .build());
        log.info("Created initial SUPER_ADMIN {}. MFA enrolment is required at first login.", email);
    }
}
