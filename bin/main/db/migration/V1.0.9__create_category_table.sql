BEGIN;

CREATE TABLE IF NOT EXISTS category
(
    id     UUID         NOT NULL DEFAULT gen_random_uuid(),
    code   VARCHAR(255) NOT NULL UNIQUE,
    CONSTRAINT pk_category_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS category_topic
(
    category_id     UUID    NOT NULL,
    topic_id        UUID    NOT NULL,
    CONSTRAINT pk_category_topic_id PRIMARY KEY (category_id, topic_id),
    CONSTRAINT fk_category_topic_category_id FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_category_topic_topic_id FOREIGN KEY (topic_id) REFERENCES topic (id)
);

COMMIT;