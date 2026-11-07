ALTER TABLE match_invites
    DROP CONSTRAINT IF EXISTS match_invites_status_check;

ALTER TABLE match_invites
    ADD CONSTRAINT match_invites_status_check
    CHECK (status IN ('PENDIENTE', 'ACEPTADA', 'LISTA_ESPERA', 'RECHAZADA', 'CANCELADA'));
