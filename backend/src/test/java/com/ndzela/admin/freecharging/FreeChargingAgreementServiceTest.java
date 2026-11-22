package com.ndzela.admin.freecharging;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.freecharging.domain.AgreementStatus;
import com.ndzela.admin.freecharging.domain.FreeChargingAgreement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Staff grant, change, pause and withdraw the free Sunday charge of a driver; every action is audited and the row is never deleted.
class FreeChargingAgreementServiceTest extends AbstractFreeChargingTest {
    private static final LocalDate START = LocalDate.of(2026, 12, 1);
    private static final LocalDate END = LocalDate.of(2027, 6, 1);

    private FreeChargingAgreement create(UUID driver) {
        return inGabon(() -> agreements.create(driver, START, END, "paid up"));
    }

    private String codeOf(Runnable action) {
        return assertThrows(BusinessRuleViolationException.class, action::run).getCode();
    }

    @Test
    void createsAnActiveAgreementForALinkedDriver() {
        FreeChargingAgreement created = create(linkedDriver);

        assertEquals(AgreementStatus.ACTIVE, created.getStatus());
        assertEquals(linkedDriver, created.getDriverId());
        assertEquals(START, created.getStartsOn());
        assertEquals("paid up", created.getConditions());
        assertEquals(created.getId(), inGabon(() -> agreements.findActiveFor(linkedDriver)).orElseThrow().getId());
        assertEquals(1, auditRows("FREE_AGREEMENT_CREATED"));
    }

    @Test
    void refusesASecondNonRevokedAgreementForTheSameDriver() {
        create(linkedDriver);

        assertEquals("FREE_AGREEMENT_EXISTS", codeOf(() -> create(linkedDriver)));
    }

    @Test
    void refusesADriverWithoutALinkedZellaAccount() {
        assertEquals("FREE_AGREEMENT_NOT_LINKED", codeOf(() -> create(unlinkedDriver)));
        assertEquals(0, auditRows("FREE_AGREEMENT_CREATED"));
    }

    @Test
    void refusesEndBeforeStart() {
        assertEquals("FREE_AGREEMENT_DATES_INVALID", codeOf(() -> inGabon(() -> agreements.create(linkedDriver, END, START, null))));
    }

    @Test
    void anOpenEndedAgreementIsAccepted() {
        FreeChargingAgreement created = inGabon(() -> agreements.create(linkedDriver, START, null, null));

        assertEquals(null, created.getEndsOn());
    }

    @Test
    void updateChangesDatesAndConditionsAndIsAudited() {
        UUID id = create(linkedDriver).getId();

        FreeChargingAgreement updated = inGabon(() -> agreements.update(id, START.plusDays(7), null, "new terms"));

        assertEquals(START.plusDays(7), updated.getStartsOn());
        assertEquals("new terms", updated.getConditions());
        assertEquals(1, auditRows("FREE_AGREEMENT_UPDATED"));
        assertEquals("FREE_AGREEMENT_DATES_INVALID", codeOf(() -> inGabon(() -> agreements.update(id, END, START, null))));
    }

    @Test
    void suspendAndResumeSwitchTheStatusAndAreAudited() {
        UUID id = create(linkedDriver).getId();

        assertEquals(AgreementStatus.SUSPENDED, inGabon(() -> agreements.suspend(id)).getStatus());
        assertTrue(inGabon(() -> agreements.findActiveFor(linkedDriver)).isEmpty());
        assertEquals(AgreementStatus.ACTIVE, inGabon(() -> agreements.resume(id)).getStatus());
        assertEquals(1, auditRows("FREE_AGREEMENT_SUSPENDED"));
        assertEquals(1, auditRows("FREE_AGREEMENT_RESUMED"));
    }

    @Test
    void aRevokedAgreementCannotBeResumedOrChanged() {
        UUID id = create(linkedDriver).getId();
        inGabon(() -> agreements.revoke(id));

        assertEquals("FREE_AGREEMENT_REVOKED", codeOf(() -> inGabon(() -> agreements.resume(id))));
        assertEquals("FREE_AGREEMENT_REVOKED", codeOf(() -> inGabon(() -> agreements.update(id, START, END, null))));
        assertEquals("FREE_AGREEMENT_REVOKED", codeOf(() -> inGabon(() -> agreements.suspend(id))));
    }

    @Test
    void revokeKeepsTheRowAndAllowsANewAgreement() {
        UUID first = create(linkedDriver).getId();

        assertEquals(AgreementStatus.REVOKED, inGabon(() -> agreements.revoke(first)).getStatus());
        UUID second = create(linkedDriver).getId();

        assertEquals(2, rows("select count(*) from free_charging_agreements where driver_id = ?", linkedDriver));
        assertEquals(second, inGabon(() -> agreements.findActiveFor(linkedDriver)).orElseThrow().getId());
        assertEquals(1, auditRows("FREE_AGREEMENT_REVOKED"));
        assertEquals(2, inGabon(() -> agreements.list()).size());
    }
}
