-- Items can be either borrowable (taken out of the lab, collect/return lifecycle)
-- or lab-only (used in the lab at an instructor-confirmed time).
-- Existing rows default to BORROWABLE to preserve current behaviour.

ALTER TABLE items
    ADD COLUMN IF NOT EXISTS usage_type VARCHAR(20) NOT NULL DEFAULT 'BORROWABLE';

CREATE INDEX IF NOT EXISTS idx_items_usage_type ON items(usage_type);
