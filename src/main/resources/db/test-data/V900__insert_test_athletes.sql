-- Datos de prueba para atletas y perfiles
-- Solo se ejecuta en ambiente de testing

-- Insertar atletas de prueba
INSERT INTO athletes (atleta_uuid, email, password_hash, nombre, created_at, updated_at, version)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440001', 'juan.perez@test.com', '$2a$10$dummyHashForTesting1', 'Juan Pérez', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('550e8400-e29b-41d4-a716-446655440002', 'maria.garcia@test.com', '$2a$10$dummyHashForTesting2', 'María García', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('550e8400-e29b-41d4-a716-446655440003', 'carlos.lopez@test.com', '$2a$10$dummyHashForTesting3', 'Carlos López', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('550e8400-e29b-41d4-a716-446655440004', 'ana.martinez@test.com', '$2a$10$dummyHashForTesting4', 'Ana Martínez', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('550e8400-e29b-41d4-a716-446655440005', 'diego.rodriguez@test.com', '$2a$10$dummyHashForTesting5', 'Diego Rodríguez', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Insertar perfiles de jugador de prueba
INSERT INTO player_profiles (atleta_uuid, alias, trust_score, created_at, updated_at, version)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440001', 'JuanP', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('550e8400-e29b-41d4-a716-446655440002', 'MariaG', 95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('550e8400-e29b-41d4-a716-446655440003', 'CarlosL', 105, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('550e8400-e29b-41d4-a716-446655440004', 'AnaM', 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('550e8400-e29b-41d4-a716-446655440005', 'DiegoR', 110, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);