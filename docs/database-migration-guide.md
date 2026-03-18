# Guía de Migración de Base de Datos: H2 a PostgreSQL

## Introducción

Esta guía proporciona instrucciones paso a paso para migrar la aplicación Atleta-Server desde H2 en memoria a PostgreSQL con Flyway para migraciones. La migración incluye configuraciones optimizadas por ambiente, testing con Testcontainers, y estrategias de monitoreo.

## Prerrequisitos

### Software Requerido

1. **PostgreSQL 16.x** instalado localmente
2. **Docker** para Testcontainers (testing)
3. **Java 21** (ya configurado en el proyecto)
4. **Maven 3.9+** (ya configurado en el proyecto)

### Instalación de PostgreSQL

#### Windows
```bash
# Usando Chocolatey
choco install postgresql

# O descargar desde https://www.postgresql.org/download/windows/
```

#### macOS
```bash
# Usando Homebrew
brew install postgresql@16
brew services start postgresql@16
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install postgresql-16 postgresql-contrib-16
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

## Pasos de Migración

### Paso 1: Configurar PostgreSQL Local

1. **Crear base de datos de desarrollo:**
```sql
-- Conectar como superusuario
sudo -u postgres psql

-- Crear usuario y base de datos
CREATE USER atleta_dev WITH PASSWORD 'dev_password';
CREATE DATABASE atleta_dev OWNER atleta_dev;
GRANT ALL PRIVILEGES ON DATABASE atleta_dev TO atleta_dev;

-- Habilitar extensiones necesarias
\c atleta_dev
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

2. **Configurar variables de entorno:**
```bash
# Para desarrollo local
export DB_USERNAME=atleta_dev
export DB_PASSWORD=dev_password
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=atleta_dev
```

### Paso 2: Verificar Dependencias

Las dependencias ya están configuradas en `pom.xml`:
- PostgreSQL JDBC Driver 42.7.7
- Flyway 10.10.0
- Testcontainers para testing

### Paso 3: Ejecutar Migraciones

1. **Verificar configuración:**
```bash
# Verificar que PostgreSQL esté ejecutándose
pg_isready -h localhost -p 5432

# Verificar conexión
psql -h localhost -U atleta_dev -d atleta_dev -c "SELECT version();"
```

2. **Ejecutar la aplicación:**
```bash
# Perfil de desarrollo
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# O usando el JAR
java -jar target/server-atleta-*.jar --spring.profiles.active=dev
```

3. **Verificar migraciones:**
```sql
-- Conectar a la base de datos
psql -h localhost -U atleta_dev -d atleta_dev

-- Verificar tabla de historial de Flyway
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- Verificar que las tablas se crearon correctamente
\dt
```

### Paso 4: Ejecutar Tests

```bash
# Ejecutar todos los tests (usa Testcontainers automáticamente)
mvn test

# Ejecutar solo tests de integración
mvn test -Dtest="*IntegrationTest"

# Ejecutar tests de migración específicos
mvn test -Dtest="FlywayIntegrationTest,DatabaseConfigurationTest"
```

## Configuración por Ambientes

### Desarrollo (dev)
- Base de datos: PostgreSQL local
- Pool de conexiones: 10 máximo
- Logging: SQL detallado habilitado
- Flyway clean: Habilitado

### Testing (test)
- Base de datos: Testcontainers PostgreSQL
- Pool de conexiones: 5 máximo
- Logging: Solo errores y warnings
- Flyway clean: Habilitado

### Staging (staging)
```bash
# Variables de entorno requeridas
export DB_HOST=staging-db.atleta.com
export DB_USERNAME=atleta_staging
export DB_PASSWORD=<password_seguro>
export DB_NAME=atleta_staging
```

### Producción (prod)
```bash
# Variables de entorno requeridas
export DB_HOST=<prod-db-host>
export DB_USERNAME=<prod-user>
export DB_PASSWORD=<password_seguro>
export DB_NAME=<prod-db-name>
```

## Troubleshooting Común

### Error: "relation does not exist"

**Problema:** Las migraciones no se ejecutaron correctamente.

**Solución:**
```bash
# Verificar estado de Flyway
mvn flyway:info -Dflyway.configFiles=src/main/resources/application-dev.yaml

# Reparar si es necesario
mvn flyway:repair

# Ejecutar migraciones manualmente
mvn flyway:migrate
```

### Error: "Connection refused"

**Problema:** PostgreSQL no está ejecutándose o configuración incorrecta.

**Solución:**
```bash
# Verificar que PostgreSQL esté ejecutándose
sudo systemctl status postgresql  # Linux
brew services list | grep postgresql  # macOS

# Verificar configuración de conexión
psql -h localhost -U atleta_dev -d atleta_dev
```

