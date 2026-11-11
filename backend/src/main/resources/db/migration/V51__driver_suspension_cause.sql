-- V51__driver_suspension_cause.sql: why a driver is suspended (ARREARS is lifted by paying, MANUAL never is) and when the system last restored one.
ALTER TABLE drivers ADD COLUMN suspension_cause VARCHAR(10);
ALTER TABLE drivers ADD COLUMN auto_restored_at TIMESTAMP WITH TIME ZONE;

UPDATE drivers SET suspension_cause = CASE WHEN status_reason LIKE 'Impayés%' THEN 'ARREARS' ELSE 'MANUAL' END WHERE status = 'SUSPENDED';
