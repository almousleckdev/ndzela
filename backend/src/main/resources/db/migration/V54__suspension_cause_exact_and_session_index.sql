-- V54__suspension_cause_exact_and_session_index.sql: only the two reasons the arrears job writes make a suspension "for arrears" (V51 also took any reason a person typed that starts with "Impayés"), and the lookups of one account's charges get an index.
UPDATE drivers SET suspension_cause = 'MANUAL'
WHERE suspension_cause = 'ARREARS' AND status = 'SUSPENDED'
  AND status_reason NOT LIKE 'Impayés : trois jours de loyer non réglés ou plus (suspension automatique)%';

CREATE INDEX idx_charging_sessions_account_start ON charging_sessions (country_id, id_tag, started_at);
