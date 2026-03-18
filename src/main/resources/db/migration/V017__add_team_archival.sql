ALTER TABLE teams
    ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE teams
    ADD COLUMN archived_at TIMESTAMP;

CREATE INDEX idx_teams_archived ON teams (archived);
