package com.ndzela.admin.user;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A staff member gets a car and free charging: only the super administrator decides, and Zella is asked to grant it.
class StaffAccessTest extends AbstractDriverTest {
    private User member;

    @BeforeEach
    void member() {
        member = staff("amina.staff@ndzela.test", "GA", "VIEWER");
        member.setFirstName("Amina");
        member.setLastName("Obame");
        member.setPhoneNumber("+24177001122");
        users.save(member);
        zella.customer("Z-STAFF", "cust-staff", "amina.staff@ndzela.test", "+24177001122", "Amina", "Obame");
    }

    private String url() {
        return "/api/v1/users/" + member.getId() + "/staff-access";
    }

    private ResultActions save(User as, String body) throws Exception {
        return send(put(url()).contentType(MediaType.APPLICATION_JSON).content(body), as);
    }

    @Test
    void theSuperAdministratorGivesAFleetCarAndFreeCharging() throws Exception {
        String vehicle = newVehicle(manager);

        save(root, "{\"vehicleId\":\"" + vehicle + "\",\"freeCharging\":true}").andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true)).andExpect(jsonPath("$.freeCharging").value(true))
                .andExpect(jsonPath("$.vehicleId").value(vehicle));

        assertTrue(zella.calls().contains("PUT cust-staff"), zella.calls().toString());
        assertTrue(zella.calls().stream().anyMatch(c -> c.startsWith("GRANT cust-staff Personnel NDZela - Amina Obame")), zella.calls().toString());
        assertEquals(1, auditRows("STAFF_FREE_CHARGING_GRANTED"));
        send(get(url()), root).andExpect(status().isOk()).andExpect(jsonPath("$.freeCharging").value(true));
    }

    @Test
    void aCarThatIsNotInTheFleetIsKeptAsAPlate() throws Exception {
        save(root, "{\"plate\":\"gb 123 aa\",\"freeCharging\":true}").andExpect(status().isOk())
                .andExpect(jsonPath("$.plate").value("GB 123 AA"));

        assertTrue(zella.calls().stream().anyMatch(c -> c.contains("GB 123 AA")), zella.calls().toString());
    }

    @Test
    void turningFreeChargingOffAsksZellaToTakeItBack() throws Exception {
        save(root, "{\"freeCharging\":true}").andExpect(status().isOk());

        save(root, "{\"freeCharging\":false}").andExpect(status().isOk()).andExpect(jsonPath("$.freeCharging").value(false));

        assertTrue(zella.calls().contains("REVOKE cust-staff"), zella.calls().toString());
    }

    @Test
    void aFleetCarAndAPlateTogetherAreRefused() throws Exception {
        save(root, "{\"vehicleId\":\"" + newVehicle(manager) + "\",\"plate\":\"X\",\"freeCharging\":false}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("STAFF_CAR_INVALID"));
    }

    @Test
    void aUserWithoutAMatchingZellaAccountIsRefused() throws Exception {
        member.setPhoneNumber("+24100000000");
        users.save(member);

        save(root, "{\"freeCharging\":true}").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ZELLA_ACCOUNT_NOT_FOUND"));

        assertFalse(zella.calls().stream().anyMatch(c -> c.startsWith("GRANT")));
    }

    @Test
    void anAdministratorCannotGiveFreeCharging() throws Exception {
        save(admin, "{\"freeCharging\":true}").andExpect(status().isForbidden());
        send(get(url()), manager).andExpect(status().isForbidden());

        assertFalse(zella.calls().stream().anyMatch(c -> c.startsWith("GRANT")));
    }

    @Test
    void removingTheAccessTakesBackFreeChargingAndUnlinks() throws Exception {
        save(root, "{\"freeCharging\":true}").andExpect(status().isOk());

        send(delete(url()), root).andExpect(status().isNoContent());

        assertTrue(zella.calls().contains("REVOKE cust-staff"), zella.calls().toString());
        send(get(url()), root).andExpect(jsonPath("$.linked").value(false));
        assertEquals(1, auditRows("STAFF_ZELLA_ACCESS_REMOVED"));
    }

    @Test
    void zellaBeingDownLeavesNothingSaved() throws Exception {
        zella.goDown();

        save(root, "{\"freeCharging\":true}").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ZELLA_UNAVAILABLE"));

        send(get(url()), root).andExpect(jsonPath("$.linked").value(false));
    }
}
