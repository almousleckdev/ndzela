-- V9__audit_trail.sql
ALTER TABLE audit_logs
    ADD COLUMN country_id UUID REFERENCES countries(id),
    ADD COLUMN target_type VARCHAR(50),
    ADD COLUMN target_id VARCHAR(64),
    ADD COLUMN details TEXT;

CREATE INDEX idx_audit_logs_country_created ON audit_logs (country_id, created_at DESC);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);

-- The audit trail is append-only.
CREATE FUNCTION audit_logs_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only';
END;
$$;

CREATE TRIGGER audit_logs_no_change BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit_logs_append_only();

INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'audit:read') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name = 'audit:read' WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
ON CONFLICT DO NOTHING;
