CREATE TABLE IF NOT EXISTS member
(
    id                UUID         NOT NULL DEFAULT uuid_generate_v4(),
    nickname          VARCHAR(8)   NOT NULL,
    provider          VARCHAR(50)  NOT NULL,
    social_id         VARCHAR(100) NOT NULL,
    email             VARCHAR(255),
    profile_image_url TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    phone_number      VARCHAR(20),
    gender            VARCHAR(100),
    name              VARCHAR(255),
    birthday          DATE,
    CONSTRAINT pk_member_id PRIMARY KEY (id),
    CONSTRAINT uq_provider_social_id UNIQUE (provider, social_id)
);

CREATE TABLE IF NOT EXISTS member_agreement
(
    id         UUID        NOT NULL DEFAULT uuid_generate_v4(),
    member_id  UUID        NOT NULL,
    term       VARCHAR(50) NOT NULL,
    value      VARCHAR(50) NOT NULL,
    agreed_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address inet        NOT NULL,
    CONSTRAINT pk_member_agreement_id PRIMARY KEY (id),
    CONSTRAINT fk_member_agreement_member_id FOREIGN KEY (member_id) REFERENCES member (id)
);

CREATE TABLE IF NOT EXISTS storyboard
(
    id             UUID         NOT NULL DEFAULT uuid_generate_v4(),
    title          VARCHAR(255) NOT NULL,
    start_scene_id UUID,
    CONSTRAINT pk_storyboard_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS scene
(
    id            UUID         NOT NULL DEFAULT uuid_generate_v4(),
    storyboard_id UUID         NOT NULL,
    name          VARCHAR(255) NOT NULL,
    scene_type    VARCHAR(255) NOT NULL,
    content       json         NOT NULL
        CHECK (json_typeof(content) = 'object'),
    CONSTRAINT pk_scene_id PRIMARY KEY (id),
    CONSTRAINT fk_scene_storyboard_id FOREIGN KEY (storyboard_id) REFERENCES storyboard (id)
);

CREATE TABLE IF NOT EXISTS video
(
    id               UUID        NOT NULL DEFAULT uuid_generate_v4(),
    storyboard_id    UUID        NOT NULL,
    member_id        UUID        NOT NULL,
    video_url        TEXT        NOT NULL,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    thumbnail_url    TEXT        NOT NULL,
    title            VARCHAR(50) NULL,
    CONSTRAINT pk_video_id PRIMARY KEY (id),
    CONSTRAINT fk_video_storyboard_id FOREIGN KEY (storyboard_id) REFERENCES storyboard (id),
    CONSTRAINT fk_video_member_id FOREIGN KEY (member_id) REFERENCES member (id)
);

ALTER TABLE video
    ADD COLUMN running_time INTEGER NOT NULL DEFAULT 0;

-- 테스트용 스토리보드와 scene 추가
BEGIN;

INSERT INTO storyboard (id, title, start_scene_id)
VALUES ('e5895e70-7713-4a35-b12f-2521af77524b', '테스트 스토리보드', '50c4dfc2-8bec-4d77-849f-57462d50d393');


INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('95f081ce-baa4-418d-8398-db77a764227d', 'e5895e70-7713-4a35-b12f-2521af77524b', '테스트 씬', 'QUESTION', '{
  "question": "테스트 질문",
  "next": "1cf0980f-baa4-418d-8398-db7137529002"
}'::json),
       ('8ee9980f-81a8-438c-8b64-3fd413752900', 'e5895e70-7713-4a35-b12f-2521af77524b', '테스트 씬', 'QUESTION', '{
         "question": "살아오며 가장 기뻤던 순간은 언제인가요?",
         "next": "95f081ce-baa4-418d-8398-db77a764227d"
       }'::json),
       ('50c4dfc2-8bec-4d77-849f-57462d50d393', 'e5895e70-7713-4a35-b12f-2521af77524b', '테스트 씬', 'QUESTION', '{
         "question": "당신에게 가장 소중한 것은 무엇인가요?",
         "next": "8ee9980f-81a8-438c-8b64-3fd413752900"
       }'::json),
       ('1cf0980f-baa4-418d-8398-db7137529002', 'e5895e70-7713-4a35-b12f-2521af77524b', '종료 씬', 'END', '{}'::json);

COMMIT;