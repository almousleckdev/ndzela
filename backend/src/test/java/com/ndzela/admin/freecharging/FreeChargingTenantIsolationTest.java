package com.ndzela.admin.freecharging;

import com.ndzela.admin.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// An agreement belongs to one country; the staff of another country can neither see nor change it.
class FreeChargingTenantIsolationTest extends AbstractFreeChargingTest {
    private UUID maliAgreement() {
        return inMali(() -> agreements.create(maliDriver, LocalDate.of(2026, 12, 1), null, null)).getId();
    }

    @Test
    void anotherCountrysStaffCannotReadOrChangeIt() {
        UUID id = maliAgreement();

        assertTrue(inGabon(() -> agreements.list()).isEmpty());
        assertTrue(inGabon(() -> agreements.findActiveFor(maliDriver)).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> inGabon(() -> agreements.suspend(id)));
        assertThrows(ResourceNotFoundException.class, () -> inGabon(() -> agreements.revoke(id)));
        assertThrows(ResourceNotFoundException.class, () -> inGabon(() -> agreements.update(id, LocalDate.of(2026, 12, 1), null, null)));
        assertEquals(1, inMali(() -> agreements.list()).size());
    }

    @Test
    void aCountryCannotGrantItToADriverOfAnotherCountry() {
        assertThrows(ResourceNotFoundException.class, () -> inGabon(() -> agreements.create(maliDriver, LocalDate.of(2026, 12, 1), null, null)));
    }
}
