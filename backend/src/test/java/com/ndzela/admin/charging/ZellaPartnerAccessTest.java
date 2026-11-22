package com.ndzela.admin.charging;

import com.ndzela.admin.charging.api.ZellaPartnerAccess;
import com.ndzela.admin.common.exception.InvalidCredentialsException;
import com.ndzela.admin.common.ratelimit.RateLimiter;
import com.ndzela.admin.country.application.CountryService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ZellaPartnerAccessTest {
    @Mock private CountryService countries;
    @Mock private RateLimiter limiter;

    private ZellaPartnerAccess withKey(String configured) {
        ZellaPartnerAccess access = new ZellaPartnerAccess(null, countries, limiter);
        ReflectionTestUtils.setField(access, "partnerKey", configured);
        return access;
    }

    @Test
    void anEmptyConfiguredKeyRefusesEveryCallEvenAnEmptyOne() {
        for (String configured : new String[] {"", "  ", null}) {
            ZellaPartnerAccess access = withKey(configured);
            for (String given : new String[] {null, "", "  ", "anything"}) {
                assertThrows(InvalidCredentialsException.class, () -> access.call("free-manage", given, () -> "reached"));
            }
        }
    }
}