### Error: "password authentication failed"

**Problema:** Credenciales incorrectas o usuario no existe.

**Solución:**
```sql
-- Recrear usuario si es necesario
sudo -u postgres psql
DROP USER IF EXISTS atleta_dev;
CREATE USER atleta_dev WITH PASSWORD 'dev_password';
GRANT ALL PRIVILEGES ON DATABASE atleta_dev TO atleta_dev;
```

### Error: "Testcontainers could not find a valid Docker environment"

**Problema:** Docker no está instalado o ejecutándose.

**Solución:**
```bash
# Verificar Docker
docker --version
docker ps

# Iniciar Docker si no está ejecutándose
sudo systemctl start docker  # Linux
# O iniciar Docker Desktop en Windows/macOS
```

### Error: "Migration checksum mismatch"

**Problema:** Archivo de migración fue modificado después de ejecutarse.

**Solución:**
```bash
# Reparar checksums
mvn flyway:repair

# O limpiar y re-migrar (solo en desarrollo)
mvn flyway:clean flyway:migrate
```

### Error de Performance: "Connection pool exhausted"

**Problema:** Pool de conexiones insuficiente para la carga.

**Solución:**
```yaml
# Ajustar en application-{profile}.yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Incrementar según necesidad
      minimum-idle: 10
```

## Comandos Útiles por Ambiente

### Desarrollo
```bash
# Iniciar aplicación
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Limpiar y re-migrar base de datos
mvn flyway:clean flyway:migrate -Dspring.profiles.active=dev

# Backup de desarrollo
./scripts/backup-database.sh dev

# Restaurar backup
./scripts/restore-database.sh dev full /path/to/backup.dump
```

### Testing
```bash
# Ejecutar todos los tests
mvn test

# Ejecutar tests específicos
mvn test -Dtest="DatabaseConfigurationTest"

# Ejecutar tests de propiedades
mvn test -Dtest="*PropertyTest"
```

### Staging
```bash
# Desplegar a staging
java -jar target/server-atleta-*.jar --spring.profiles.active=staging

# Backup de staging
./scripts/backup-database.sh staging

# Verificar migraciones
mvn flyway:info -Dspring.profiles.active=staging
```

### Producción
```bash
# Desplegar a producción
java -jar target/server-atleta-*.jar --spring.profiles.active=prod

# Backup de producción
./scripts/backup-database.sh prod

# Solo verificar estado (no ejecutar migraciones automáticamente)
mvn flyway:info -Dspring.profiles.active=prod
```

## Rollback y Recuperación

### Rollback de Migraciones

Flyway no soporta rollback automático. Para revertir cambios:

1. **Crear migración de rollback manual:**
```sql
-- V004__rollback_previous_changes.sql
-- Revertir cambios específicos de V003
```

2. **Restaurar desde backup:**
```bash
# Restaurar backup completo
./scripts/restore-database.sh dev full /path/to/backup.dump

# Restaurar solo datos
./scripts/restore-database.sh dev data /path/to/data_backup.dump
```

### Recuperación de Desastres

1. **Backup automático diario** (configurar en cron):
```bash
# Agregar a crontab
0 2 * * * /path/to/scripts/backup-database.sh prod
```

2. **Verificación de integridad:**
```bash
# Verificar backup
pg_restore --list /path/to/backup.dump

# Test de restauración en ambiente de prueba
./scripts/restore-database.sh staging full /path/to/prod_backup.dump
```

## Monitoreo Post-Migración

### Health Checks
```bash
# Verificar health de la aplicación
curl http://localhost:8080/actuator/health

# Verificar métricas de base de datos
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### Logs Importantes
```bash
# Logs de Flyway
grep "flyway" logs/application.log

# Logs de conexiones
grep "HikariPool" logs/application.log

# Errores de base de datos
grep "SQLException" logs/application.log
```

## Checklist de Migración Completa

- [ ] PostgreSQL instalado y configurado
- [ ] Variables de entorno configuradas
- [ ] Base de datos de desarrollo creada
- [ ] Migraciones ejecutadas exitosamente
- [ ] Tests pasando (incluyendo Testcontainers)
- [ ] Health checks funcionando
- [ ] Métricas de base de datos disponibles
- [ ] Scripts de backup configurados
- [ ] Logging configurado por ambiente
- [ ] Documentación actualizada

## Contacto y Soporte

Para problemas adicionales:
1. Revisar logs de aplicación en `logs/`
2. Verificar configuración en `application-{profile}.yaml`
3. Consultar documentación de PostgreSQL y Flyway
4. Revisar issues conocidos en el repositorio del proyecto