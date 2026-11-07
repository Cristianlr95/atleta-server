ALTER TABLE matches
    ADD COLUMN creation_idempotency_key VARCHAR(100);

ALTER TABLE matches
    ADD CONSTRAINT uq_matches_creator_idempotency
    UNIQUE (creador_user_id, creation_idempotency_key);
