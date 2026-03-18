-- Add basic indexes for performance (H2 Compatible)
-- Versión: V002
-- Descripción: Índices básicos para mejorar rendimiento

-- Índices para búsquedas frecuentes
CREATE INDEX idx_athletes_email ON athletes(email);
CREATE INDEX idx_player_profiles_trust_score ON player_profiles(trust_score);
CREATE INDEX idx_teams_nombre ON teams(nombre);
CREATE INDEX idx_matches_fecha_hora ON matches(fecha_hora_programada);
CREATE INDEX idx_matches_estado ON matches(estado);
