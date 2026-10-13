-- V30__ledger_payment_reference_lookup.sql
-- Serves the "this transaction reference was already recorded" check. Not unique on purpose: a reversed payment frees its reference.
CREATE INDEX idx_ledger_payment_reference ON ledger_entries (country_id, method, lower(reference)) WHERE entry_type = 'PAYMENT';
