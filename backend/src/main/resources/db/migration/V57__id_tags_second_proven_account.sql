-- V57__id_tags_second_proven_account.sql: a driver can hold a second linked charging account, the one whose e-mail Google or Apple proved. The one-linked-account rule is now kept by the application for every other case.
DROP INDEX ux_id_tags_linked_driver;
CREATE INDEX idx_id_tags_linked_driver ON id_tags (driver_id) WHERE status = 'LINKED';
