package com.ndzela.admin.notification;

import com.ndzela.admin.notification.application.NotificationService;
import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationDetailTest extends AbstractIntegrationTest {
    @Autowired private NotificationService notifications;

    @Test
    void aNotificationOpensForItsRecipientOnly() throws Exception {
        User owner = staff("owner@ndzela.test", "GA", "GESTIONNAIRE");
        User other = staff("other@ndzela.test", "GA", "GESTIONNAIRE");
        notifications.notify(owner, "CONTACT_MESSAGE", "Nouveau message du site", "Awa : Une longue question sur les contrats");
        String id = jdbc.queryForObject("select id::text from notifications where user_id = ?", String.class, owner.getId());

        mvc.perform(get("/api/v1/notifications/" + id).header("Authorization", bearer(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nouveau message du site")).andExpect(jsonPath("$.type").value("CONTACT_MESSAGE"))
                .andExpect(jsonPath("$.read").value(false));
        mvc.perform(get("/api/v1/notifications/" + id).header("Authorization", bearer(other))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/notifications/" + UUID.randomUUID()).header("Authorization", bearer(owner))).andExpect(status().isNotFound());
    }
}
