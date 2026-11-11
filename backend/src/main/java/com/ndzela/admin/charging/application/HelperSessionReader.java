package com.ndzela.admin.charging.application;

import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// The charges of one account in the country of the request, shaped for the signals.
@Component
@RequiredArgsConstructor
class HelperSessionReader {
    private static final int HISTORY_DAYS = 30;
    private static final BigDecimal WH_PER_KWH = BigDecimal.valueOf(1000);

    private final JdbcTemplate jdbc;

    List<HelpSession> between(String idTag, OffsetDateTime from, OffsetDateTime to) {
        UUID country = TenantHolder.requireCountry();
        return jdbc.query("select started_at, ended_at, charge_point_id, connector_id, soc_before, soc_after, energy_wh from charging_sessions "
                        + "where country_id = ? and id_tag = ? and started_at >= ? and started_at <= ? order by started_at",
                (rs, row) -> new HelpSession(rs.getObject("started_at", OffsetDateTime.class), rs.getObject("ended_at", OffsetDateTime.class), rs.getString("charge_point_id"),
                        rs.getInt("connector_id"), rs.getBigDecimal("soc_before"), rs.getBigDecimal("soc_after"),
                        rs.getBigDecimal("energy_wh") == null ? null : rs.getBigDecimal("energy_wh").divide(WH_PER_KWH, 3, RoundingMode.HALF_UP)),
                country, idTag, from, to);
    }

    /** What the account normally takes over `days` days, from its last 30 days before `before`; null when it has no history. */
    BigDecimal normalKwh(String idTag, OffsetDateTime before, int days) {
        BigDecimal wh = jdbc.queryForObject("select coalesce(sum(energy_wh), 0) from charging_sessions where country_id = ? and id_tag = ? and started_at >= ? and started_at < ?",
                BigDecimal.class, TenantHolder.requireCountry(), idTag, before.minusDays(HISTORY_DAYS), before);
        if (wh == null || wh.signum() == 0) {
            return null;
        }
        return wh.divide(WH_PER_KWH, 3, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf(HISTORY_DAYS), 3, RoundingMode.HALF_UP);
    }
}
