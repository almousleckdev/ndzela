-- Gives back the contract, car and payment history of drivers whose "Mettre inactif" ended them (before ADR-0046).
--
-- Run with psql against the database, read the report, then change the last line from ROLLBACK to COMMIT and run it again.
--   psql "$DATABASE_URL" -f backend/scripts/restore-deactivated-contracts.sql
--
-- A driver is handled when a contract of his was terminated within 2 minutes of a DRIVER_DEACTIVATED audit line and he is
-- INACTIVE or was reactivated (ACTIVE) afterwards. That contract (number, payments, history) is reopened.
--  * No new contract yet: it is reopened on the old car, if the car is still free.
--  * He already got a new contract after being reactivated ("Jour 0 sur 1 248"): that new contract is closed and replaced by the
--    old one, on the car he has now, but only when the new contract has no payment, incident or cancelled day; otherwise it is listed.
-- The rent between the deactivation and the reactivation (or until today if still inactive) is not charged: it becomes a pause.
BEGIN;

CREATE TEMP TABLE restore_candidates ON COMMIT DROP AS
WITH ended AS (
    SELECT DISTINCT ON (c.driver_id) c.id AS contract_id, c.country_id, c.driver_id, c.vehicle_id, c.contract_number, a.created_at AS deactivated_at
    FROM contracts c
    JOIN LATERAL (SELECT l.created_at FROM audit_logs l WHERE l.action = 'DRIVER_DEACTIVATED' AND l.target_id = c.driver_id::text
                  AND l.created_at BETWEEN c.ended_at - interval '2 minutes' AND c.ended_at + interval '2 minutes'
                  ORDER BY l.created_at DESC LIMIT 1) a ON TRUE
    WHERE c.status = 'TERMINATED'
    ORDER BY c.driver_id, c.ended_at DESC
)
SELECT e.contract_id, e.country_id, e.driver_id, e.contract_number, e.deactivated_at, d.status AS driver_status,
       d.last_name || ' ' || d.first_name AS driver_name, k.timezone,
       CASE WHEN d.status = 'INACTIVE' THEN NULL ELSE (SELECT min(r.created_at) FROM audit_logs r
            WHERE r.action = 'DRIVER_REACTIVATED' AND r.target_id = d.id::text AND r.created_at > e.deactivated_at) END AS reactivated_at,
       n.id AS new_contract_id,
       (n.id IS NOT NULL AND (EXISTS (SELECT 1 FROM ledger_entries x WHERE x.contract_id = n.id)
                           OR EXISTS (SELECT 1 FROM day_incidents x WHERE x.contract_id = n.id)
                           OR EXISTS (SELECT 1 FROM cancelled_rent_days x WHERE x.contract_id = n.id))) AS new_has_activity,
       o.vehicle_id AS open_vehicle_id,
       COALESCE(o.vehicle_id, e.vehicle_id) AS target_vehicle_id,
       (o.vehicle_id IS NOT NULL OR (v.status = 'AVAILABLE'
            AND NOT EXISTS (SELECT 1 FROM contracts x WHERE x.vehicle_id = e.vehicle_id AND x.status = 'ACTIVE')
            AND NOT EXISTS (SELECT 1 FROM vehicle_assignments x WHERE x.vehicle_id = e.vehicle_id AND x.ended_at IS NULL))) AS vehicle_ok
FROM ended e
JOIN drivers d ON d.id = e.driver_id
JOIN countries k ON k.id = e.country_id
JOIN vehicles v ON v.id = e.vehicle_id
LEFT JOIN contracts n ON n.driver_id = e.driver_id AND n.status = 'ACTIVE'
LEFT JOIN vehicle_assignments o ON o.driver_id = e.driver_id AND o.ended_at IS NULL
WHERE d.status IN ('INACTIVE', 'ACTIVE');

ALTER TABLE restore_candidates ADD COLUMN restorable BOOLEAN;
UPDATE restore_candidates SET restorable = vehicle_ok AND NOT new_has_activity;

-- Report: restorable = true rows are restored below; false rows need a manual decision.
SELECT driver_name, driver_status, contract_number AS old_contract, deactivated_at, reactivated_at,
       (new_contract_id IS NOT NULL) AS has_new_contract, new_has_activity, vehicle_ok, restorable
FROM restore_candidates ORDER BY deactivated_at;

-- The empty contract opened after the reactivation makes way for the original one.
UPDATE contracts SET status = 'TERMINATED', ended_at = now(), end_reason = 'Remplacé par le contrat d''origine restauré'
WHERE id IN (SELECT new_contract_id FROM restore_candidates WHERE restorable AND new_contract_id IS NOT NULL);

UPDATE contracts c SET status = 'ACTIVE', ended_at = NULL, end_reason = NULL, vehicle_id = r.target_vehicle_id
FROM restore_candidates r WHERE c.id = r.contract_id AND r.restorable;

UPDATE vehicles SET status = 'ASSIGNED', updated_at = now()
WHERE id IN (SELECT target_vehicle_id FROM restore_candidates WHERE restorable AND open_vehicle_id IS NULL);

INSERT INTO vehicle_assignments (id, country_id, driver_id, vehicle_id, assigned_at)
SELECT gen_random_uuid(), r.country_id, r.driver_id, r.target_vehicle_id,
       COALESCE((SELECT max(a.assigned_at) FROM vehicle_assignments a WHERE a.driver_id = r.driver_id AND a.vehicle_id = r.target_vehicle_id), now())
FROM restore_candidates r WHERE r.restorable AND r.open_vehicle_id IS NULL;

INSERT INTO driver_pauses (id, country_id, driver_id, cause, started_on, ended_on, reason)
SELECT gen_random_uuid(), r.country_id, r.driver_id, 'INACTIVE', (r.deactivated_at AT TIME ZONE r.timezone)::date,
       (r.reactivated_at AT TIME ZONE r.timezone)::date, 'Contrat restauré après mise en inactivité'
FROM restore_candidates r
WHERE r.restorable AND (r.reactivated_at IS NULL OR (r.reactivated_at AT TIME ZONE r.timezone)::date > (r.deactivated_at AT TIME ZONE r.timezone)::date);

INSERT INTO audit_logs (id, actor, action, target, result, target_type, target_id, country_id, details, created_at)
SELECT gen_random_uuid(), 'system', 'CONTRACT_RESTORED', r.driver_name, 'SUCCESS', 'DRIVER', r.driver_id::text, r.country_id,
       'contract ' || r.contract_number || ' reopened after deactivation (ADR-0046)', now()
FROM restore_candidates r WHERE r.restorable;

-- Review the report above, then replace the next line with COMMIT;
ROLLBACK;
