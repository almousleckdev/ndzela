-- V52__zella_command.sql: what NDZela must tell the Zella app about a suspended or restored driver, kept until Zella has it.
CREATE TABLE zella_commands (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    customer_id VARCHAR(64) NOT NULL,
    type VARCHAR(10) NOT NULL,
    owed_minor BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(10) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_zella_commands_type CHECK (type IN ('SUSPEND', 'RESTORE')),
    CONSTRAINT chk_zella_commands_status CHECK (status IN ('PENDING', 'SENT', 'CANCELLED'))
);
CREATE INDEX idx_zella_commands_due ON zella_commands (status, next_attempt_at);
CREATE INDEX idx_zella_commands_driver ON zella_commands (driver_id, created_at);
