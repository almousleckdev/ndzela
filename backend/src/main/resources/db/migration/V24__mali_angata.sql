-- V24__mali_angata.sql: Mali's company (Angata), its contract template and its mobile-money operators.
UPDATE countries SET contract_prefix = 'ANG-', contract_template = 'ML-1', legal_name = 'ANGATA', legal_form = 'Société à responsabilité limitée',
    head_office = 'Bamako', signing_city = 'Bamako'
WHERE code = 'ML';

INSERT INTO payment_operators (id, country_id, code, label, sort_order)
SELECT gen_random_uuid(), id, 'ORANGE_MONEY', 'Orange Money', 1 FROM countries WHERE code = 'ML'
ON CONFLICT (country_id, code) DO NOTHING;
INSERT INTO payment_operators (id, country_id, code, label, sort_order)
SELECT gen_random_uuid(), id, 'MOOV_MONEY', 'Moov Money', 2 FROM countries WHERE code = 'ML'
ON CONFLICT (country_id, code) DO NOTHING;
