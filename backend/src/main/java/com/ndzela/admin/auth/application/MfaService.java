package com.ndzela.admin.auth.application;

import com.ndzela.admin.auth.api.dto.MfaSetupResponse;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.config.AppProperties;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private static final String REPLAY_PREFIX = "auth:totp-used:";
    private static final Duration REPLAY_TTL = Duration.ofSeconds(120);

    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;
    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final AppProperties props;

    @Transactional
    public MfaSetupResponse beginSetup(User user) {
        if (user.isMfaEnabled()) {
            throw new BusinessRuleViolationException("MFA is already configured for this user.");
        }
        String secret = secretGenerator.generate();
        user.setMfaSecret(secret);
        userRepository.save(user);

        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(props.mfa().issuer())
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            String png = Base64.getEncoder().encodeToString(qrGenerator.generate(data));
            return new MfaSetupResponse(secret, "data:image/png;base64," + png);
        } catch (QrGenerationException e) {
            throw new IllegalStateException("Failed to generate MFA setup QR code", e);
        }
    }

    /**
     * Verifies a login code. For a user still enrolling, a valid code also confirms the enrolment
     * and activates the account.
     */
    @Transactional
    public boolean verifyLoginCode(User user, String code) {
        if (user.getMfaSecret() == null) {
            throw new BusinessRuleViolationException("MFA setup has not been started.");
        }
        if (!codeVerifier.isValidCode(user.getMfaSecret(), code) || isReplay(user, code)) {
            return false;
        }
        if (!user.isMfaEnabled()) {
            user.setMfaEnabled(true);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
        return true;
    }

    private boolean isReplay(User user, String code) {
        Boolean firstUse = redis.opsForValue().setIfAbsent(REPLAY_PREFIX + user.getId() + ":" + code, "1", REPLAY_TTL);
        return !Boolean.TRUE.equals(firstUse);
    }
}
