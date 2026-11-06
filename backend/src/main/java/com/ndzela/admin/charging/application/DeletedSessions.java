package com.ndzela.admin.charging.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

// The sessions staff deleted: the sync leaves them out so they do not come back
@Component
@RequiredArgsConstructor
public class DeletedSessions {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public void remember(UUID countryId, String transactionId, String deletedBy, String reason) {
        jdbc.update("INSERT INTO deleted_sessions (country_id, transaction_id, deleted_by, reason) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING", countryId, transactionId, deletedBy, reason);
    }

    public <T> List<T> withoutDeleted(UUID countryId, List<T> items, java.util.function.Function<T, String> transactionOf) {
        if (items.isEmpty()) {
            return items;
        }
        Collection<String> ids = items.stream().map(transactionOf).distinct().toList();
        List<String> gone = named.query("SELECT transaction_id FROM deleted_sessions WHERE country_id = :country AND transaction_id IN (:ids)",
                new MapSqlParameterSource("country", countryId).addValue("ids", ids), (rs, i) -> rs.getString(1));
        return gone.isEmpty() ? items : items.stream().filter(item -> !gone.contains(transactionOf.apply(item))).toList();
    }
}
