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

CREATE TABLE IF NOT EXISTS storyboard
(
    id             UUID         NOT NULL DEFAULT uuid_generate_v4(),
    title          VARCHAR(255) NOT NULL,
    start_scene_id UUID,
    CONSTRAINT pk_storyboard_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS term_agreement
(
    id         UUID        NOT NULL DEFAULT uuid_generate_v4(),
    member_id  UUID        NOT NULL,
    term       VARCHAR(50) NOT NULL,
    value      VARCHAR(50) NOT NULL,
    agreed_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address inet,
    CONSTRAINT pk_member_agreement_id PRIMARY KEY (id),
    CONSTRAINT fk_member_agreement_member_id FOREIGN KEY (member_id) REFERENCES member (id)
);

CREATE TABLE IF NOT EXISTS storyboard_preview
(
    storyboard_id UUID   NOT NULL,
    examples      TEXT[] NOT NULL,
    CONSTRAINT pk_storyboard_preview PRIMARY KEY (storyboard_id),
    CONSTRAINT fk_storyboard_preview_storyboard FOREIGN KEY (storyboard_id)
        REFERENCES storyboard (id) ON DELETE CASCADE
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
    id            UUID        NOT NULL DEFAULT uuid_generate_v4(),
    storyboard_id UUID        NOT NULL,
    member_id     UUID        NOT NULL,
    video_url     TEXT        NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    thumbnail_url TEXT        NOT NULL,
    title         VARCHAR(50) NULL,
    CONSTRAINT pk_video_id PRIMARY KEY (id),
    CONSTRAINT fk_video_storyboard_id FOREIGN KEY (storyboard_id) REFERENCES storyboard (id),
    CONSTRAINT fk_video_member_id FOREIGN KEY (member_id) REFERENCES member (id)
);

CREATE TABLE IF NOT EXISTS topic
(
    id          UUID        NOT NULL DEFAULT uuid_generate_v4(),
    name        VARCHAR(30) NOT NULL,
    description TEXT,
    CONSTRAINT pk_topic_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS storyboard_topic
(
    storyboard_id UUID NOT NULL,
    topic_id      UUID NOT NULL,
    CONSTRAINT pk_storyboard_topic_storyboard_id PRIMARY KEY (storyboard_id, topic_id),
    CONSTRAINT fk_storyboard_topic_storyboard FOREIGN KEY (storyboard_id)
        REFERENCES storyboard (id) ON DELETE CASCADE,
    CONSTRAINT fk_storyboard_topic_topic FOREIGN KEY (topic_id)
        REFERENCES topic (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS storyboard_usage_history
(
    id            UUID        NOT NULL DEFAULT uuid_generate_v4(),
    storyboard_id UUID        NOT NULL,
    member_id     UUID        NOT NULL,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        VARCHAR(30) NOT NULL,
    CONSTRAINT pk_storyboard_usage_history_id PRIMARY KEY (id),
    CONSTRAINT fk_storyboard_usage_history_storyboard_id FOREIGN KEY (storyboard_id) REFERENCES storyboard (id),
    CONSTRAINT fk_storyboard_usage_history_member_id FOREIGN KEY (member_id) REFERENCES member (id)
);

ALTER TABLE video
    ADD COLUMN running_time INTEGER NOT NULL DEFAULT 0;
