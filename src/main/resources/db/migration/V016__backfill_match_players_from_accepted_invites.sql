WITH default_position AS (
    SELECT id AS position_id
    FROM positions
    ORDER BY id
    LIMIT 1
),
accepted_invites AS (
    SELECT
        mi.match_id,
        mi.target_user_id AS user_id,
        COALESCE(
            mi.team_id,
            (
                SELECT mt.team_id
                FROM match_teams mt
                WHERE mt.match_id = mi.match_id
                  AND mt.es_local = true
                LIMIT 1
            ),
            (
                SELECT mt.team_id
                FROM match_teams mt
                WHERE mt.match_id = mi.match_id
                LIMIT 1
            )
        ) AS team_id,
        COALESCE(
            (
                SELECT pp.position_id
                FROM player_positions pp
                WHERE pp.player_id = mi.target_user_id
                ORDER BY pp.prioridad ASC
                LIMIT 1
            ),
            (SELECT position_id FROM default_position)
        ) AS position_id
    FROM match_invites mi
    WHERE mi.status = 'ACEPTADA'
)
INSERT INTO match_players (
    match_id,
    team_id,
    user_id,
    position_id,
    rol,
    confirmado,
    team_side,
    created_at,
    updated_at,
    version
)
SELECT
    ai.match_id,
    ai.team_id,
    ai.user_id,
    ai.position_id,
    'JUGADOR',
    TRUE,
    CASE
        WHEN mt.es_local = TRUE THEN 'LOCAL'
        WHEN mt.es_local = FALSE THEN 'VISITA'
        ELSE NULL
    END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM accepted_invites ai
LEFT JOIN match_teams mt
    ON mt.match_id = ai.match_id
   AND mt.team_id = ai.team_id
WHERE ai.team_id IS NOT NULL
  AND ai.position_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM match_players mp_existing
      WHERE mp_existing.match_id = ai.match_id
        AND mp_existing.user_id = ai.user_id
  );

UPDATE match_players mp
SET confirmado = TRUE,
    updated_at = CURRENT_TIMESTAMP
FROM match_invites mi
WHERE mi.match_id = mp.match_id
  AND mi.target_user_id = mp.user_id
  AND mi.status = 'ACEPTADA'
  AND mp.confirmado = FALSE;

UPDATE match_players mp
SET team_side = CASE
        WHEN mt.es_local = TRUE THEN 'LOCAL'
        WHEN mt.es_local = FALSE THEN 'VISITA'
        ELSE mp.team_side
    END,
    updated_at = CURRENT_TIMESTAMP
FROM match_teams mt
WHERE mt.match_id = mp.match_id
  AND mt.team_id = mp.team_id
  AND mp.team_side IS NULL;
