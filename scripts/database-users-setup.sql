-- Scripts SQL para configuración de usuarios y permisos por ambiente
-- Proyecto: Atleta Server
-- Descripción: Configuración de usuarios con permisos mínimos necesarios

-- =============================================================================
-- AMBIENTE DE DESARROLLO
-- =============================================================================

-- Crear usuario para desarrollo (permisos amplios para facilitar desarrollo)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'atleta_dev') THEN
        CREATE ROLE atleta_dev LOGIN PASSWORD 'dev_password_change_me';
    END IF;
END
$$;

-- Permisos para desarrollo
GRANT CONNECT ON DATABASE atleta_dev TO atleta_dev;
GRANT USAGE ON SCHEMA public TO atleta_dev;
GRANT CREATE ON SCHEMA public TO atleta_dev;

-- Permisos sobre tablas existentes y futuras
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO atleta_dev;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO atleta_dev;

-- Permisos sobre secuencias
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO atleta_dev;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO atleta_dev;

-- Permisos para Flyway (necesario para migraciones)
GRANT CREATE ON DATABASE atleta_dev TO atleta_dev;

-- =============================================================================
-- AMBIENTE DE TESTING
-- =============================================================================

-- Crear usuario para testing (similar a desarrollo pero más restrictivo)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'atleta_test') THEN
        CREATE ROLE atleta_test LOGIN PASSWORD 'test_password_change_me';
    END IF;
END
$$;

-- Permisos para testing
GRANT CONNECT ON DATABASE atleta_test TO atleta_test;
GRANT USAGE ON SCHEMA public TO atleta_test;
GRANT CREATE ON SCHEMA public TO atleta_test;

-- Permisos sobre tablas (necesario para tests de integración)
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO atleta_test;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO atleta_test;

-- Permisos sobre secuencias
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO atleta_test;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO atleta_test;

-- Permisos para Flyway en testing
GRANT CREATE ON DATABASE atleta_test TO atleta_test;

-- =============================================================================
-- AMBIENTE DE STAGING
-- =============================================================================

-- Crear usuario para staging (permisos de aplicación sin administración)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'atleta_staging') THEN
        CREATE ROLE atleta_staging LOGIN PASSWORD 'CHANGE_ME_STAGING_PASSWORD';
    END IF;
END
$$;

-- Permisos básicos para staging
GRANT CONNECT ON DATABASE atleta_staging TO atleta_staging;
GRANT USAGE ON SCHEMA public TO atleta_staging;

-- Permisos sobre tablas (solo operaciones de aplicación)
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO atleta_staging;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO atleta_staging;

-- Permisos sobre secuencias
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO atleta_staging;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO atleta_staging;

-- Usuario separado para migraciones en staging
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'atleta_staging_migration') THEN
        CREATE ROLE atleta_staging_migration LOGIN PASSWORD 'CHANGE_ME_STAGING_MIGRATION_PASSWORD';
    END IF;
END
$$;

-- Permisos para migraciones en staging
GRANT CONNECT ON DATABASE atleta_staging TO atleta_staging_migration;
GRANT USAGE ON SCHEMA public TO atleta_staging_migration;
GRANT CREATE ON SCHEMA public TO atleta_staging_migration;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO atleta_staging_migration;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO atleta_staging_migration;

-- =============================================================================
-- AMBIENTE DE PRODUCCIÓN
-- =============================================================================

-- Crear usuario para aplicación en producción (permisos mínimos)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'atleta_prod_app') THEN
        CREATE ROLE atleta_prod_app LOGIN PASSWORD 'CHANGE_ME_PRODUCTION_APP_PASSWORD';
    END IF;
END
$$;

-- Permisos mínimos para aplicación en producción
GRANT CONNECT ON DATABASE atleta_production TO atleta_prod_app;
GRANT USAGE ON SCHEMA public TO atleta_prod_app;

-- Solo operaciones CRUD básicas (sin CREATE/DROP)
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO atleta_prod_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO atleta_prod_app;

-- Permisos sobre secuencias
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO atleta_prod_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO atleta_prod_app;

-- Usuario separado para migraciones en producción
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'atleta_prod_migration') THEN
        CREATE ROLE atleta_prod_migration LOGIN PASSWORD 'CHANGE_ME_PRODUCTION_MIGRATION_PASSWORD';
    END IF;
