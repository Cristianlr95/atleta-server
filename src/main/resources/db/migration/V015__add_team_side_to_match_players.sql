ALTER TABLE match_players
    ADD COLUMN IF NOT EXISTS team_side VARCHAR(16);

ALTER TABLE match_players
    ADD CONSTRAINT chk_match_players_team_side
        CHECK (team_side IS NULL OR team_side IN ('LOCAL', 'VISITA'));

CREATE INDEX IF NOT EXISTS idx_match_players_match_team_side
    ON match_players(match_id, team_side);

UPDATE match_players mp
SET team_side = 'LOCAL'
FROM match_teams mt
WHERE mp.match_id = mt.match_id
  AND mp.team_id = mt.team_id
  AND mt.es_local = true
  AND mp.team_side IS NULL;

UPDATE match_players mp
SET team_side = 'VISITA'
FROM match_teams mt
WHERE mp.match_id = mt.match_id
  AND mp.team_id = mt.team_id
  AND mt.es_local = false
  AND mp.team_side IS NULL;
