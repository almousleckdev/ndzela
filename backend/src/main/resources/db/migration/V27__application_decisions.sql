-- V27__application_decisions.sql: the answer given to a candidate: an interview date when accepted, a reason when refused.
ALTER TABLE driver_applications ADD COLUMN interview_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE driver_applications ADD COLUMN candidate_message VARCHAR(500);
