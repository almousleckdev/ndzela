package com.ndzela.admin.audit;

import com.ndzela.admin.audit.application.AuditActors;
import com.ndzela.admin.audit.domain.AuditLog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditActorsTest {
    private static final String RAW = "DriverPrincipal[driverId=0421ea7b-c938-45aa-8267-ecd66f468126, countryId=70e42d12-d90c-439e-9b4b-5b8538c3c5e5, status=ACTIVE, mustChangePassword=false]";

    private static AuditLog row(String actor, String targetType, String target) {
        return AuditLog.builder().actor(actor).targetType(targetType).target(target).build();
    }

    @Test
    void aRawDriverPrincipalIsShownAsTheDriverTheRowIsAbout() {
        assertEquals("Chauffeur · Nze Awa", AuditActors.display(row(RAW, "DRIVER", "Nze Awa")));
    }

    @Test
    void aRawDriverPrincipalWithoutADriverTargetIsJustADriver() {
        assertEquals("Chauffeur", AuditActors.display(row(RAW, "PAYMENT", "REF")));
    }

    @Test
    void aStaffMemberIsShownAsTheyAre() {
        assertEquals("admin@ndzela.com", AuditActors.display(row("admin@ndzela.com", "DRIVER", "Nze Awa")));
    }
}
