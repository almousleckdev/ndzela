-- Test-only table used to prove the automatic country isolation (see TenantMechanismTest). Never part of the real schema.
CREATE TABLE tenant_test_items (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country_id UUID NOT NULL REFERENCES countries(id)
);
CREATE INDEX idx_tenant_test_items_country ON tenant_test_items (country_id);
