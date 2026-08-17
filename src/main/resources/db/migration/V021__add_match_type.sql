ALTER TABLE matches
    ADD COLUMN match_type VARCHAR(20);

UPDATE matches
SET match_type = 'FRIENDLY'
WHERE match_type IS NULL;

ALTER TABLE matches
    ALTER COLUMN match_type SET NOT NULL;
