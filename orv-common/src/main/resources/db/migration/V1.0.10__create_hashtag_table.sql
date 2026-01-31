BEGIN;

CREATE TABLE IF NOT EXISTS hashtag
(
    id    UUID         NOT NULL DEFAULT gen_random_uuid(),
    name  VARCHAR(255) NOT NULL,
    color VARCHAR(7)   NOT NULL,
    CONSTRAINT pk_hashtag_id PRIMARY KEY (id),
    CONSTRAINT color_hex_constraint CHECK (color ~* '^#[a-f0-9]{6}$')
);

CREATE TABLE IF NOT EXISTS hashtag_topic
(
    hashtag_id UUID NOT NULL,
    topic_id   UUID NOT NULL,
    CONSTRAINT pk_hashtag_topic_id PRIMARY KEY (hashtag_id, topic_id),
    CONSTRAINT fk_hashtag_topic_hashtag_id FOREIGN KEY (hashtag_id) REFERENCES hashtag (id),
    CONSTRAINT fk_hashtag_topic_topic_id FOREIGN KEY (topic_id) REFERENCES topic (id)
);

COMMIT;