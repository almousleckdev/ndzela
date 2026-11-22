-- V74: the free charging window (weekday, first moment, end) becomes a setting of each country, edited in the dashboards. No row means the default: Sunday, the whole day.
CREATE TABLE free_charging_window (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL UNIQUE REFERENCES countries(id),
    day_of_week VARCHAR(9) NOT NULL,
    from_time TIME NOT NULL,
    until_time TIME,
    updated_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_free_window_day CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT chk_free_window_order CHECK (until_time IS NULL OR from_time < until_time)
);
