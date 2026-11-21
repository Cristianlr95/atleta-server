ALTER TABLE team_members
    ADD COLUMN es_dt BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE team_members SET es_dt = TRUE WHERE rol = 'DT';
