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

// Keeps PVit's secret key in memory and renews it before its one-hour life ends
@Slf4j
public class PvitSecretProvider {
    private static final Duration RENEW_MARGIN = Duration.ofMinutes(5);

    private final RestClient client;
    private final PvitProperties props;
    private final ObjectMapper mapper;

    private String secret;
    private Instant renewAt = Instant.MIN;

    public PvitSecretProvider(RestClient client, PvitProperties props, ObjectMapper mapper) {
        this.client = client;
        this.props = props;
        this.mapper = mapper;
    }

    public synchronized String current() {
        if (secret == null || !Instant.now().isBefore(renewAt)) {
            renew();
        }
        return secret;
    }

    // Forgets the key, for when PVit says it is no longer valid.
    public synchronized void invalidate() {
        secret = null;
    }

    private void renew() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("operationAccountCode", props.operationAccountCode());
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
            secret = renewed;
            renewAt = Instant.now().plusSeconds(seconds).minus(RENEW_MARGIN);
            log.info("PVit secret key renewed");
        } catch (RestClientException e) {
            secret = null;
            throw new PvitException("Could not renew the PVit secret key.", e);
        }
    }
}
