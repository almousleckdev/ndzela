-- V46__id_tags_by_customer.sql: Zella gives a customer's idTag only after the first charge, so the customer id is the stable key.
ALTER TABLE id_tags ALTER COLUMN id_tag DROP NOT NULL;
ALTER TABLE id_tags DROP CONSTRAINT uq_id_tags_tag;
CREATE UNIQUE INDEX ux_id_tags_tag ON id_tags (country_id, id_tag) WHERE id_tag IS NOT NULL;
CREATE UNIQUE INDEX ux_id_tags_customer ON id_tags (country_id, customer_id);
-- True once the Zella app has been told about the link (its payment feed only serves linked customers)
ALTER TABLE id_tags ADD COLUMN zella_notified BOOLEAN NOT NULL DEFAULT FALSE;
