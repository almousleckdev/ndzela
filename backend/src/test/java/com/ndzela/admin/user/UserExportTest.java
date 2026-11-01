package com.ndzela.admin.user;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.support.ExportSheets;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserExportTest extends AbstractDriverTest {
    @Test
    void theUsersExportIsAProfessionalSpreadsheetThatIsAudited() throws Exception {
        var rows = ExportSheets.rows(send(get("/api/v1/users/export"), admin).andExpect(status().isOk()).andExpect(header().string("Content-Disposition", containsString("utilisateurs.xlsx"))));
        assertEquals("Liste des utilisateurs", rows.get(1).get(1));
        assertEquals(List.of("E-mail", "Prénom", "Nom"), rows.get(ExportSheets.HEADER).subList(0, 3));
        var data = ExportSheets.data(rows);
        assertTrue(data.stream().anyMatch(r -> r.get(0).equals("ga.admin@ndzela.test")));
        assertEquals(1, rows("select count(*) from audit_logs where action = 'USERS_EXPORTED'"));
    }

    @Test
    void anExportCanBeChosenAsPdf() throws Exception {
        assertTrue(ExportSheets.isPdf(send(get("/api/v1/users/export?format=PDF"), admin).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("utilisateurs.pdf")))));
    }

    @Test
    void theExportNeedsItsOwnPermission() throws Exception {
        send(get("/api/v1/users/export"), manager).andExpect(status().isForbidden());
        send(get("/api/v1/users/export"), viewer).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users/export")).andExpect(status().isUnauthorized());
    }
}
