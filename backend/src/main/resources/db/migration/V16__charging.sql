-- V16__charging.sql
-- Arrival battery level (percent) at or below which a car counts as a critical account.
ALTER TABLE countries ADD COLUMN charging_critical_percent INT NOT NULL DEFAULT 20;

CREATE TABLE stations (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_stations_name UNIQUE (country_id, name)
);

CREATE UNIQUE INDEX ux_stations_name_ci ON stations (country_id, lower(name));

CREATE TABLE charging_entries (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    entry_type VARCHAR(12) NOT NULL,
    vehicle_id UUID REFERENCES vehicles(id),
    driver_id UUID REFERENCES drivers(id),
    public_label VARCHAR(100),
    station_id UUID NOT NULL REFERENCES stations(id),
    amount NUMERIC(19,4) NOT NULL,
    method VARCHAR(20),
    reference VARCHAR(100),
    battery_before INT,
    battery_after INT,
    kwh NUMERIC(10,2),
    reverses_entry_id UUID REFERENCES charging_entries(id),
    reason VARCHAR(255),
    request_id UUID,
    recorded_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_charging_type CHECK (entry_type IN ('RECHARGE', 'REVERSAL')),
    CONSTRAINT chk_charging_amount CHECK (amount > 0),
    CONSTRAINT chk_charging_target CHECK ((vehicle_id IS NOT NULL) <> (public_label IS NOT NULL)),
    CONSTRAINT chk_charging_battery CHECK (entry_type = 'REVERSAL' OR (battery_before BETWEEN 0 AND 100 AND battery_after BETWEEN 0 AND 100 AND battery_after >= battery_before))
);

CREATE UNIQUE INDEX ux_charging_reversal ON charging_entries (reverses_entry_id) WHERE reverses_entry_id IS NOT NULL;
CREATE UNIQUE INDEX ux_charging_request ON charging_entries (country_id, request_id) WHERE request_id IS NOT NULL;
CREATE INDEX idx_charging_country_created ON charging_entries (country_id, created_at DESC);
CREATE INDEX idx_charging_station ON charging_entries (station_id);

-- Charging records are append-only: a mistake is corrected by a reversal row.
CREATE FUNCTION charging_entries_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'charging_entries is append-only';
END;
$$;

CREATE TRIGGER charging_entries_no_change BEFORE UPDATE OR DELETE ON charging_entries
    FOR EACH ROW EXECUTE FUNCTION charging_entries_append_only();

INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'charging:read'),
    (gen_random_uuid(), 'charging:create'),
    (gen_random_uuid(), 'charging:reverse'),
    (gen_random_uuid(), 'charging:export'),
    (gen_random_uuid(), 'charging:manage')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON
       (r.name = 'SUPER_ADMIN' AND p.name LIKE 'charging:%')
    OR (r.name = 'ADMIN' AND p.name IN ('charging:read', 'charging:reverse', 'charging:export', 'charging:manage'))
    OR (r.name = 'GESTIONNAIRE' AND p.name IN ('charging:read', 'charging:create', 'charging:manage'))
    OR (r.name = 'CAISSIER' AND p.name IN ('charging:read', 'charging:create'))
    OR (r.name = 'COMPTABLE' AND p.name IN ('charging:read', 'charging:export'))
    OR (r.name = 'VIEWER' AND p.name = 'charging:read')
ON CONFLICT DO NOTHING;
