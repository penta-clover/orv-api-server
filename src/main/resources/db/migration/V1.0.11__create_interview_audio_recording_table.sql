CREATE TABLE interview_audio_recording
(
    id            UUID PRIMARY KEY,
    storyboard_id UUID NOT NULL,
    member_id     UUID NOT NULL,
    video_url     TEXT NOT NULL,
    created_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    running_time  INTEGER NOT NULL,
    CONSTRAINT fk_storyboard
        FOREIGN KEY (storyboard_id)
            REFERENCES STORYBOARD (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_member
        FOREIGN KEY (member_id)
            REFERENCES MEMBER (id)
            ON DELETE CASCADE
);
