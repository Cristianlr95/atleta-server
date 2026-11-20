\set ON_ERROR_STOP on
BEGIN TRANSACTION READ ONLY;

WITH captain_counts AS (
    SELECT t.id,
           count(*) FILTER (WHERE m.rol = 'CAPITAN') AS captains,
           count(*) FILTER (WHERE m.user_id = t.creador_user_id) AS founder_memberships,
           count(*) FILTER (WHERE m.user_id = t.creador_user_id AND m.rol = 'JUGADOR') AS founder_player_memberships,
           a.atleta_uuid IS NOT NULL AND a.deleted_at IS NULL AS founder_account_available
    FROM public.teams AS t
    LEFT JOIN public.athletes AS a ON a.atleta_uuid = t.creador_user_id
    LEFT JOIN public.team_members AS m ON m.team_id = t.id AND m.activo IS TRUE
    WHERE t.archived IS FALSE
    GROUP BY t.id, a.atleta_uuid, a.deleted_at
), assignment AS (
    SELECT *,
           captains = 0 AND founder_memberships = 1
               AND founder_player_memberships = 1 AND founder_account_available AS can_promote_founder
    FROM captain_counts
)
SELECT count(*) AS active_teams,
       count(*) FILTER (WHERE captains = 0) AS zero_active_captains,
       count(*) FILTER (WHERE captains = 1) AS one_active_captain,
       count(*) FILTER (WHERE captains > 1) AS multiple_active_captains,
       count(*) FILTER (WHERE captains <> 1 AND founder_memberships > 0) AS invalid_with_active_founder,
       count(*) FILTER (WHERE can_promote_founder) AS invalid_with_unambiguous_founder_candidate,
       count(*) FILTER (WHERE captains <> 1 AND NOT can_promote_founder) AS invalid_needing_assignment_review,
       (SELECT count(*) FROM (
           SELECT m.team_id FROM public.team_members m
           JOIN public.teams t ON t.id = m.team_id
           WHERE t.archived AND m.activo AND m.rol = 'CAPITAN'
           GROUP BY m.team_id HAVING count(*) > 1
       ) AS duplicates) AS archived_with_multiple_active_captains
FROM assignment;

COMMIT;
