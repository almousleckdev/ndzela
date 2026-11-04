package com.ndzela.admin.infrastructure.pvit;

import com.ndzela.admin.config.properties.PvitProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// Keeps PVit's secret key of each operation account in memory and renews it before its one-hour life ends
@Slf4j
public class PvitSecretProvider {
    private static final Duration RENEW_MARGIN = Duration.ofMinutes(5);

    private final RestClient client;
    private final PvitProperties props;
    private final ObjectMapper mapper;

    private record Key(String secret, Instant renewAt) {}

    private final Map<String, Key> keys = new HashMap<>();

    public PvitSecretProvider(RestClient client, PvitProperties props, ObjectMapper mapper) {
        this.client = client;
        this.props = props;
        this.mapper = mapper;
    }

    public synchronized String current(String account) {
        Key key = keys.get(account);
        if (key == null || !Instant.now().isBefore(key.renewAt())) {
            key = renew(account);
        }
        return key.secret();
    }

    // Forgets the key, for when PVit says it is no longer valid.
    public synchronized void invalidate(String account) {
        keys.remove(account);
    }

    private Key renew(String account) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("operationAccountCode", account);
        form.add("password", props.secretPassword());
        try {
            String body = client.post().uri("/v2/{code}/renew-secret", props.codeUrlSecret())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(String.class);
            JsonNode json = mapper.readTree(body);
            String renewed = json.path("secret").asString(null);
            if (renewed == null || renewed.isBlank()) {
                throw new PvitException("PVit returned no secret key.");
            }
            long seconds = json.path("expires_in").asLong(3600);
            Key key = new Key(renewed, Instant.now().plusSeconds(seconds).minus(RENEW_MARGIN));
            keys.put(account, key);
            log.info("PVit secret key renewed");
            return key;
        } catch (RestClientException e) {
            keys.remove(account);
            throw new PvitException("Could not renew the PVit secret key.", e);
        }
    }
}
