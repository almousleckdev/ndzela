-- V32__recharge_from_session_and_live_power.sql
-- A recharge recorded by staff can say which network session it pays for; the session is then "paid" until that recharge is cancelled.
ALTER TABLE charging_entries ADD COLUMN session_id UUID REFERENCES charging_sessions(id);
CREATE INDEX idx_charging_entries_session ON charging_entries (session_id) WHERE session_id IS NOT NULL;

-- The latest power reading (kW) of a session still under way
ALTER TABLE charging_sessions ADD COLUMN power_kw NUMERIC(8, 3);
