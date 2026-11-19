-- V69: a driver who is inactive, or has no car for a while, keeps his contract; the rent is not charged during the pause (ADR-0046).
CREATE TABLE driver_pauses (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    cause VARCHAR(20) NOT NULL,
    started_on DATE NOT NULL,
    ended_on DATE,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_driver_pauses_dates CHECK (ended_on IS NULL OR ended_on > started_on)
);

CREATE UNIQUE INDEX ux_driver_pauses_open ON driver_pauses (driver_id, cause) WHERE ended_on IS NULL;
CREATE INDEX idx_driver_pauses_driver ON driver_pauses (driver_id);
CREATE INDEX idx_driver_pauses_country ON driver_pauses (country_id);

-- A contract may be without a car (the car was taken, the driver is blocked...) and takes the next car given to the driver.
ALTER TABLE contracts ALTER COLUMN vehicle_id DROP NOT NULL;
