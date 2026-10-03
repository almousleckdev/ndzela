-- V26__public_site_inquiries.sql: what visitors of the public website send: driver applications and contact messages.
CREATE TABLE driver_applications (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(255),
    birth_date DATE,
    city VARCHAR(100),
    address VARCHAR(255),
    licence_number VARCHAR(50),
    message VARCHAR(2000),
    photo_file VARCHAR(100) NOT NULL,
    licence_file VARCHAR(100) NOT NULL,
    id_file VARCHAR(100) NOT NULL,
    address_proof_file VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    staff_note VARCHAR(500),
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_driver_applications_country_status ON driver_applications (country_id, status, created_at DESC);

CREATE TABLE contact_messages (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(30),
    subject VARCHAR(150) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    handled_by VARCHAR(255),
    handled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_contact_messages_country_status ON contact_messages (country_id, status, created_at DESC);

INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'inquiries:read'), (gen_random_uuid(), 'inquiries:manage') ON CONFLICT (name) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN', 'GESTIONNAIRE') AND p.name IN ('inquiries:read', 'inquiries:manage')
ON CONFLICT DO NOTHING;
