-- V45__id_tags.sql: which NDZela driver holds each charging idTag (from the Zella Charging app) and who may link or unlink them.
CREATE TABLE id_tags (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    id_tag VARCHAR(100) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    first_name VARCHAR(120),
    last_name VARCHAR(120),
    status VARCHAR(12) NOT NULL,
    matched_on VARCHAR(20) NOT NULL,
    decided_by VARCHAR(255) NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_id_tags_tag UNIQUE (country_id, id_tag),
    CONSTRAINT chk_id_tags_status CHECK (status IN ('LINKED', 'SUGGESTED', 'UNLINKED'))
);
-- A driver holds one linked idTag at a time
CREATE UNIQUE INDEX ux_id_tags_linked_driver ON id_tags (driver_id) WHERE status = 'LINKED';
CREATE INDEX idx_id_tags_driver ON id_tags (driver_id);

INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'charging:link') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name = 'charging:link' AND r.name IN ('SUPER_ADMIN', 'ADMIN', 'GESTIONNAIRE')
ON CONFLICT DO NOTHING;
