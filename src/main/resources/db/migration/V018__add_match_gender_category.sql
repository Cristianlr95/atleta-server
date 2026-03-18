ALTER TABLE matches
    ADD COLUMN categoria_genero VARCHAR(20);

UPDATE matches
SET categoria_genero = 'MIXTO'
WHERE categoria_genero IS NULL;

ALTER TABLE matches
    ALTER COLUMN categoria_genero SET NOT NULL;
