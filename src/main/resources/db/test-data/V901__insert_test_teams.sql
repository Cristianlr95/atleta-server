-- Datos de prueba para equipos
-- Solo se ejecuta en ambiente de testing

-- Insertar equipos de prueba
INSERT INTO teams (nombre, logo_url, anio_fundacion, creador_user_id, created_at, updated_at, version)
VALUES 
    ('Los Tigres FC', 'https://example.com/logos/tigres.png', 2020, '550e8400-e29b-41d4-a716-446655440001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('Águilas United', 'https://example.com/logos/aguilas.png', 2019, '550e8400-e29b-41d4-a716-446655440002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('Leones del Sur', 'https://example.com/logos/leones.png', 2021, '550e8400-e29b-41d4-a716-446655440003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);