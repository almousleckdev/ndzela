-- V41__vehicle_images.sql
-- One or more photos per vehicle (private, served through the backend, like driver documents).
CREATE TABLE vehicle_images (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_vehicle_images_vehicle ON vehicle_images (vehicle_id);
CREATE INDEX idx_vehicle_images_country ON vehicle_images (country_id);
