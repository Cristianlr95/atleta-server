-- Preserve rating_history as an audit of the values recorded under the old rules.
UPDATE player_ratings SET current_rating = 100.00 WHERE current_rating > 100.00;

ALTER TABLE player_ratings ADD CONSTRAINT ck_player_ratings_max_100 CHECK (current_rating <= 100.00);
