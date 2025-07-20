BEGIN;

CREATE TABLE IF NOT EXISTS interview_reservation
(
    id              UUID        NOT NULL DEFAULT uuid_generate_v4(),
    storyboard_id   UUID        NOT NULL,
    member_id       UUID        NOT NULL,
    scheduled_at    TIMESTAMP   NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_interview_reservation_id PRIMARY KEY (id),
    CONSTRAINT fk_interview_reservation_storyboard_id FOREIGN KEY (storyboard_id) REFERENCES storyboard (id),
    CONSTRAINT fk_interview_reservation_member_id FOREIGN KEY (member_id) REFERENCES member (id)
);

COMMIT;