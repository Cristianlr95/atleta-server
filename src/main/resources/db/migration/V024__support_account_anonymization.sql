ALTER TABLE athletes ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_athletes_deleted_at ON athletes(deleted_at);
