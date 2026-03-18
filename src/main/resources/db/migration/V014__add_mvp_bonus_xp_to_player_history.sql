ALTER TABLE player_history
    ADD COLUMN mvp_bonus_xp INTEGER NOT NULL DEFAULT 0;

ALTER TABLE player_history
    ADD CONSTRAINT chk_player_history_mvp_bonus_xp_non_negative
        CHECK (mvp_bonus_xp >= 0);
