-- Restores the car and the contract of drivers whose "Mettre inactif" ended them before ADR-0046.
--
-- Run with psql against the database, read the report, then change the last line from ROLLBACK to COMMIT and run it again.
--   psql "$DATABASE_URL" -f backend/scripts/restore-deactivated-contracts.sql
--
-- A driver is restored when: his latest contract was terminated within 2 minutes of a DRIVER_DEACTIVATED audit line,
-- he has no active contract and no open car now, and he is INACTIVE or was reactivated (ACTIVE) afterwards.
-- The same contract (number, payments, history) is reopened on the same car. The rent of the days between the
-- deactivation and the reactivation (or until today if he is still inactive) is not charged: they become a pause.
-- A driver whose car has meanwhile been given to someone else, or who already got a new contract, is only listed.
BEGIN;

CREATE TEMP TABLE restore_candidates ON COMMIT DROP AS
WITH last_contract AS (
    SELECT DISTINCT ON (c.driver_id) c.* FROM contracts c ORDER BY c.driver_id, c.created_at DESC, c.id DESC
)
SELECT c.id AS contract_id, c.country_id, c.driver_id, c.vehicle_id, c.contract_number, d.status AS driver_status,
       d.last_name || ' ' || d.first_name AS driver_name, k.timezone, a.created_at AS deactivated_at,
       CASE WHEN d.status = 'INACTIVE' THEN NULL ELSE (SELECT min(r.created_at) FROM audit_logs r
            WHERE r.action = 'DRIVER_REACTIVATED' AND r.target_id = d.id::text AND r.created_at > a.created_at) END AS reactivated_at,
       (v.status = 'AVAILABLE'
        AND NOT EXISTS (SELECT 1 FROM contracts x WHERE x.vehicle_id = c.vehicle_id AND x.status = 'ACTIVE')
        AND NOT EXISTS (SELECT 1 FROM vehicle_assignments x WHERE x.vehicle_id = c.vehicle_id AND x.ended_at IS NULL)) AS vehicle_free
FROM last_contract c
JOIN drivers d ON d.id = c.driver_id
JOIN countries k ON k.id = c.country_id
JOIN vehicles v ON v.id = c.vehicle_id
JOIN LATERAL (SELECT l.created_at FROM audit_logs l WHERE l.action = 'DRIVER_DEACTIVATED' AND l.target_id = d.id::text
              AND l.created_at BETWEEN c.ended_at - interval '2 minutes' AND c.ended_at + interval '2 minutes'
              ORDER BY l.created_at DESC LIMIT 1) a ON TRUE
WHERE c.status = 'TERMINATED' AND d.status IN ('INACTIVE', 'ACTIVE')
  AND NOT EXISTS (SELECT 1 FROM contracts x WHERE x.driver_id = c.driver_id AND x.status = 'ACTIVE')
  AND NOT EXISTS (SELECT 1 FROM vehicle_assignments x WHERE x.driver_id = c.driver_id AND x.ended_at IS NULL);

-- Report: restored = true rows are restored below; false rows need a manual decision (the car is no longer free).
SELECT driver_name, driver_status, contract_number, deactivated_at, reactivated_at, vehicle_free AS restored FROM restore_candidates ORDER BY deactivated_at;

UPDATE contracts SET status = 'ACTIVE', ended_at = NULL, end_reason = NULL WHERE id IN (SELECT contract_id FROM restore_candidates WHERE vehicle_free);

UPDATE vehicles SET status = 'ASSIGNED', updated_at = now() WHERE id IN (SELECT vehicle_id FROM restore_candidates WHERE vehicle_free);

INSERT INTO vehicle_assignments (id, country_id, driver_id, vehicle_id, assigned_at)
SELECT gen_random_uuid(), r.country_id, r.driver_id, r.vehicle_id,
       COALESCE((SELECT max(a.assigned_at) FROM vehicle_assignments a WHERE a.driver_id = r.driver_id AND a.vehicle_id = r.vehicle_id), now())
FROM restore_candidates r WHERE r.vehicle_free;

INSERT INTO driver_pauses (id, country_id, driver_id, cause, started_on, ended_on, reason)
SELECT gen_random_uuid(), r.country_id, r.driver_id, 'INACTIVE', (r.deactivated_at AT TIME ZONE r.timezone)::date,
       (r.reactivated_at AT TIME ZONE r.timezone)::date, 'Contrat restauré après mise en inactivité'
FROM restore_candidates r
WHERE r.vehicle_free AND (r.reactivated_at IS NULL OR (r.reactivated_at AT TIME ZONE r.timezone)::date > (r.deactivated_at AT TIME ZONE r.timezone)::date);

INSERT INTO audit_logs (id, actor, action, target, result, target_type, target_id, country_id, details, created_at)
SELECT gen_random_uuid(), 'system', 'CONTRACT_RESTORED', r.driver_name, 'SUCCESS', 'DRIVER', r.driver_id::text, r.country_id,
       'contract ' || r.contract_number || ' reopened after deactivation (ADR-0046)', now()
FROM restore_candidates r WHERE r.vehicle_free;

-- Review the report above, then replace the next line with COMMIT;
ROLLBACK;
