-- V49__charging_delete.sql
-- Deleting a network session or a recharge is reserved to the super administrator and the administrator.
-- A deleted session leaves a tombstone so the next sync does not bring it back.
INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'charging:delete') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN') AND p.name = 'charging:delete'
ON CONFLICT DO NOTHING;

CREATE TABLE deleted_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_id UUID NOT NULL REFERENCES countries(id),
    transaction_id VARCHAR(100) NOT NULL,
    deleted_by VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_deleted_sessions_transaction UNIQUE (country_id, transaction_id)
);

-- Recharges stay append-only: an update is always refused, and a delete is accepted only inside the transaction
-- of the deletion service, which switches app.charging_delete on for that transaction alone.
CREATE OR REPLACE FUNCTION charging_entries_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' AND current_setting('app.charging_delete', true) = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'charging_entries is append-only';
END;
$$;
