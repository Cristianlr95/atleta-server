-- Base de datos: base estructural para "Finalizar Match + XP/Stats"
-- Paso 1: solo estructura y restricciones (sin lógica de negocio)

-- 1) Idempotencia del historial inmutable por jugador/match
ALTER TABLE player_history
    ADD CONSTRAINT uk_player_history_match_user UNIQUE (match_id, user_id);

CREATE INDEX idx_player_history_user_created_at
    ON player_history (user_id, created_at DESC);

CREATE INDEX idx_player_history_match_created_at
    ON player_history (match_id, created_at DESC);

-- 2) Snapshot/metadata de finalización y validación del match
ALTER TABLE matches
    ADD COLUMN finalized_at TIMESTAMP;

ALTER TABLE matches
    ADD COLUMN validation_status VARCHAR(40) NOT NULL DEFAULT 'PENDING';

ALTER TABLE matches
    ADD COLUMN validation_reason VARCHAR(255);

ALTER TABLE matches
    ADD COLUMN final_score_local INTEGER NOT NULL DEFAULT 0 CHECK (final_score_local >= 0);

ALTER TABLE matches
    ADD COLUMN final_score_away INTEGER NOT NULL DEFAULT 0 CHECK (final_score_away >= 0);

ALTER TABLE matches
    ADD CONSTRAINT chk_matches_validation_status
        CHECK (validation_status IN (
            'PENDING',
            'VALID',
            'INVALID_GPS',
            'INVALID_TIME_WINDOW',
            'INVALID_CONFIRMATION_THRESHOLD',
            'INVALID_STATE'
        ));
