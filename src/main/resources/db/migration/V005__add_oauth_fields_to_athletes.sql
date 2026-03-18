-- Agregar campos para autenticación OAuth2 (Google)
-- Permite que los usuarios se registren con Google o localmente

ALTER TABLE athletes
ADD COLUMN auth_provider VARCHAR(20) DEFAULT 'LOCAL' NOT NULL;

ALTER TABLE athletes
ADD COLUMN google_id VARCHAR(255) UNIQUE;

ALTER TABLE athletes
ADD COLUMN picture_url VARCHAR(500);

-- Hacer que password_hash sea nullable para usuarios de Google
ALTER TABLE athletes
ALTER COLUMN password_hash DROP NOT NULL;

-- Índice para búsqueda rápida por Google ID
CREATE INDEX idx_athletes_google_id ON athletes(google_id);

-- Índice para búsqueda por proveedor
CREATE INDEX idx_athletes_auth_provider ON athletes(auth_provider);

-- Comentarios
COMMENT ON COLUMN athletes.auth_provider IS 'Proveedor de autenticación: LOCAL o GOOGLE';
COMMENT ON COLUMN athletes.google_id IS 'ID único de Google (sub claim del token)';
COMMENT ON COLUMN athletes.picture_url IS 'URL de la foto de perfil de Google';
