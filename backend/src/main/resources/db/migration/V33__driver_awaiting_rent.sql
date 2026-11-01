-- V33__driver_awaiting_rent.sql
-- Rent of a day that has ended but whose deadline (the next day at noon) has not passed yet: not late, but owed. Kept with the other arrears figures of the driver row.
ALTER TABLE drivers ADD COLUMN awaiting_days INT NOT NULL DEFAULT 0;
ALTER TABLE drivers ADD COLUMN awaiting_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
