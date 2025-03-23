BEGIN;

ALTER TABLE storyboard_preview
    ADD COLUMN question_count INTEGER NOT NULL DEFAULT 0;

COMMIT;