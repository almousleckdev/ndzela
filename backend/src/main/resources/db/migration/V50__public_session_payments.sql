-- V50__public_session_payments.sql: payments of public sessions are kept without a customer, and the public feed is read again to fetch them.
ALTER TABLE session_payments ALTER COLUMN customer_id DROP NOT NULL;
UPDATE zella_sync_state SET public_sync_cursor = NULL;
