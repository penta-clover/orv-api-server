BEGIN;

ALTER TABLE interview_reservation
    ADD COLUMN reservation_status VARCHAR(10) NOT NULL DEFAULT 'pending';

ALTER TABLE interview_reservation
    ADD CONSTRAINT chk_reservation_status CHECK (reservation_status IN ('pending', 'canceled', 'done'));

COMMIT;