END
$$;

-- Permisos para migraciones en producción (solo durante despliegues)
GRANT CONNECT ON DATABASE atleta_production TO atleta_prod_migration;
GRANT USAGE ON SCHEMA public TO atleta_prod_migration;
GRANT CREATE ON SCHEMA public TO atleta_prod_migration;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO atleta_prod_migration;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO atleta_prod_migration;

-- Usuario de solo lectura para monitoreo y reportes
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'atleta_prod_readonly') THEN
        CREATE ROLE atleta_prod_readonly LOGIN PASSWORD 'CHANGE_ME_PRODUCTION_READONLY_PASSWORD';
    END IF;
END
$$;

-- Permisos de solo lectura
GRANT CONNECT ON DATABASE atleta_production TO atleta_prod_readonly;
GRANT USAGE ON SCHEMA public TO atleta_prod_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO atleta_prod_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO atleta_prod_readonly;

-- =============================================================================
-- CONFIGURACIONES DE SEGURIDAD ADICIONALES
-- =============================================================================

-- Revocar permisos públicos por defecto
REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE CREATE ON DATABASE atleta_production FROM PUBLIC;
REVOKE CREATE ON DATABASE atleta_staging FROM PUBLIC;

-- Configurar límites de conexión por usuario
ALTER ROLE atleta_dev CONNECTION LIMIT 10;
ALTER ROLE atleta_test CONNECTION LIMIT 5;
ALTER ROLE atleta_staging CONNECTION LIMIT 20;
ALTER ROLE atleta_prod_app CONNECTION LIMIT 50;
ALTER ROLE atleta_prod_migration CONNECTION LIMIT 2;
ALTER ROLE atleta_prod_readonly CONNECTION LIMIT 10;

-- Configurar timeouts de sesión
ALTER ROLE atleta_prod_app SET statement_timeout = '60s';
ALTER ROLE atleta_prod_app SET idle_in_transaction_session_timeout = '10min';
ALTER ROLE atleta_staging SET statement_timeout = '120s';
ALTER ROLE atleta_staging SET idle_in_transaction_session_timeout = '15min';

-- =============================================================================
-- VERIFICACIÓN DE PERMISOS
-- =============================================================================

-- Script para verificar permisos de un usuario
-- Ejecutar: SELECT * FROM check_user_permissions('nombre_usuario');

CREATE OR REPLACE FUNCTION check_user_permissions(username TEXT)
RETURNS TABLE (
    object_type TEXT,
    object_name TEXT,
    privilege_type TEXT,
    is_grantable BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        'table'::TEXT as object_type,
        schemaname || '.' || tablename as object_name,
        privilege_type::TEXT,
        is_grantable::BOOLEAN
    FROM information_schema.table_privileges 
    WHERE grantee = username
    
    UNION ALL
    
    SELECT 
        'sequence'::TEXT as object_type,
        sequence_schema || '.' || sequence_name as object_name,
        privilege_type::TEXT,
        is_grantable::BOOLEAN
    FROM information_schema.usage_privileges 
    WHERE grantee = username AND object_type = 'SEQUENCE'
    
    UNION ALL
    
    SELECT 
        'schema'::TEXT as object_type,
        schema_name as object_name,
        privilege_type::TEXT,
        is_grantable::BOOLEAN
    FROM information_schema.schema_privileges 
    WHERE grantee = username
    
    ORDER BY object_type, object_name, privilege_type;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- COMENTARIOS Y DOCUMENTACIÓN
-- =============================================================================

COMMENT ON ROLE atleta_dev IS 'Usuario para ambiente de desarrollo - permisos amplios';
COMMENT ON ROLE atleta_test IS 'Usuario para ambiente de testing - permisos de desarrollo';
COMMENT ON ROLE atleta_staging IS 'Usuario de aplicación para staging - permisos limitados';
COMMENT ON ROLE atleta_staging_migration IS 'Usuario para migraciones en staging';
COMMENT ON ROLE atleta_prod_app IS 'Usuario de aplicación para producción - permisos mínimos';
COMMENT ON ROLE atleta_prod_migration IS 'Usuario para migraciones en producción - uso temporal';
COMMENT ON ROLE atleta_prod_readonly IS 'Usuario de solo lectura para monitoreo y reportes';