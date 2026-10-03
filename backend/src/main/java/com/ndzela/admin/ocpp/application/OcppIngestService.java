package com.ndzela.admin.ocpp.application;

import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.ocpp.domain.ChargingSession;
import com.ndzela.admin.ocpp.domain.ChargingSessionRepository;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

import java.util.Optional;
import java.util.UUID;

// Records what the OCPP platform tells us about a session, once per event and once per transaction
@Slf4j
@Service
@RequiredArgsConstructor
public class OcppIngestService {
    private final AppProperties props;
    private final CountryRepository countries;
    private final ChargingSessionRepository sessions;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    // Returns false when the event was already received or holds no session.
    public boolean ingest(JsonNode body) {
        Optional<OcppSessionParser.Parsed> parsed = OcppSessionParser.parse(body);
        if (parsed.isEmpty()) {
            log.warn("OCPP event without a transaction id ignored");
            return false;
        }
        UUID countryId = countries.findByCode(props.ocpp().countryCode())
                .orElseThrow(() -> new IllegalStateException("The OCPP country does not exist: " + props.ocpp().countryCode())).getId();
        return TenantHolder.callAs(TenantContext.country(countryId), () -> transaction.execute(status -> record(parsed.get(), body.toString())));
    }

    private boolean record(OcppSessionParser.Parsed p, String raw) {
        if (p.eventId() != null && jdbc.update("insert into ocpp_events (event_id) values (?) on conflict do nothing", p.eventId()) == 0) {
            return false;
        }
        ChargingSession session = sessions.findByTransactionId(p.transactionId())
                .orElseGet(() -> ChargingSession.builder().transactionId(p.transactionId()).raw(raw).build());
        merge(session, p);
        session.setRaw(raw);
        sessions.save(session);
        return true;
    }

    // A later event only fills or updates what it carries: a missing field never erases a known one.
    private static void merge(ChargingSession s, OcppSessionParser.Parsed p) {
        if (p.chargePointId() != null) s.setChargePointId(p.chargePointId());
        if (p.connectorId() != null) s.setConnectorId(p.connectorId());
        if (p.idTag() != null) s.setIdTag(p.idTag());
        if (p.startedAt() != null) s.setStartedAt(p.startedAt());
        if (p.endedAt() != null) s.setEndedAt(p.endedAt());
        if (p.status() != null) s.setStatus(p.status());
        if (p.energyWh() != null) s.setEnergyWh(p.energyWh());
        if (p.costMinor() != null) s.setCostMinor(p.costMinor());
        if (p.currency() != null) s.setCurrency(p.currency());
        if (p.stopReason() != null) s.setStopReason(p.stopReason());
        if (p.socBefore() != null) s.setSocBefore(p.socBefore());
        if (p.socAfter() != null) s.setSocAfter(p.socAfter());
    }
}
