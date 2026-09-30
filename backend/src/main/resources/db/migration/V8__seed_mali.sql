-- V8__seed_mali.sql
INSERT INTO countries (id, code, name, currency_code, phone_code, timezone, default_language)
VALUES (gen_random_uuid(), 'ML', 'Mali', 'XOF', '+223', 'Africa/Bamako', 'fr')
ON CONFLICT (code) DO NOTHING;
