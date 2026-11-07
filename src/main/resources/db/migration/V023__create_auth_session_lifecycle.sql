CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY,
    athlete_uuid UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_refresh_sessions_athlete FOREIGN KEY (athlete_uuid) REFERENCES athletes(atleta_uuid) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_sessions_athlete_active ON refresh_sessions(athlete_uuid, revoked_at, expires_at);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    athlete_uuid UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_password_reset_tokens_athlete FOREIGN KEY (athlete_uuid) REFERENCES athletes(atleta_uuid) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_athlete_active ON password_reset_tokens(athlete_uuid, used_at, expires_at);
