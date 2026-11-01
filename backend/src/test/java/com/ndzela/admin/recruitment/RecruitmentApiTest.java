package com.ndzela.admin.recruitment;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecruitmentApiTest extends AbstractIntegrationTest {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] PDF = "%PDF-1.4 test".getBytes(StandardCharsets.UTF_8);
    private static final String APPLICATION = "{\"firstName\":\"Jean\",\"lastName\":\"Mba\",\"phone\":\"+241 62 00 00 00\",\"email\":\"jean@example.com\",\"city\":\"Libreville\",\"licenceNumber\":\"L-123\"}";

    private User manager;
    private User maliManager;
    private User viewer;

    @BeforeEach
    void people() {
        manager = staff("ga.manager@ndzela.test", "GA", "GESTIONNAIRE");
        maliManager = staff("ml.manager@ndzela.test", "ML", "GESTIONNAIRE");
        viewer = staff("ga.viewer@ndzela.test", "GA", "VIEWER");
    }

    private ResultActions apply(String json, boolean withDocuments) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/public/applications").file(new MockMultipartFile("data", "", "application/json", json.getBytes(StandardCharsets.UTF_8)));
        if (withDocuments) {
            request.file(new MockMultipartFile("photo", "p.png", "image/png", PNG)).file(new MockMultipartFile("licence", "l.pdf", "application/pdf", PDF))
                    .file(new MockMultipartFile("id", "i.png", "image/png", PNG));
        }
        return mvc.perform(request);
    }

    private ResultActions contact(String json) throws Exception {
        return mvc.perform(post("/api/v1/public/contact").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String message(String name) {
        return "{\"name\":\"" + name + "\",\"email\":\"a@b.co\",\"subject\":\"Question\",\"message\":\"Bonjour\"}";
    }

    @Test
    void anAnonymousVisitorCanApplyAndTheStaffOfTheCountryReceivesIt() throws Exception {
        apply(APPLICATION, true).andExpect(status().isAccepted());
        assertEquals(1, jdbc.queryForObject("select count(*) from driver_applications where status = 'NEW'", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from notifications where type = 'DRIVER_APPLICATION'", Integer.class));

        String body = mvc.perform(get("/api/v1/applications").header("Authorization", bearer(manager))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].lastName").value("Mba")).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.items[0].id");
        mvc.perform(get("/api/v1/applications/" + id + "/files/photo").header("Authorization", bearer(manager))).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(get("/api/v1/applications/" + id + "/files/licence").header("Authorization", bearer(manager))).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
        mvc.perform(get("/api/v1/applications/" + id + "/files/address-proof").header("Authorization", bearer(manager))).andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/applications/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\",\"note\":\"Dossier complet\",\"interviewAt\":\"" + java.time.OffsetDateTime.now().plusDays(3) + "\",\"candidateMessage\":\"Apportez vos originaux.\"}")
                .header("Authorization", bearer(manager))).andExpect(status().isOk()).andExpect(jsonPath("$.interviewAt").isNotEmpty()).andExpect(jsonPath("$.candidateMessage").value("Apportez vos originaux.")).andExpect(jsonPath("$.status").value("ACCEPTED")).andExpect(jsonPath("$.reviewedBy").value("ga.manager@ndzela.test"));
        assertEquals(1, jdbc.queryForObject("select count(*) from audit_logs where action = 'APPLICATION_ACCEPTED'", Integer.class));
    }

    @Test
    void acceptingNeedsAFutureInterviewAndRefusingNeedsAReason() throws Exception {
        apply(APPLICATION, true).andExpect(status().isAccepted());
        String body = mvc.perform(get("/api/v1/applications").header("Authorization", bearer(manager))).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.items[0].id");
        String url = "/api/v1/applications/" + id + "/status";
        decide(manager, url, "{\"status\":\"ACCEPTED\"}").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INTERVIEW_DATE_REQUIRED"));
        decide(manager, url, "{\"status\":\"ACCEPTED\",\"interviewAt\":\"" + java.time.OffsetDateTime.now().minusDays(1) + "\"}").andExpect(status().isUnprocessableEntity());
        decide(manager, url, "{\"status\":\"REJECTED\"}").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("REASON_REQUIRED"));
        decide(manager, url, "{\"status\":\"REJECTED\",\"candidateMessage\":\"Permis trop récent.\"}").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.interviewAt").doesNotExist()).andExpect(jsonPath("$.notificationFailed").value(false));
        decide(manager, url, "{\"status\":\"CONTACTED\"}").andExpect(status().isOk());
    }

    private ResultActions decide(User as, String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json).header("Authorization", bearer(as)));
    }

    @Test
    void theApplicationNeedsThePhotoTheLicenceAndTheIdentityDocumentAndSavesNothingOtherwise() throws Exception {
        apply(APPLICATION, false).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DOCUMENTS_REQUIRED"));
        apply("{\"firstName\":\"\",\"lastName\":\"Mba\",\"phone\":\"12\"}", true).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/public/applications").file(new MockMultipartFile("data", "", "application/json", APPLICATION.getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("photo", "p.png", "image/png", PNG)).file(new MockMultipartFile("licence", "l.txt", "text/plain", "not an image".getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("id", "i.png", "image/png", PNG))).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_FILE"));
        assertEquals(0, jdbc.queryForObject("select count(*) from driver_applications", Integer.class));
    }

    @Test
    void aContactMessageIsSavedAndARobotIsIgnored() throws Exception {
        contact(message("Awa")).andExpect(status().isAccepted());
        contact("{\"name\":\"Bot\",\"subject\":\"Hi\",\"message\":\"Buy\",\"website\":\"http://spam\"}").andExpect(status().isAccepted());
        contact("{\"name\":\"\",\"subject\":\"Hi\",\"message\":\"x\"}").andExpect(status().isBadRequest());
        assertEquals(1, jdbc.queryForObject("select count(*) from contact_messages", Integer.class));

        String body = mvc.perform(get("/api/v1/contact-messages").header("Authorization", bearer(manager))).andExpect(jsonPath("$.total").value(1)).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.items[0].id");
        mvc.perform(post("/api/v1/contact-messages/" + id + "/handle").header("Authorization", bearer(manager))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("HANDLED"));
        mvc.perform(get("/api/v1/inquiries/new-count").header("Authorization", bearer(manager))).andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void staffCanReplyToAMessageByEmailAndDeleteItAfterward() throws Exception {
        contact(message("Awa")).andExpect(status().isAccepted());
        String body = mvc.perform(get("/api/v1/contact-messages").header("Authorization", bearer(manager))).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.items[0].id");

        mvc.perform(post("/api/v1/contact-messages/" + id + "/reply").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Merci, nous revenons vers vous.\"}")
                        .header("Authorization", bearer(manager)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("HANDLED")).andExpect(jsonPath("$.replyFailed").value(false))
                .andExpect(jsonPath("$.replyMessage").value("Merci, nous revenons vers vous."));
        assertEquals(1, jdbc.queryForObject("select count(*) from audit_logs where action = 'MESSAGE_REPLIED'", Integer.class));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/contact-messages/" + id).header("Authorization", bearer(viewer)))
                .andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/contact-messages/" + id).header("Authorization", bearer(manager)))
                .andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("select count(*) from contact_messages", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from audit_logs where action = 'MESSAGE_DELETED'", Integer.class));
    }

    @Test
    void aMessageWithNoEmailCannotBeRepliedToByEmail() throws Exception {
        contact("{\"name\":\"Awa\",\"phone\":\"+241620000000\",\"subject\":\"Question\",\"message\":\"Bonjour\"}").andExpect(status().isAccepted());
        String body = mvc.perform(get("/api/v1/contact-messages").header("Authorization", bearer(manager))).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.items[0].id");

        mvc.perform(post("/api/v1/contact-messages/" + id + "/reply").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Bonjour\"}")
                        .header("Authorization", bearer(manager)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("NO_EMAIL_ON_FILE"));
    }

    @Test
    void staffCanDeleteAnApplicationWithItsDocuments() throws Exception {
        apply(APPLICATION, true).andExpect(status().isAccepted());
        String body = mvc.perform(get("/api/v1/applications").header("Authorization", bearer(manager))).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.items[0].id");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/applications/" + id).header("Authorization", bearer(manager)))
                .andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("select count(*) from driver_applications", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from audit_logs where action = 'APPLICATION_DELETED'", Integer.class));
        mvc.perform(get("/api/v1/applications/" + id).header("Authorization", bearer(manager))).andExpect(status().isNotFound());
    }

    @Test
    void oneVisitorCannotFloodTheEndpoints() throws Exception {
        for (int i = 0; i < 5; i++) {
            contact(message("Visitor " + i)).andExpect(status().isAccepted());
        }
        contact(message("One too many")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
        assertEquals(5, jdbc.queryForObject("select count(*) from contact_messages", Integer.class));
    }

    @Test
    void onlyTheStaffOfThatCountryWithThePermissionCanReadThem() throws Exception {
        apply(APPLICATION, true).andExpect(status().isAccepted());
        contact(message("Awa")).andExpect(status().isAccepted());
        mvc.perform(get("/api/v1/applications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/applications").header("Authorization", bearer(viewer))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/applications").header("Authorization", bearer(maliManager))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/contact-messages").header("Authorization", bearer(maliManager))).andExpect(jsonPath("$.total").value(0));
        assertEquals(0, jdbc.queryForObject("select count(*) from notifications where user_id = (select id from users where email = 'ml.manager@ndzela.test')", Integer.class));
    }
}
