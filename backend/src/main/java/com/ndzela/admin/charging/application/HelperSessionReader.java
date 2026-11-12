package com.ndzela.admin.charging.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// The charges of one account in one country, shaped for the signals.
@Component
@RequiredArgsConstructor
class HelperSessionReader {
    private static final int HISTORY_DAYS = 30;
    private static final long STILL_RUNNING_HOURS = 12;
    private static final BigDecimal WH_PER_KWH = BigDecimal.valueOf(1000);

    private final JdbcTemplate jdbc;

    /** A session with no recorded end that began more than 12 hours ago is not running, its end was lost: it counts as instantaneous. */
    List<HelpSession> between(UUID countryId, String idTag, OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime lost = OffsetDateTime.now().minusHours(STILL_RUNNING_HOURS);
        return jdbc.query("select started_at, ended_at, charge_point_id, connector_id, soc_before, soc_after, energy_wh from charging_sessions "
                        + "where country_id = ? and id_tag = ? and started_at >= ? and started_at <= ? order by started_at",
                (rs, row) -> {
                    OffsetDateTime started = rs.getObject("started_at", OffsetDateTime.class);
                    OffsetDateTime ended = rs.getObject("ended_at", OffsetDateTime.class);
                    return new HelpSession(started, ended == null && started.isBefore(lost) ? started : ended, rs.getString("charge_point_id"), rs.getInt("connector_id"),
                            rs.getBigDecimal("soc_before"), rs.getBigDecimal("soc_after"), kwh(rs.getBigDecimal("energy_wh")));
                },
                countryId, idTag, from, to);
    }

    /** What the account normally takes over `days` days, from the energy of the last 30 days before `before` spread over the days since its first charge there; null when it has no history. */
    BigDecimal normalKwh(UUID countryId, String idTag, OffsetDateTime before, int days) {
        return jdbc.query("select coalesce(sum(energy_wh), 0), min(started_at) from charging_sessions where country_id = ? and id_tag = ? and started_at >= ? and started_at < ?", rs -> {
            rs.next();
            BigDecimal wh = rs.getBigDecimal(1);
            OffsetDateTime first = rs.getObject(2, OffsetDateTime.class);
            if (wh == null || wh.signum() == 0 || first == null) {
                return null;
            }
            long historyDays = Math.max(1, Math.min(HISTORY_DAYS, (long) Math.ceil(Duration.between(first, before).toHours() / 24.0)));
            return kwh(wh).multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf(historyDays), 3, RoundingMode.HALF_UP);
        }, countryId, idTag, before.minusDays(HISTORY_DAYS), before);
    }

    private static BigDecimal kwh(BigDecimal wh) {
        return wh == null ? null : wh.divide(WH_PER_KWH, 3, RoundingMode.HALF_UP);
    }
}
