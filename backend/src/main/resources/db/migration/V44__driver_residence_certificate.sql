-- V44__driver_residence_certificate.sql
-- A fourth optional document per driver: the certificate of residence.
ALTER TABLE drivers ADD COLUMN residence_certificate_file VARCHAR(120);
