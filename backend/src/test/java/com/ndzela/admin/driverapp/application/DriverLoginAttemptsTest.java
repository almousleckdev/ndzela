package com.ndzela.admin.driverapp.application;

import com.ndzela.admin.TestProperties;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.AccountLockedException;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverLoginAttemptsTest {
    @Mock
    private DriverRepository drivers;
    @Mock
    private AuditService audit;

    private DriverLoginAttempts attempts;
    private Driver driver;
    private UUID driverId;
    private UUID countryId;

    @BeforeEach
    void setUp() {
        attempts = new DriverLoginAttempts(drivers, TestProperties.create(), audit);
        driverId = UUID.randomUUID();
        countryId = UUID.randomUUID();
        driver = Driver.builder().id(driverId).failedLoginAttempts(0).build();
        TenantHolder.set(TenantContext.country(countryId));
    }

    @AfterEach
    void tearDown() {
        TenantHolder.clear();
    }

    @Test
    void reachingThresholdLocksAndRecordsAnAuditEntry() {
        driver.setFailedLoginAttempts(4);
        when(drivers.findForUpdate(driverId)).thenReturn(Optional.of(driver));

        attempts.failed(driverId);

        assertTrue(driver.getLoginLockedUntil().isAfter(OffsetDateTime.now()));
        verify(audit).record(eq("DRIVER_LOGIN_LOCKED"), eq("DRIVER"), eq(driverId.toString()), eq(driverId.toString()), eq(countryId), anyString());
    }

    @Test
    void failuresBelowThresholdRecordNoAuditEntry() {
        when(drivers.findForUpdate(driverId)).thenReturn(Optional.of(driver));

        attempts.failed(driverId);

        verifyNoInteractions(audit);
    }

    @Test
    void activeLockBlocksAuthentication() {
        driver.setLoginLockedUntil(OffsetDateTime.now().plusMinutes(10));
        assertThrows(AccountLockedException.class, () -> attempts.assertNotLocked(driver));
    }
}
