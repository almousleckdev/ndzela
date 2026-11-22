package com.ndzela.admin.charging;

import com.ndzela.admin.TestProperties;
import com.ndzela.admin.charging.api.ZellaPartnerAccess;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.ratelimit.RateLimiter;
import com.ndzela.admin.country.api.dto.CountryResponse;
import com.ndzela.admin.country.application.CountryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// The free-charging read is limited per Zella customer, under a high ceiling shared by all customers, so the display traffic of some customers can never starve the free start of another.
class ZellaPartnerReadLimitTest {
    private static final String KEY = "partner-key";
    private static final int PER_CUSTOMER = 30;
    private static final int CEILING = 2000;

    private final CountingLimiter limiter = new CountingLimiter();
    private final CountryService countries = mock(CountryService.class);
    private final ZellaPartnerAccess access = new ZellaPartnerAccess(TestProperties.create(), countries, limiter);

    // Counts in memory exactly as the Redis limiter does in its window
    private static final class CountingLimiter extends RateLimiter {
        private final Map<String, Integer> counts = new HashMap<>();

        CountingLimiter() {
            super(null);
        }

        @Override
        public boolean tryAcquire(String kind, String subject, int max, Duration window) {
            return counts.merge(kind + ":" + subject, 1, Integer::sum) <= max;
        }
    }

    @BeforeEach
    void gabon() {
        ReflectionTestUtils.setField(access, "partnerKey", KEY);
        when(countries.list()).thenReturn(List.of(new CountryResponse(UUID.randomUUID(), "GA", "Gabon", "XAF", "+241", "Africa/Libreville", "fr")));
    }

    private String read(String customer) {
        return access.callForCustomer("free-read", customer, KEY, () -> "answer");
    }

    @Test
    void aCustomerOverItsBucketDoesNotStarveAnotherCustomer() {
        for (int i = 0; i < PER_CUSTOMER; i++) {
            assertEquals("answer", read("customer-a"));
        }
        assertThrows(BusinessRuleViolationException.class, () -> read("customer-a"));
        assertEquals("answer", read("customer-b"));
    }

    @Test
    void theCeilingStillBoundsAFloodSpreadOverManyCustomers() {
        for (int i = 0; i < CEILING; i++) {
            assertEquals("answer", read("customer-" + i));
        }
        assertThrows(BusinessRuleViolationException.class, () -> read("one-more-customer"));
    }
}
