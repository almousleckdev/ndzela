package com.ndzela.admin.driverapp;

import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.infrastructure.pvit.PvitStatus;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A driver's payment receipt: its content and who may read it
class DriverReceiptDownloadTest extends AbstractPvitFlowTest {
    private String pdfText(byte[] pdf) throws Exception {
        var reader = new PdfReader(pdf);
        return new PdfTextExtractor(reader).getTextFromPage(1).replace('\u00a0', ' ');
    }

    @Test
    void aConfirmedPaymentHasAReceiptWithAllItsDetails() throws Exception {
        String id = startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), "3.75");
        callback(reference(), "PAY0110261325109", 200).andExpect(status().isOk());

        var result = asDriver("/api/v1/driver/payments/" + id + "/receipt", token).andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("recu-" + reference() + ".pdf")))
                .andReturn();
        byte[] pdf = result.getResponse().getContentAsByteArray();
        Files.write(Path.of("target/payment-receipt-sample.pdf"), pdf);
        String text = pdfText(pdf);

        assertEquals("%PDF", new String(pdf, 0, 4));
        for (String expected : new String[] {"REÇU DE PAIEMENT", "N° " + reference(), "PAY1", "Airtel Money", "07*****11", "Contrat", "NDZ-", "PAYÉ", "TOTAL PAYÉ", "Frais opérateur", "4 FCFA",
                "Chauffeur", "Loyer du", String.valueOf(body.personalValue("firstName")), String.valueOf(body.personalValue("lastName"))}) {
            assertTrue(text.contains(expected), "the receipt should contain: " + expected + "\n" + text);
        }
        assertFalse(text.contains("RCCM"));
        assertFalse(text.contains("074111111"));
    }

    @Test
    void aReceiptExistsOnlyForAConfirmedPayment() throws Exception {
        String id = startOk();

        asDriver("/api/v1/driver/payments/" + id + "/receipt", token).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("RECEIPT_NOT_AVAILABLE"));

        pvitSays(PvitStatus.FAILED, amount(), null);
        jdbc.update("update payment_intents set created_at = now() - interval '10 minutes'");
        settlement.recheckPending();
        asDriver("/api/v1/driver/payments/" + id + "/receipt", token).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aReceiptIsOnlyForItsOwnerNeverAnotherDriverNorAnotherCountryNorStaff() throws Exception {
        String id = startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), null);
        callback(reference(), "PAY1", 200).andExpect(status().isOk());
        DriverJson other = DriverJson.create(newVehicle(manager), newCenter(manager, "AUTRE"));
        createDriver(manager, other).andExpect(status().isCreated());
        DriverJson mali = DriverJson.create(newVehicle(maliManager), newCenter(maliManager, "NDZELA ML"));
        createDriver(maliManager, mali).andExpect(status().isCreated());
        jdbc.update("update drivers set must_change_password = false");

        for (DriverJson intruder : new DriverJson[] {other, mali}) {
            String intruderToken = JsonPath.read(login(String.valueOf(intruder.personalValue("email")), DRIVER_PASSWORD).andReturn().getResponse().getContentAsString(), "$.accessToken");
            asDriver("/api/v1/driver/payments/" + id + "/receipt", intruderToken).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/v1/driver/payments/" + id + "/receipt").header("Authorization", bearer(manager))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/driver/payments/" + id + "/receipt")).andExpect(status().isUnauthorized());
    }
}
