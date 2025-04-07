BEGIN;

CREATE TABLE IF NOT EXISTS role (
    id         UUID             NOT NULL DEFAULT gen_random_uuid(),
    name       VARCHAR(255)     NOT NULL,
    CONSTRAINT pk_role_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS member_role (
    member_id   UUID         NOT NULL,
    role_id     UUID         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_member_role_id PRIMARY KEY (member_id, role_id),
    CONSTRAINT fk_member_role_member_id FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_member_role_role_id FOREIGN KEY (role_id) REFERENCES role (id)
);

COMMIT;