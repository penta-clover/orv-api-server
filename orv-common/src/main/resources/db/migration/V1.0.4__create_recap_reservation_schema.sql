BEGIN;

CREATE TABLE IF NOT EXISTS recap_reservation
(
    id              UUID        NOT NULL DEFAULT uuid_generate_v4(),
    member_id       UUID        NOT NULL,
    video_id        UUID        NOT NULL,
    scheduled_at    TIMESTAMP   NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_recap_reservation_id PRIMARY KEY (id),
    CONSTRAINT fk_recap_reservation_member_id FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_recap_reservation_video_id FOREIGN KEY (video_id) REFERENCES video (id)
);

COMMIT;