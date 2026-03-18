-- Ajuste de regla de negocio: se elimina validacion por GPS
ALTER TABLE matches
    DROP CONSTRAINT IF EXISTS chk_matches_validation_status;

ALTER TABLE matches
    ADD CONSTRAINT chk_matches_validation_status
        CHECK (validation_status IN (
            'PENDING',
            'VALID',
            'INVALID_TIME_WINDOW',
            'INVALID_CONFIRMATION_THRESHOLD',
            'INVALID_STATE'
        ));
