-- V20__driver_support_and_profile.sql
-- How a country's drivers reach support, and the messages they send from the app.
ALTER TABLE countries ADD COLUMN support_phone VARCHAR(30);
ALTER TABLE countries ADD COLUMN support_whatsapp VARCHAR(30);
ALTER TABLE countries ADD COLUMN support_email VARCHAR(255);
UPDATE countries SET support_phone = legal_phone, support_whatsapp = legal_phone, support_email = legal_email;

CREATE TABLE support_messages (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    body VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_support_messages_driver ON support_messages (driver_id, created_at DESC);
