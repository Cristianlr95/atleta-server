# Guía de Seguridad de Base de Datos - Proyecto Atleta

## Visión General

Este documento describe la estrategia de seguridad de base de datos implementada para el proyecto Atleta, incluyendo usuarios, permisos, y configuraciones de seguridad por ambiente.

## Principios de Seguridad

### 1. Principio de Menor Privilegio
Cada usuario tiene únicamente los permisos mínimos necesarios para su función específica.

### 2. Separación de Responsabilidades
- **Usuarios de aplicación**: Solo operaciones CRUD
- **Usuarios de migración**: Solo durante despliegues
- **Usuarios de monitoreo**: Solo lectura

### 3. Defensa en Profundidad
- Múltiples capas de seguridad
- Validación a nivel de aplicación y base de datos
- Monitoreo y auditoría continua

## Usuarios por Ambiente

### Desarrollo (atleta_dev)

**Propósito**: Facilitar el desarrollo local con permisos amplios

**Permisos**:
- Conexión a base de datos
- CREATE, DROP, ALTER en esquemas
- SELECT, INSERT, UPDATE, DELETE en todas las tablas
- Ejecución de migraciones Flyway
- Acceso a secuencias

**Configuración**:
```sql
-- Usuario: atleta_dev
-- Password: dev_password (cambiar en configuración local)
-- Connection Limit: 10
-- Timeouts: Sin restricciones especiales
```

**Variables de Entorno**:
```bash
DB_USERNAME=atleta_dev
DB_PASSWORD=your_dev_password
```

### Testing (atleta_test)

**Propósito**: Ejecutar tests de integración con Testcontainers

**Permisos**:
- Similar a desarrollo pero en contenedor aislado
- Permisos para crear/eliminar datos de prueba
- Ejecución de migraciones de test

**Configuración**:
```sql
-- Usuario: atleta_test
-- Password: test_password (manejado por Testcontainers)
-- Connection Limit: 5
-- Timeouts: Sin restricciones especiales
```

### Staging (atleta_staging + atleta_staging_migration)

**Propósito**: Ambiente de pre-producción con seguridad intermedia

#### Usuario de Aplicación (atleta_staging)
**Permisos**:
- Conexión a base de datos
- SELECT, INSERT, UPDATE, DELETE en tablas de aplicación
- Acceso a secuencias
- **NO** permisos de DDL (CREATE, DROP, ALTER)

#### Usuario de Migración (atleta_staging_migration)
**Permisos**:
- Usado solo durante despliegues
- Permisos de DDL para migraciones
- Acceso temporal y controlado

**Configuración**:
```sql
-- Usuario App: atleta_staging
-- Connection Limit: 20
-- Statement Timeout: 120s
-- Idle Timeout: 15min

-- Usuario Migration: atleta_staging_migration
-- Connection Limit: 2
-- Uso temporal durante despliegues
```

**Variables de Entorno**:
```bash
# Para aplicación
DB_USERNAME=atleta_staging
DB_PASSWORD=${STAGING_DB_PASSWORD}

# Para migraciones (solo durante despliegue)
FLYWAY_USER=atleta_staging_migration
FLYWAY_PASSWORD=${STAGING_MIGRATION_PASSWORD}
```

### Producción (atleta_prod_app + atleta_prod_migration + atleta_prod_readonly)

**Propósito**: Máxima seguridad con permisos mínimos

#### Usuario de Aplicación (atleta_prod_app)
**Permisos**:
- Solo operaciones CRUD básicas
- **NO** permisos de DDL
- **NO** acceso a tablas del sistema
- Timeouts estrictos

#### Usuario de Migración (atleta_prod_migration)
**Permisos**:
- Usado exclusivamente durante despliegues
- Permisos de DDL controlados
- Conexiones limitadas (máximo 2)

#### Usuario de Solo Lectura (atleta_prod_readonly)
**Permisos**:
- Solo SELECT en tablas de aplicación
- Para monitoreo, reportes y análisis
- Sin acceso a datos sensibles

**Configuración**:
```sql
-- Usuario App: atleta_prod_app
-- Connection Limit: 50
-- Statement Timeout: 60s
-- Idle Timeout: 10min

-- Usuario Migration: atleta_prod_migration
-- Connection Limit: 2
-- Solo durante despliegues

-- Usuario Readonly: atleta_prod_readonly
-- Connection Limit: 10
-- Solo SELECT
```

**Variables de Entorno**:
```bash
# Para aplicación
DB_USERNAME=atleta_prod_app
DB_PASSWORD=${PROD_APP_PASSWORD}

# Para migraciones (solo durante despliegue)
FLYWAY_USER=atleta_prod_migration
FLYWAY_PASSWORD=${PROD_MIGRATION_PASSWORD}

# Para monitoreo
READONLY_USER=atleta_prod_readonly
READONLY_PASSWORD=${PROD_READONLY_PASSWORD}
```

## Configuraciones de Seguridad

### 1. Límites de Conexión

