ALTER TABLE storyboard ADD COLUMN participation_count INT NOT NULL DEFAULT 0;
ALTER TABLE storyboard ADD COLUMN max_participation_limit INT DEFAULT NULL;
