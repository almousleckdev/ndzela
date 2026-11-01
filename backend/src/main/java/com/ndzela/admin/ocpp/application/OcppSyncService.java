package com.ndzela.admin.ocpp.application;

import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.infrastructure.ocpp.FleetCursorExpiredException;
import com.ndzela.admin.infrastructure.ocpp.FleetException;
import com.ndzela.admin.infrastructure.ocpp.FleetPage;
import com.ndzela.admin.infrastructure.ocpp.FleetRateLimitedException;
import com.ndzela.admin.infrastructure.ocpp.OcppFleetClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

// Keeps our sessions equal to the OCPP platform's: the Fleet API's incremental sync is the source of truth.
// Webhooks are only a hint that something changed, so they ask for a sync; one sync runs at a time across all instances.
@Slf4j
@Service
@RequiredArgsConstructor
public class OcppSyncService {
    private static final String LOCK = "ocpp:sync:lock";
    private static final String WANTED = "ocpp:sync:wanted";
    private static final String BACKOFF = "ocpp:sync:backoff";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final int MAX_PAGES_PER_RUN = 100;

    private final AppProperties props;
    private final CountryRepository countries;
    private final OcppFleetClient fleet;
    private final OcppSessionWriter writer;
    private final OcppBatteryService battery;
    private final OcppSyncState state;
    private final StringRedisTemplate redis;

    /** A webhook arrived: sync soon. Many webhooks in a row make one sync. */
    public void requestSync() {
        redis.opsForValue().set(WANTED, "1", Duration.ofHours(1));
    }

    /** Called every few seconds: syncs when a webhook asked for it or the interval has passed, and never while the platform asked us to wait. */
    public void runIfDue() {
        if (!props.ocpp().fleetConfigured() || Boolean.TRUE.equals(redis.hasKey(BACKOFF))) {
            return;
        }
        UUID countryId = countryId();
        boolean asked = Boolean.TRUE.equals(redis.hasKey(WANTED));
        if (asked || state.isDue(countryId, props.ocpp().syncInterval())) {
            redis.delete(WANTED);
            sync();
        }
    }

    /** Pulls every page since the saved cursor. Returns how many sessions were created or updated. */
    public int sync() {
        if (!props.ocpp().fleetConfigured()) {
            return 0;
        }
        String token = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(LOCK, token, LOCK_TTL))) {
            return 0;
        }
        UUID countryId = countryId();
        try {
            state.attempted(countryId);
            int changed = pull(countryId);
            battery.refresh(countryId);
            state.succeeded(countryId);
            return changed;
        } catch (FleetRateLimitedException e) {
            redis.opsForValue().set(BACKOFF, "1", e.retryAfter());
            state.failed(countryId, e.getMessage());
            log.warn("OCPP sync paused: {}", e.getMessage());
        } catch (FleetException e) {
            state.failed(countryId, e.getMessage());
            log.error("OCPP sync failed: {}", e.getMessage(), e);
        } finally {
            if (token.equals(redis.opsForValue().get(LOCK))) {
                redis.delete(LOCK);
            }
        }
        return 0;
    }

    // A cursor the platform refuses (410) is dropped and the sync starts over: sessions are applied by version, so replaying them is harmless.
    private int pull(UUID countryId) {
        String saved = state.cursor(countryId);
        try {
            return pullFrom(countryId, saved);
        } catch (FleetCursorExpiredException e) {
            if (saved == null) {
                throw e;
            }
            log.warn("OCPP cursor refused by the platform, syncing again from the start");
            state.saveCursor(countryId, null);
            return pullFrom(countryId, null);
        }
    }

    private int pullFrom(UUID countryId, String cursor) {
        int changed = 0;
        for (int page = 0; page < MAX_PAGES_PER_RUN; page++) {
            FleetPage result = fleet.sync(cursor, props.ocpp().syncPageSize());
            changed += writer.apply(countryId, result.items(), result.nextCursor());
            cursor = result.nextCursor() != null ? result.nextCursor() : cursor;
            if (!result.hasMore()) {
                break;
            }
        }
        return changed;
    }

    private UUID countryId() {
        return countries.findByCode(props.ocpp().countryCode())
                .orElseThrow(() -> new IllegalStateException("The OCPP country does not exist: " + props.ocpp().countryCode())).getId();
    }
}
