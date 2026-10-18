-- V39__inquiry_replies.sql
-- Tracks whether an application-decision email actually reached the candidate, and lets staff reply
-- to a contact message by real e-mail instead of a client-side mailto link.
ALTER TABLE driver_applications ADD COLUMN notification_failed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE contact_messages ADD COLUMN reply_message TEXT;
ALTER TABLE contact_messages ADD COLUMN replied_by VARCHAR(255);
ALTER TABLE contact_messages ADD COLUMN replied_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE contact_messages ADD COLUMN reply_failed BOOLEAN NOT NULL DEFAULT FALSE;
