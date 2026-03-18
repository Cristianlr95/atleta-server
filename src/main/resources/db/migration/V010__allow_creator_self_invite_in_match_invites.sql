-- Permite auto-invitacion del creador del partido.
-- Antes match_invites exigia requester_user_id <> target_user_id.
-- Para reflejar al creador como participante aceptado, se elimina esa restriccion
-- solo en match_invites.

ALTER TABLE match_invites
DROP CONSTRAINT IF EXISTS ck_match_invites_distinct_users;
