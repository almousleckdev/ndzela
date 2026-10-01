package com.ndzela.admin.driver;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.vehicle.VehicleApiTest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared people and helpers for the driver tests. */
public abstract class AbstractDriverTest extends AbstractIntegrationTest {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    protected User manager;
    protected User maliManager;
    protected User admin;
    protected User viewer;
    protected User root;

    @BeforeEach
    void people() {
        jdbc.update("update countries set contract_prefix = 'MLI-', contract_template = 'GA-1', legal_name = 'NDZELA MALI', legal_form = 'SARL', head_office = 'Bamako', registry_number = 'ML-TEST', "
                + "legal_phone = '+223 70 00 00 00', legal_email = 'mali@ndzela.test', signing_city = 'Bamako' where code = 'ML'");
        manager = staff("ga.manager@ndzela.test", "GA", "GESTIONNAIRE");
        maliManager = staff("ml.manager@ndzela.test", "ML", "GESTIONNAIRE");
        admin = staff("ga.admin@ndzela.test", "GA", "ADMIN");
        viewer = staff("ga.viewer@ndzela.test", "GA", "VIEWER");
        root = staff("root@ndzela.test", null, "SUPER_ADMIN");
    }

    protected ResultActions send(MockHttpServletRequestBuilder request, User as) throws Exception {
        return mvc.perform(request.header("Authorization", bearer(as)));
    }

    protected ResultActions postJson(String url, String body, User as) throws Exception {
        return send(post(url).contentType(MediaType.APPLICATION_JSON).content(body), as);
    }

    protected String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    protected String newVehicle(User as) throws Exception {
        int n = SEQUENCE.incrementAndGet();
        return id(postJson("/api/v1/vehicles", VehicleApiTest.json("V-" + n, "PL-" + n), as).andExpect(status().isCreated()));
    }

    protected String newCenter(User as, String name) throws Exception {
        return id(postJson("/api/v1/training-centers", "{\"name\":\"" + name + "\"}", as).andExpect(status().isCreated()));
    }

    protected static final String REASON = "{\"reason\":\"Motif de test\"}";
    protected static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    protected String driverWithoutVehicle(User as, String center) throws Exception {
        String driver = id(createDriver(as, DriverJson.create(newVehicle(as), center)).andExpect(status().isCreated()));
        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, as).andExpect(status().isOk());
        return driver;
    }

    protected ResultActions createDriver(User as, DriverJson body) throws Exception {
        return multipartCreate(body.build(), PNG, as, null);
    }

    protected ResultActions multipartCreate(String json, byte[] photo, User as, String country) throws Exception {
        var request = multipart("/api/v1/drivers")
                .file(new MockMultipartFile("data", "", "application/json", json.getBytes(StandardCharsets.UTF_8)));
        if (photo != null) {
            request.file(new MockMultipartFile("photo", "photo.png", "image/png", photo));
        }
        if (country != null) {
            request.header("X-Country", country);
        }
        return mvc.perform(request.header("Authorization", bearer(as)));
    }

    protected int rows(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    protected int auditRows(String action) {
        return rows("select count(*) from audit_logs where action = ?", action);
    }
}
