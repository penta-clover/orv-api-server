ALTER TABLE storyboard ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
CREATE INDEX idx_storyboard_status ON storyboard(status);
