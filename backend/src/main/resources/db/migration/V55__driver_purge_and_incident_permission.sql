-- V55: a driver can be erased together with the money recorded on them (drivers:purge), and half days / stops get their own permission.
INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'drivers:purge'),
    (gen_random_uuid(), 'incidents:declare')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN') AND p.name IN ('drivers:purge', 'incidents:declare')
ON CONFLICT DO NOTHING;

-- The ledger stays append-only: a delete is accepted only inside the transaction of the purge service, which switches app.ledger_purge on for that transaction alone.
CREATE OR REPLACE FUNCTION ledger_entries_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' AND current_setting('app.ledger_purge', true) = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'ledger_entries is append-only';
END;
$$;

CREATE OR REPLACE FUNCTION ledger_entry_days_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' AND current_setting('app.ledger_purge', true) = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'ledger_entry_days is append-only';
END;
$$;

-- A half day can also be granted because the contract was signed after noon.
ALTER TABLE day_incidents DROP CONSTRAINT chk_day_incidents_cause;
ALTER TABLE day_incidents ADD CONSTRAINT chk_day_incidents_cause CHECK (cause IN ('ELECTRICITY', 'OTHER', 'LATE_SIGNING'));
