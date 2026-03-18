-- Ensure DT exists as a playable position in the catalog.
INSERT INTO positions (nombre)
SELECT 'DT'
WHERE NOT EXISTS (
    SELECT 1
    FROM positions
    WHERE LOWER(nombre) IN ('dt', 'director tecnico', 'entrenador', 'coach')
);

