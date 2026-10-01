-- V18__deletable_arrears_events.sql
-- Escalation steps are derived data: they follow their contract and driver when those are deleted (which is only allowed while no money is recorded).
ALTER TABLE arrears_events DROP CONSTRAINT arrears_events_contract_id_fkey;
ALTER TABLE arrears_events DROP CONSTRAINT arrears_events_driver_id_fkey;
ALTER TABLE arrears_events ADD CONSTRAINT arrears_events_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE;
ALTER TABLE arrears_events ADD CONSTRAINT arrears_events_driver_id_fkey FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE CASCADE;
