package com.ndzela.admin.freecharging;

import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.freecharging.application.DriverOption;
import com.ndzela.admin.freecharging.application.FreeChargingDriverSearch;
import com.ndzela.admin.vehicle.VehicleApiTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Staff find a driver to add by name, e-mail, phone or the number or plate of the car they hold.
class FreeChargingDriverSearchTest extends AbstractFreeChargingTest {
    @Autowired private FreeChargingDriverSearch search;

    private UUID paul;
    private UUID awa;
    private UUID jean;

    private UUID driver(String last, String first, String phone, String email, String fleet, String plate) throws Exception {
        String vehicle = id(postJson("/api/v1/vehicles", VehicleApiTest.json(fleet, plate), manager).andExpect(status().isCreated()));
        DriverJson body = DriverJson.create(vehicle, newCenter(manager, "C-" + fleet)).personal("lastName", last).personal("firstName", first)
                .personal("phonePrimary", phone).personal("email", email);
        UUID created = UUID.fromString(id(createDriver(manager, body).andExpect(status().isCreated())));
        link(created);
        return created;
    }

    @BeforeEach
    void people() throws Exception {
        paul = driver("NZE", "Paul", "+24177001122", "paul.nze@x.test", "V015", "PL-0150");
        awa = driver("OBIANG", "Awa", "+24166002233", "awa@x.test", "V030", "PL-0300");
        jean = driver("MBA", "Jean", "+24162003344", "jean@x.test", "V041", "PL-0410");
    }

    private List<UUID> found(String q) {
        return inGabon(() -> search.find(q, 20)).stream().map(DriverOption::driverId).toList();
    }

    @Test
    void findsByNameInAnyOrderAndCase() {
        assertEquals(List.of(paul), found("nze"));
        assertEquals(List.of(paul), found("PAUL"));
        assertEquals(List.of(paul), found("paul nze"));
        assertEquals(List.of(paul), found("nze paul"));
    }

    @Test
    void findsByEmail() {
        assertEquals(List.of(awa), found("awa@x"));
        assertEquals(List.of(awa), found("AWA@X.TEST"));
    }

    @Test
    void findsByPhoneWhateverTheSpacingAndPrefix() {
        assertEquals(List.of(paul), found("77001122"));
        assertEquals(List.of(paul), found("077 00 11 22"));
        assertEquals(List.of(paul), found("+241 77 00 11 22"));
        assertEquals(List.of(paul), found("241-77-00-11-22"));
        assertEquals(List.of(awa), found("6600"));
    }

    @Test
    void findsByTheFleetNumberOfTheCarTheyHold() {
        assertEquals(List.of(paul), found("V015"));
        assertEquals(List.of(paul), found("v015"));
        assertEquals(List.of(awa), found("V03"));
        assertEquals(List.of(jean), found("V041"));
    }

    @Test
    void findsByThePlateOfTheCarTheyHold() {
        assertEquals(List.of(jean), found("pl-0410"));
        assertEquals(List.of(awa), found("PL-03"));
    }

    @Test
    void aCarTheyNoLongerHoldDoesNotFindThem() throws Exception {
        postJson("/api/v1/drivers/" + paul + "/vehicle/remove", REASON, manager).andExpect(status().isOk());

        assertTrue(found("V015").isEmpty());
        assertEquals(List.of(paul), found("nze"));
    }

    @Test
    void anEmptySearchListsDriversByNameAndRespectsTheLimit() {
        assertEquals(3, inGabon(() -> search.find("", 20)).stream().filter(option -> List.of(paul, awa, jean).contains(option.driverId())).count());
        assertEquals(2, inGabon(() -> search.find(null, 2)).size());
    }

    @Test
    void resignedAndPausedDriversAreNotOffered() {
        jdbc.update("update drivers set status = 'ARCHIVED' where id = ?", paul);
        jdbc.update("update drivers set status = 'INACTIVE' where id = ?", awa);

        assertTrue(found("nze").isEmpty());
        assertTrue(found("awa").isEmpty());
        assertEquals(List.of(jean), found("mba"));
    }

    @Test
    void anotherCountryNeverAppears() {
        assertTrue(inMali(() -> search.find("nze", 20)).isEmpty());
        assertTrue(inMali(() -> search.find("V015", 20)).isEmpty());
    }

    @Test
    void eachOptionShowsTheCarTheLinkAndWhetherTheDriverIsAlreadyInTheProgram() {
        inGabon(() -> agreements.create(paul, LocalDate.of(2026, 1, 1), null, null));

        DriverOption inProgram = inGabon(() -> search.find("nze", 5)).get(0);
        DriverOption free = inGabon(() -> search.find("awa", 5)).get(0);

        assertEquals("V015", inProgram.fleetNumber());
        assertEquals("PL-0150", inProgram.plate());
        assertEquals("cust-" + paul, inProgram.customerId());
        assertTrue(inProgram.linked());
        assertTrue(inProgram.inProgram());
        assertFalse(free.inProgram());
        assertEquals("+24166002233", free.phone());
        assertEquals("awa@x.test", free.email());
    }

    @Test
    void aDriverWithoutAZellaLinkIsListedButNotLinked() throws Exception {
        UUID stranger = UUID.fromString(id(createDriver(manager, DriverJson.create(id(postJson("/api/v1/vehicles", VehicleApiTest.json("V099", "PL-0990"), manager)
                .andExpect(status().isCreated())), newCenter(manager, "C-99")).personal("lastName", "LONELY")).andExpect(status().isCreated())));

        DriverOption option = inGabon(() -> search.find("lonely", 5)).get(0);

        assertEquals(stranger, option.driverId());
        assertFalse(option.linked());
        assertNull(option.customerId());
    }
}
