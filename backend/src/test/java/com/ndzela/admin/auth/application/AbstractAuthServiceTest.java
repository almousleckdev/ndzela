package com.ndzela.admin.auth.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.infrastructure.email.EmailProvider;
import com.ndzela.admin.security.LoginAttemptService;
import com.ndzela.admin.security.jwt.JwtService;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.*;

// An AuthService over mocked collaborators, with an active user who has MFA enrolled.
@ExtendWith(MockitoExtension.class)
abstract class AbstractAuthServiceTest {
    protected static final String EMAIL = "test@ndzela.com";

    @Mock protected UserRepository userRepository;
    @Mock protected LoginChallengeService challenges;
    @Mock protected OtpService otpService;
    @Mock protected MfaService mfaService;
    @Mock protected JwtService jwtService;
    @Mock protected RefreshTokenService refreshTokens;
    @Mock protected EmailProvider emailProvider;
    @Mock protected LoginAttemptService loginAttempts;
    @Mock protected PasswordEncoder passwordEncoder;
    @Mock protected AuditService audit;
    @Mock protected com.ndzela.admin.common.ratelimit.RateLimiter limiter;

    protected AuthService authService;
    protected User user;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("decoy-hash");
        lenient().when(limiter.tryAcquire(anyString(), any(), anyInt(), any())).thenReturn(true);
        authService = new AuthService(userRepository, challenges, otpService, mfaService, jwtService,
                refreshTokens, emailProvider, loginAttempts, passwordEncoder, audit, limiter, com.ndzela.admin.TestProperties.create());
        user = User.builder().email(EMAIL).password("hash").status(UserStatus.ACTIVE).mfaEnabled(true).build();
    }
}
