ALTER TABLE session_payments
    ADD CONSTRAINT session_payments_free_category_check
    CHECK (free_category IS NULL OR free_category IN ('NDZELA_STAFF', 'OTHER', 'FREE_SUNDAY'));
