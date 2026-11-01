-- V34__ocpp_battery_start_from_sync.sql: the start level now comes from the sync (socStartPercent); the 0 stored before was the charge point's start value.
UPDATE charging_sessions SET soc_before = NULL WHERE soc_before = 0;
-- Read the sessions again from the beginning so every session takes its real start level; the upsert makes this harmless.
UPDATE ocpp_sync_state SET sync_cursor = NULL;
