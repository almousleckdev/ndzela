package com.ndzela.admin.ocpp;

import com.ndzela.admin.ocpp.application.OcppSyncService;
import com.ndzela.admin.support.AbstractIntegrationTest;

import org.springframework.beans.factory.annotation.Autowired;

// Shared setup and helpers.
abstract class AbstractOcppSyncTest extends AbstractIntegrationTest {
    @Autowired OcppSyncService sync;

    protected String cursor() {
        return jdbc.query("select sync_cursor from ocpp_sync_state", rs -> rs.next() ? rs.getString(1) : null);
    }

    protected int sessionCount() {
        return jdbc.queryForObject("select count(*) from charging_sessions", Integer.class);
    }
}
