
BEGIN;

ALTER TABLE interview_reservation
ALTER COLUMN scheduled_at TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE interview_reservation
ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE;

COMMIT;
