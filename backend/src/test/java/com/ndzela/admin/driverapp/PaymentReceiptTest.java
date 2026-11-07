package com.ndzela.admin.driverapp;

import com.ndzela.admin.driver.DriverJson;
import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The ticket receipt of a confirmed payment
class PaymentReceiptTest extends AbstractDriverPayingTest {
    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    private String intentId;
    private String oldestUnpaid;

    private void payOneOfTwoDays() throws Exception {
        withUnpaidDays(2);
        oldestUnpaid = LocalDate.parse(oldestUnpaidDay()).format(LONG_DAY);
        intentId = JsonPath.read(start("1").andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString(), "$.id");
        pvitConfirms(RENT);
    }

    private String oldestUnpaidDay() throws Exception {
        String body = send(get("/api/v1/payments/accounts/" + jdbc.queryForObject("select id from drivers", String.class)), manager).andReturn().getResponse().getContentAsString();
        java.util.List<String> days = JsonPath.read(body, "$.days[?(@.status=='UNPAID')].day");
        return days.stream().sorted().findFirst().orElseThrow();
    }

    private byte[] receipt() throws Exception {
        return asDriver("/api/v1/driver/payments/" + intentId + "/receipt", token).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
    }

    private static String text(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        return new PdfTextExtractor(reader).getTextFromPage(1);
    }

    @Test
    void theReceiptNeverShowsTheCompanyRegistryNumber() throws Exception {
        String original = jdbc.queryForObject("select registry_number from countries where code = 'GA'", String.class);
        jdbc.update("update countries set registry_number = 'RC/LBV/2020/B/0042' where code = 'GA'");
        try {
            payOneOfTwoDays();

            String text = text(receipt());

            assertFalse(text.contains("RCCM"));
            assertFalse(text.contains("RC/LBV/2020/B/0042"));
        } finally {
            jdbc.update("update countries set registry_number = ? where code = 'GA'", original);
        }
    }

    @Test
    void theReceiptShowsTheFleetNumberOfTheDriversCar() throws Exception {
        payOneOfTwoDays();
        String fleetNumber = jdbc.queryForObject("select fleet_number from vehicles limit 1", String.class);

        String text = text(receipt());

        assertTrue(text.contains("Véhicule"), text);
        assertTrue(text.contains(fleetNumber), text);
    }

    @Test
    void theReceiptNamesTheDayPaidAndWhenItWasPaid() throws Exception {
        payOneOfTwoDays();

        String text = text(receipt());

        assertTrue(text.contains("Loyer du " + oldestUnpaid), text);
        assertTrue(text.matches("(?s).*payé le \\p{L}+ \\d{1,2} \\p{L}+ \\d{4} à \\d{2}:\\d{2}.*"), text);
    }

    @Test
    void thePhoneNumberIsMasked() throws Exception {
        payOneOfTwoDays();

        String text = text(receipt());

        assertFalse(text.contains("074111111"));
        assertTrue(text.contains("07*****11"), text);
    }

    @Test
    void theReceiptIsATicketNotAFullPage() throws Exception {
        payOneOfTwoDays();

        PdfReader reader = new PdfReader(receipt());

        assertEquals(226.8f, reader.getPageSize(1).getWidth(), 1.0f);
        assertTrue(reader.getPageSize(1).getHeight() < 640f);
        assertEquals(1, reader.getNumberOfPages());
    }

    @Test
    void theWholeTicketFitsItsPage() throws Exception {
        payOneOfTwoDays();

        String text = text(receipt());

        assertTrue(text.contains("Conservez-le avec sa référence"), text);
        assertTrue(text.contains(intentReference()), text);
    }

    private String intentReference() {
        return jdbc.queryForObject("select reference from payment_intents", String.class);
    }

    @Test
    void theResponseIsNeverCached() throws Exception {
        payOneOfTwoDays();

        asDriver("/api/v1/driver/payments/" + intentId + "/receipt", token).andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    void aDriverCannotReadAnotherDriversReceiptNorAnotherCountrys() throws Exception {
        payOneOfTwoDays();
        DriverJson other = DriverJson.create(newVehicle(manager), newCenter(manager, "AUTRE"));
        createDriver(manager, other).andExpect(status().isCreated());
        DriverJson mali = DriverJson.create(newVehicle(maliManager), newCenter(maliManager, "NDZELA ML"));
        createDriver(maliManager, mali).andExpect(status().isCreated());
        jdbc.update("update drivers set must_change_password = false");

        for (DriverJson intruder : new DriverJson[] {other, mali}) {
            String intruderToken = JsonPath.read(login(String.valueOf(intruder.personalValue("email")), DRIVER_PASSWORD).andReturn().getResponse().getContentAsString(), "$.accessToken");
            asDriver("/api/v1/driver/payments/" + intentId + "/receipt", intruderToken).andExpect(status().isNotFound());
        }
    }

    @Test
    void staffCanDownloadTheReceiptOfAnyPaymentButNotOfAnotherCountry() throws Exception {
        payOneOfTwoDays();
        String driverId = jdbc.queryForObject("select id from drivers where email = '" + email + "'", String.class);
        String body = "{\"driverId\":\"" + driverId + "\",\"amount\":23000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"CASH-" + UUID.randomUUID() + "\",\"requestId\":\"" + UUID.randomUUID() + "\"}";
        String entry = JsonPath.read(postJson("/api/v1/payments", body, manager).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.entry.id");

        byte[] pdf = send(get("/api/v1/payments/" + entry + "/receipt"), manager).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertTrue(text(pdf).contains("REÇU DE PAIEMENT"));
        send(get("/api/v1/payments/" + entry + "/receipt"), maliManager).andExpect(status().isNotFound());
    }

    @Test
    void staffCanAlsoDownloadTheReceiptOfAnAppPaymentByItsAttemptId() throws Exception {
        payOneOfTwoDays();

        byte[] pdf = send(get("/api/v1/payments/" + intentId + "/receipt"), manager).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        assertTrue(text(pdf).contains("Loyer du " + oldestUnpaid));
    }

    @Test
    void theDaysOnAReceiptDoNotChangeWhenAnotherPaymentIsReversed() throws Exception {
        payOneOfTwoDays();
        String before = daysLines(text(receipt()));
        String driverId = jdbc.queryForObject("select id from drivers where email = '" + email + "'", String.class);
        String body = "{\"driverId\":\"" + driverId + "\",\"amount\":23000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"CASH-" + UUID.randomUUID() + "\",\"requestId\":\"" + UUID.randomUUID() + "\"}";
        String entry = JsonPath.read(postJson("/api/v1/payments", body, manager).andReturn().getResponse().getContentAsString(), "$.entry.id");

        postJson("/api/v1/payments/" + entry + "/reverse", "{\"reason\":\"wrong driver\"}", admin).andExpect(status().isOk());

        assertEquals(before, daysLines(text(receipt())));
    }

    @Test
    void aReversedPaymentsReceiptSaysItIsCancelled() throws Exception {
        payOneOfTwoDays();
        String entry = jdbc.queryForObject("select id from ledger_entries where entry_type = 'PAYMENT'", String.class);

        postJson("/api/v1/payments/" + entry + "/reverse", "{\"reason\":\"duplicate\"}", admin).andExpect(status().isOk());

        assertTrue(text(receipt()).contains("ANNULÉ"));
    }

    private static String daysLines(String text) {
        return text.lines().filter(line -> line.contains("Loyer du") || line.contains("payé le")).reduce("", (a, b) -> a + b + "\n");
    }
}
