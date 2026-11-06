package com.ndzela.admin.charging;

import com.ndzela.admin.charging.application.ZellaFeedService;
import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A driver linked to a Zella customer, and the helpers to feed and read the sessions of the charging pages.
abstract class AbstractZellaFeedTest extends AbstractDriverTest {
    @Autowired protected ZellaFeedService feed;

    protected String email;
    protected String phone;

    @BeforeEach
    void linkedDriver() throws Exception {
        DriverJson json = DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"));
        email = (String) json.personalValue("email");
        phone = (String) json.personalValue("phonePrimary");
        createDriver(manager, json).andExpect(status().isCreated());
        for (int i = 0; i < 50 && zella.asked().isEmpty(); i++) {
            Thread.sleep(100);
        }
        Thread.sleep(200);
        zella.asked().clear();
        zella.customer("Z-ABC", "cust-1", email, phone, "Almousleck", "ATALIB AG");
        postJson("/api/v1/charging/id-tags/lookup", "{}", manager).andExpect(status().isOk());
    }

    protected void session(String transaction) {
        jdbc.update("insert into charging_sessions (id, country_id, transaction_id, id_tag, started_at, ended_at, status, energy_wh, cost_minor, currency, currency_decimals, raw) "
                + "select gen_random_uuid(), id, ?, 'Z-ABC', now(), now(), 'COMPLETED', 10000, 3500, 'XAF', 0, '{}' from countries where code = 'GA'", transaction);
    }

    protected int sync() {
        return TenantHolder.callAs(TenantContext.country(country("GA").getId()), () -> feed.sync(country("GA").getId()));
    }

    protected ResultActions sessions() throws Exception {
        return send(get("/api/v1/charging/sessions"), manager);
    }

    protected Map<String, Object> row(String transaction) {
        return jdbc.queryForMap("select charge_point_id, connector_id, id_tag, status, energy_wh, cost_minor, currency, soc_before, soc_after, power_kw, "
                + "price_per_kwh_minor, duration_seconds from charging_sessions where transaction_id = ?", transaction);
    }
}
