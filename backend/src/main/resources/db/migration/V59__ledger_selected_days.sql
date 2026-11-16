-- The rent days a staff payment was meant to pay (null: it settles the oldest days first)
ALTER TABLE ledger_entries ADD COLUMN selected_days DATE[];
