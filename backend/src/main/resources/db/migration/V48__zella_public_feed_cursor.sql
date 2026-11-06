-- Where the feed of the sessions of customers who are not NDZela drivers resumes (kept apart from the drivers' feed cursor)
ALTER TABLE zella_sync_state ADD COLUMN public_sync_cursor TEXT;
