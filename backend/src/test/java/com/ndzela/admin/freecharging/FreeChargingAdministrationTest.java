package com.ndzela.admin.freecharging;

import com.ndzela.admin.charging.application.IdTagLinkService;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.DriverFinder;
import com.ndzela.admin.freecharging.application.AgreementView;
import com.ndzela.admin.freecharging.application.FreeChargingAdministration;
import com.ndzela.admin.freecharging.application.FreeChargingAgreementService;
import com.ndzela.admin.freecharging.application.FreeChargingDriverSearch;
import com.ndzela.admin.freecharging.application.FreeChargingEligibility;
import com.ndzela.admin.freecharging.application.FreeChargingWindowService;
import com.ndzela.admin.freecharging.application.FreeChargingUsageSource;
import com.ndzela.admin.freecharging.domain.FreeChargingAgreement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreeChargingAdministrationTest {
    @Mock private FreeChargingAgreementService agreements;
    @Mock private FreeChargingEligibility eligibility;
    @Mock private FreeChargingUsageSource usage;
    @Mock private DriverArrearsService drivers;
    @Mock private IdTagLinkService links;
    @Mock private FreeChargingWindowService windows;
    @Mock private FreeChargingDriverSearch search;
    @Mock private DriverFinder finder;

    @Test
    void anAgreementWhoseDriverCannotBeFoundIsLeftOutOfTheList() {
        FreeChargingAgreement orphan = FreeChargingAgreement.builder().id(UUID.randomUUID()).driverId(UUID.randomUUID()).build();
        when(agreements.list()).thenReturn(List.of(orphan));
        when(drivers.refs(any())).thenReturn(Map.of());
        when(usage.lastUseOf(any())).thenReturn(Map.of());

        List<AgreementView> rows = new FreeChargingAdministration(agreements, eligibility, usage, drivers, links, windows, search, finder).list();

        assertTrue(rows.isEmpty());
    }
}
