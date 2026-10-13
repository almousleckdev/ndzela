package com.ndzela.admin.infrastructure.ocpp;

import com.ndzela.admin.TestProperties;
import com.ndzela.admin.config.properties.OcppProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Reads the real Fleet API (read-only); off unless OCPP_LIVE_TEST=true and OCPP_FLEET_API_KEY is set
@EnabledIfEnvironmentVariable(named = "OCPP_LIVE_TEST", matches = "true")
class OcppFleetLiveTest {
    private static OcppFleetClient client(String key) {
        OcppProperties base = TestProperties.create().ocpp();
        OcppProperties live = new OcppProperties("https://zella-ocpp.com", key, base.webhookSecret(), base.webhookHeader(), base.countryCode(), false, base.syncInterval(), 100, Duration.ofSeconds(20));
        return new OcppFleetClient(RestClient.builder().baseUrl(live.baseUrl()).build(), live, JsonMapper.builder().build());
    }

    @Test
    void theSyncAnswersAndGivesACursorThatIsAcceptedBack() {
        OcppFleetClient client = client(System.getenv("OCPP_FLEET_API_KEY"));

        FleetPage first = client.sync(null, 100);
        assertNotNull(first.nextCursor());

        FleetPage next = client.sync(first.nextCursor(), 100);
        assertNotNull(next.items());
    }

    @Test
    void aWrongKeyIsRefused() {
        assertThrows(FleetException.class, () -> client("zk_live_wrong").sync(null, 10));
    }
}
