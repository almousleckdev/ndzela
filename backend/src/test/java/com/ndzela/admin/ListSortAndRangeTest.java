package com.ndzela.admin;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Every table list accepts a whitelisted sort and a day range.
class ListSortAndRangeTest extends AbstractDriverTest {
    private final LocalDate today = LocalDate.now(ZoneId.of("Africa/Libreville"));
    private String center;

    @BeforeEach
    void setup() throws Exception {
        center = newCenter(manager, "NDZELA");
    }

    private String driverNamed(String lastName, String firstName) throws Exception {
        return id(createDriver(manager, DriverJson.create(newVehicle(manager), center).personal("lastName", lastName).personal("firstName", firstName)).andExpect(status().isCreated()));
    }

    @Test
    void vehiclesSortByAnAllowedFieldAndFilterByCreationDay() throws Exception {
        newVehicle(manager);
        newVehicle(manager);
        send(get("/api/v1/vehicles?sort=fleetNumber,desc"), viewer).andExpect(status().isOk());
        send(get("/api/v1/vehicles?sort=dailyRent,desc&size=1"), viewer).andExpect(jsonPath("$.items.length()").value(1));
        send(get("/api/v1/vehicles?sort=vin,asc"), viewer).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        send(get("/api/v1/vehicles?sort=plate,sideways"), viewer).andExpect(status().isBadRequest());
        send(get("/api/v1/vehicles?from=" + today + "&to=" + today), viewer).andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/vehicles?to=" + today.minusDays(1)), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/vehicles?from=" + today.plusDays(1)), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/vehicles/export?from=" + today.plusDays(1)), manager).andExpect(status().isOk());
    }

    @Test
    void driversSortByNameAndFilterByRegistrationDay() throws Exception {
        driverNamed("ZAMBO", "Paul");
        driverNamed("ABOGHE", "Luc");
        send(get("/api/v1/drivers?sort=lastName,asc"), viewer).andExpect(jsonPath("$.items[0].personal.lastName").value("ABOGHE"));
        send(get("/api/v1/drivers?sort=lastName,desc"), viewer).andExpect(jsonPath("$.items[0].personal.lastName").value("ZAMBO"));
        send(get("/api/v1/drivers?sort=idNumber,asc"), viewer).andExpect(status().isBadRequest());
        send(get("/api/v1/drivers?from=" + today + "&to=" + today), viewer).andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/drivers?to=" + today.minusDays(1)), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/drivers/export?to=" + today.minusDays(1)), manager).andExpect(status().isOk());
    }

    @Test
    void theFollowUpTableSortsFiltersByLateDayAndSumsWhatWasCollected() throws Exception {
        String first = driverNamed("ZAMBO", "Paul");
        driverNamed("ABOGHE", "Luc");
        postJson("/api/v1/payments", "{\"driverId\":\"" + first + "\",\"amount\":23000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"R1\",\"requestId\":\"" + UUID.randomUUID() + "\"}", manager).andExpect(status().isCreated());

        send(get("/api/v1/payments/accounts?sort=driverName,asc"), viewer).andExpect(jsonPath("$.items[0].driverName").value("ABOGHE Luc"));
        send(get("/api/v1/payments/accounts?sort=driverName,desc"), viewer).andExpect(jsonPath("$.items[0].driverName").value("ZAMBO Paul"));
        send(get("/api/v1/payments/accounts?sort=contractNumber,asc"), viewer).andExpect(status().isBadRequest());
        send(get("/api/v1/payments/accounts"), viewer).andExpect(jsonPath("$.items[0].collected").doesNotExist());
        send(get("/api/v1/payments/accounts?paidFrom=" + today + "&paidTo=" + today + "&sort=driverName,desc"), viewer).andExpect(jsonPath("$.items[0].collected").value(23000)).andExpect(jsonPath("$.items[1].collected").value(0));
        send(get("/api/v1/payments/accounts?paidTo=" + today.minusDays(1)), viewer).andExpect(jsonPath("$.items[0].collected").value(0));
        send(get("/api/v1/payments/accounts?filter=LATE&lateFrom=" + today), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/summary?from=" + today + "&to=" + today), viewer).andExpect(jsonPath("$.collectedRange").value(23000));
        send(get("/api/v1/payments/summary?to=" + today.minusDays(1)), viewer).andExpect(jsonPath("$.collectedRange").value(0));
        send(get("/api/v1/payments/summary"), viewer).andExpect(jsonPath("$.collectedRange").doesNotExist());
    }

    @Test
    void theLedgerHistorySortsByAmount() throws Exception {
        String driver = driverNamed("ZAMBO", "Paul");
        for (int amount : new int[]{1000, 9000, 4000}) {
            postJson("/api/v1/payments", "{\"driverId\":\"" + driver + "\",\"amount\":" + amount + ",\"method\":\"AIRTEL_MONEY\",\"reference\":\"R" + amount + "\",\"requestId\":\"" + UUID.randomUUID() + "\"}", manager).andExpect(status().isCreated());
        }
        send(get("/api/v1/payments/history?sort=amount,desc"), viewer).andExpect(jsonPath("$.items[0].amount").value(9000)).andExpect(jsonPath("$.items[2].amount").value(1000));
        send(get("/api/v1/payments/history?sort=amount,asc"), viewer).andExpect(jsonPath("$.items[0].amount").value(1000));
        send(get("/api/v1/payments/history?sort=recordedBy,asc"), viewer).andExpect(status().isBadRequest());
    }

    @Test
    void theAuditTrailSorts() throws Exception {
        newVehicle(manager);
        send(get("/api/v1/audit?sort=actor,asc"), admin).andExpect(status().isOk());
        send(get("/api/v1/audit?sort=action,desc"), admin).andExpect(status().isOk());
        send(get("/api/v1/audit?sort=details,asc"), admin).andExpect(status().isBadRequest());
    }

    @Test
    void historyAndChargingSearchByDriverReferencePlateAndLabel() throws Exception {
        String driver = driverNamed("ZAMBO", "Paul");
        postJson("/api/v1/payments", "{\"driverId\":\"" + driver + "\",\"amount\":5000,\"method\":\"MOOV_MONEY\",\"reference\":\"REF-77\",\"requestId\":\"" + UUID.randomUUID() + "\"}", manager);
        send(get("/api/v1/payments/history?q=zambo"), viewer).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/history?q=ref-77"), viewer).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/history?q=nobody"), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/export?q=zambo"), accountantOf()).andExpect(status().isOk());

        String station = id(postJson("/api/v1/charging/stations", "{\"name\":\"Akanda\"}", manager).andExpect(status().isCreated()));
        String vehicle = jdbc.queryForObject("select vehicle_id::text from vehicle_assignments", String.class);
        postJson("/api/v1/charging/recharges", "{\"vehicleId\":\"" + vehicle + "\",\"stationId\":\"" + station + "\",\"amount\":3000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"R1\",\"batteryBefore\":20,\"batteryAfter\":80,\"requestId\":\"" + UUID.randomUUID() + "\"}", manager);
        postJson("/api/v1/charging/recharges", "{\"publicLabel\":\"Toyota blanche\",\"stationId\":\"" + station + "\",\"amount\":2000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"R1\",\"batteryBefore\":20,\"batteryAfter\":80,\"requestId\":\"" + UUID.randomUUID() + "\"}", manager);
        send(get("/api/v1/charging/recharges?q=toyota"), viewer).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/charging/recharges?q=zambo"), viewer).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].amount").value(3000));
        send(get("/api/v1/charging/recharges?q=" + jdbc.queryForObject("select plate from vehicles", String.class).toLowerCase()), viewer).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/charging/recharges?q=nothing"), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/charging/recharges"), viewer).andExpect(jsonPath("$.total").value(2));
    }

    private com.ndzela.admin.user.domain.User accountantOf() {
        return staff("ga.accountant2@ndzela.test", "GA", "COMPTABLE");
    }
}
