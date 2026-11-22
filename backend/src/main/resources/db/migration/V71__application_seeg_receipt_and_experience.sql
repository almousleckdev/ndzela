-- SEEG utility receipt (proof of address) and years of driving experience on website applications; old rows keep NULL.
ALTER TABLE driver_applications ADD COLUMN seeg_receipt_file VARCHAR(100);
ALTER TABLE driver_applications ADD COLUMN driving_experience_years INTEGER;
