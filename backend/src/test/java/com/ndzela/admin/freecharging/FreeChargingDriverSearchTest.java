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
        return inGabon(() -> search.find(q, "ALL", 0, 20)).items().stream().map(DriverOption::driverId).toList();
    }

    private List<DriverOption> options(String q, String membership, int page, int size) {
        return inGabon(() -> search.find(q, membership, page, size)).items();
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
        assertEquals(List.of(jean, paul, awa), mine(options("", "ALL", 0, 20)));
        assertEquals(2, options(null, "ALL", 0, 2).size());
    }

    @Test
    void resignedAndPausedDriversAreListedWithTheirStatus() {
        jdbc.update("update drivers set status = 'ARCHIVED' where id = ?", paul);
        jdbc.update("update drivers set status = 'INACTIVE' where id = ?", awa);

        assertEquals("ARCHIVED", options("nze", "ALL", 0, 5).get(0).status().name());
        assertEquals("INACTIVE", options("awa", "ALL", 0, 5).get(0).status().name());
        assertEquals(List.of(jean), found("mba"));
    }

    private static final int BASE_DRIVERS = 2;

    private List<UUID> mine(List<DriverOption> options) {
        return options.stream().map(DriverOption::driverId).filter(id -> List.of(paul, awa, jean).contains(id)).toList();
    }

    @Test
    void everyDriverCanBeListedPageByPageWithTheTotal() {
        var first = inGabon(() -> search.find("", "ALL", 0, 2));
        var second = inGabon(() -> search.find("", "ALL", 1, 2));
        var third = inGabon(() -> search.find("", "ALL", 2, 2));

        assertEquals(3 + BASE_DRIVERS, first.total());
        assertEquals(2, first.items().size());
        assertEquals(2, second.items().size());
        assertEquals(1, third.items().size());
        assertEquals(2, third.page());
        assertEquals(List.of(jean, paul, awa), mine(java.util.stream.Stream.of(first, second, third).flatMap(page -> page.items().stream()).toList()));
    }

    @Test
    void theMembershipFilterKeepsOnlyDriversInOrOutOfTheProgram() {
        inGabon(() -> agreements.create(paul, LocalDate.of(2026, 1, 1), null, null));

        assertEquals(List.of(paul), options("", "IN_PROGRAM", 0, 20).stream().map(DriverOption::driverId).toList());
        assertEquals(List.of(jean, awa), mine(options("", "NOT_IN_PROGRAM", 0, 20)));
        assertFalse(options("", "NOT_IN_PROGRAM", 0, 20).stream().anyMatch(option -> option.driverId().equals(paul)));
        assertEquals(3 + BASE_DRIVERS, inGabon(() -> search.find("", "ALL", 0, 20)).total());
        assertEquals(1, inGabon(() -> search.find("", "IN_PROGRAM", 0, 20)).total());
        assertEquals(2 + BASE_DRIVERS, inGabon(() -> search.find("", "NOT_IN_PROGRAM", 0, 20)).total());
    }

    @Test
    void aRevokedAgreementPutsTheDriverBackOutOfTheProgram() {
        var agreement = inGabon(() -> agreements.create(paul, LocalDate.of(2026, 1, 1), null, null));
        inGabon(() -> agreements.revoke(agreement.getId()));

        assertTrue(options("", "IN_PROGRAM", 0, 20).isEmpty());
        assertFalse(options("nze", "ALL", 0, 5).get(0).inProgram());
    }

    @Test
    void theMembershipFilterWorksTogetherWithTheSearchText() {
        inGabon(() -> agreements.create(paul, LocalDate.of(2026, 1, 1), null, null));

        assertEquals(List.of(paul), options("V015", "IN_PROGRAM", 0, 20).stream().map(DriverOption::driverId).toList());
        assertTrue(options("V015", "NOT_IN_PROGRAM", 0, 20).isEmpty());
    }

    @Test
    void anOptionNamesItsAgreementSoStaffCanSuspendOrRemoveIt() {
        var agreement = inGabon(() -> agreements.create(paul, LocalDate.of(2026, 1, 1), null, null));
        inGabon(() -> agreements.suspend(agreement.getId()));

        DriverOption option = options("nze", "ALL", 0, 5).get(0);
        assertEquals(agreement.getId(), option.agreementId());
        assertEquals("SUSPENDED", option.agreementStatus());
        assertNull(options("awa", "ALL", 0, 5).get(0).agreementId());
    }

    @Test
    void anotherCountryNeverAppears() {
        assertTrue(inMali(() -> search.find("nze", "ALL", 0, 20)).items().isEmpty());
        assertTrue(inMali(() -> search.find("V015", "ALL", 0, 20)).items().isEmpty());
    }

    @Test
    void eachOptionShowsTheCarTheLinkAndWhetherTheDriverIsAlreadyInTheProgram() {
        inGabon(() -> agreements.create(paul, LocalDate.of(2026, 1, 1), null, null));

        DriverOption inProgram = options("nze", "ALL", 0, 5).get(0);
        DriverOption free = options("awa", "ALL", 0, 5).get(0);

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

        DriverOption option = options("lonely", "ALL", 0, 5).get(0);

        assertEquals(stranger, option.driverId());
        assertFalse(option.linked());
        assertNull(option.customerId());
    }
}