| Usuario | Límite | Justificación |
|---------|--------|---------------|
| atleta_dev | 10 | Desarrollo local |
| atleta_test | 5 | Tests automatizados |
| atleta_staging | 20 | Carga de staging |
| atleta_prod_app | 50 | Carga de producción |
| atleta_prod_migration | 2 | Solo despliegues |
| atleta_prod_readonly | 10 | Consultas de monitoreo |

### 2. Timeouts

#### Producción
- **Statement Timeout**: 60 segundos
- **Idle in Transaction**: 10 minutos
- **Connection Timeout**: 30 segundos

#### Staging
- **Statement Timeout**: 120 segundos
- **Idle in Transaction**: 15 minutos
- **Connection Timeout**: 30 segundos

### 3. Configuración SSL

**Producción**: SSL obligatorio con certificados
**Staging**: SSL recomendado
**Desarrollo**: SSL opcional

## Procedimientos de Seguridad

### 1. Rotación de Contraseñas

**Frecuencia**:
- Producción: Cada 90 días
- Staging: Cada 180 días
- Desarrollo: Según necesidad

**Proceso**:
1. Generar nueva contraseña segura
2. Actualizar en gestor de secretos
3. Actualizar variable de entorno
4. Reiniciar aplicación
5. Verificar conectividad

### 2. Auditoría de Permisos

**Verificación Mensual**:
```sql
-- Verificar permisos de usuario
SELECT * FROM check_user_permissions('atleta_prod_app');

-- Verificar conexiones activas
SELECT usename, application_name, client_addr, state, query_start
FROM pg_stat_activity
WHERE usename LIKE 'atleta_%';
```

### 3. Monitoreo de Seguridad

**Métricas a Monitorear**:
- Intentos de conexión fallidos
- Conexiones desde IPs no autorizadas
- Queries con tiempo de ejecución anómalo
- Acceso a tablas sensibles

**Alertas**:
- Más de 5 intentos fallidos en 5 minutos
- Conexiones desde IPs no conocidas
- Queries que excedan timeout configurado

## Configuración por Ambiente

### Variables de Entorno Requeridas

#### Desarrollo
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=atleta_dev
DB_USERNAME=atleta_dev
DB_PASSWORD=dev_password_change_me
```

#### Staging
```bash
DB_HOST=staging-db.atleta.com
DB_PORT=5432
DB_NAME=atleta_staging
DB_USERNAME=atleta_staging
DB_PASSWORD=${STAGING_DB_PASSWORD}
FLYWAY_USER=atleta_staging_migration
FLYWAY_PASSWORD=${STAGING_MIGRATION_PASSWORD}
```

#### Producción
```bash
DB_HOST=${PROD_DB_HOST}
DB_PORT=5432
DB_NAME=atleta_production
DB_USERNAME=atleta_prod_app
DB_PASSWORD=${PROD_APP_PASSWORD}
FLYWAY_USER=atleta_prod_migration
FLYWAY_PASSWORD=${PROD_MIGRATION_PASSWORD}

# SSL Configuration
SSL_CERT_PATH=/path/to/client-cert.pem
SSL_KEY_PATH=/path/to/client-key.pem
SSL_ROOT_CERT_PATH=/path/to/ca-cert.pem
SSL_PASSWORD=${SSL_CERT_PASSWORD}
```

## Comandos de Administración

### Crear Usuarios
```bash
# Ejecutar script de configuración
psql -h $DB_HOST -U postgres -d $DB_NAME -f scripts/database-users-setup.sql
```

### Verificar Configuración
```sql
-- Listar usuarios y sus permisos
\du

-- Verificar permisos específicos
SELECT * FROM check_user_permissions('atleta_prod_app');

-- Verificar conexiones activas
SELECT * FROM pg_stat_activity WHERE usename LIKE 'atleta_%';
```

### Cambiar Contraseñas
```sql
-- Cambiar contraseña de usuario
ALTER ROLE atleta_prod_app PASSWORD 'nueva_contraseña_segura';
```

## Troubleshooting

### Problemas Comunes

#### 1. "Permission denied for table"
**Causa**: Usuario sin permisos suficientes
**Solución**: Verificar y otorgar permisos necesarios

#### 2. "Too many connections"
**Causa**: Límite de conexiones excedido
**Solución**: Revisar connection pooling y límites

#### 3. "SSL connection required"
**Causa**: Configuración SSL incorrecta
**Solución**: Verificar certificados y configuración SSL

### Logs de Auditoría

Habilitar logging de conexiones y statements:
```sql
-- En postgresql.conf
log_connections = on
log_disconnections = on
log_statement = 'mod'  # Solo modificaciones en producción
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
```

## Referencias

- [PostgreSQL Security Best Practices](https://www.postgresql.org/docs/current/security.html)
- [Database Security Checklist](https://wiki.postgresql.org/wiki/Security_Checklist)
- [Spring Boot Database Security](https://spring.io/guides/gs/securing-web/)