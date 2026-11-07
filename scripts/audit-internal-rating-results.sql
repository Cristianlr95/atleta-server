-- PostgreSQL: aggregated diagnostic only; run against a disposable copy with a read-only user.
-- A mismatch is a candidate for review, not proof that recalculation is safe.
BEGIN TRANSACTION READ ONLY;

WITH candidates AS (
    SELECT rh.id, rh.match_id, pr.player_profile_id,
           CASE
             WHEN mp.user_id IS NULL OR mp.team_side IS NULL THEN NULL
             WHEN m.final_score_local = m.final_score_away THEN 'EMPATE'
             WHEN (mp.team_side = 'LOCAL' AND m.final_score_local > m.final_score_away)
               OR (mp.team_side = 'VISITA' AND m.final_score_away > m.final_score_local) THEN 'GANADO'
             ELSE 'PERDIDO'
           END AS expected_result,
           rh.match_result
    FROM rating_history rh
    JOIN player_ratings pr ON pr.id = rh.player_rating_id
    JOIN matches m ON m.id = rh.match_id
    LEFT JOIN match_players mp ON mp.match_id = m.id AND mp.user_id = pr.player_profile_id
    WHERE m.estado = 'FINALIZADO' AND m.match_type = 'INTERNAL'
      AND rh.rotative_goalkeeper_mode = FALSE
)
SELECT COUNT(*) AS registros_examinados,
       COUNT(*) FILTER (WHERE expected_result IS NULL) AS resultado_indeterminable,
       COUNT(*) FILTER (WHERE expected_result IS NOT NULL AND expected_result <> match_result) AS registros_distintos,
       COUNT(DISTINCT match_id) FILTER (WHERE expected_result IS NOT NULL AND expected_result <> match_result) AS partidos_con_diferencias,
       COUNT(DISTINCT player_profile_id) FILTER (WHERE expected_result IS NOT NULL AND expected_result <> match_result) AS jugadores_con_diferencias
FROM candidates;

ROLLBACK;
