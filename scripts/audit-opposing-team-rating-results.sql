-- PostgreSQL: aggregated diagnostic only; use a disposable copy and a read-only user.
BEGIN TRANSACTION READ ONLY;

WITH teams AS (
    SELECT match_id,
           COUNT(*) AS team_count,
           MAX(team_id) FILTER (WHERE es_local) AS local_id,
           MAX(team_id) FILTER (WHERE NOT es_local) AS away_id,
           MAX(goles) FILTER (WHERE es_local) AS local_goals,
           MAX(goles) FILTER (WHERE NOT es_local) AS away_goals
    FROM match_teams GROUP BY match_id
), candidates AS (
    SELECT rh.match_id, pr.player_profile_id, rh.match_result,
           CASE
             WHEN t.team_count <> 2 OR t.local_id IS NULL OR t.away_id IS NULL
               OR mp.team_id IS NULL OR mp.team_id NOT IN (t.local_id, t.away_id) THEN NULL
             WHEN t.local_goals = t.away_goals THEN 'EMPATE'
             WHEN (mp.team_id = t.local_id AND t.local_goals > t.away_goals)
               OR (mp.team_id = t.away_id AND t.away_goals > t.local_goals) THEN 'GANADO'
             ELSE 'PERDIDO'
           END AS expected_result
    FROM rating_history rh
    JOIN player_ratings pr ON pr.id = rh.player_rating_id
    JOIN matches m ON m.id = rh.match_id
    LEFT JOIN match_players mp ON mp.match_id = m.id AND mp.user_id = pr.player_profile_id
    LEFT JOIN teams t ON t.match_id = m.id
    WHERE m.estado = 'FINALIZADO' AND m.match_type IN ('FRIENDLY', 'POINTS')
      AND rh.rotative_goalkeeper_mode = FALSE
)
SELECT COUNT(*) AS registros_examinados,
       COUNT(*) FILTER (WHERE expected_result IS NULL) AS resultado_indeterminable,
       COUNT(*) FILTER (WHERE expected_result IS NOT NULL AND expected_result <> match_result) AS registros_distintos,
       COUNT(DISTINCT match_id) FILTER (WHERE expected_result IS NOT NULL AND expected_result <> match_result) AS partidos_con_diferencias,
       COUNT(DISTINCT player_profile_id) FILTER (WHERE expected_result IS NOT NULL AND expected_result <> match_result) AS jugadores_con_diferencias
FROM candidates;

ROLLBACK;
