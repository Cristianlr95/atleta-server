# Configuración de Base de Datos - Atleta Server

## Introducción

Este directorio contiene toda la configuración relacionada con la base de datos PostgreSQL del proyecto Atleta-Server, incluyendo migraciones Flyway, datos de prueba, y scripts de callback.

## Estructura de Directorios

```
db/
├── migration/              # Migraciones principales de Flyway
│   ├── V001__create_initial_schema.sql
│   ├── V002__add_indexes_performance.sql
│   ├── V003__insert_initial_data.sql
│   └── V00X__future_migrations.sql
├── test-data/             # Datos específicos para testing
│   ├── V900__insert_test_athletes.sql
│   └── V901__insert_test_teams.sql
├── callbacks/             # Scripts de callback de Flyway
│   ├── beforeMigrate.sql
│   └── afterMigrate.sql
└── README.md             # Este archivo
```

## Configuración de PostgreSQL Local

### Instalación Rápida

#### Windows (PowerShell como Administrador)
```powershell
# Usando Chocolatey
choco install postgresql

# O usando Scoop
scoop install postgresql
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

### Configuración Inicial

1. **Crear usuario y base de datos para desarrollo:**

```sql
-- Conectar como superusuario postgres
sudo -u postgres psql

-- Crear usuario de desarrollo
CREATE USER atleta_dev WITH PASSWORD 'dev_password';

-- Crear base de datos
CREATE DATABASE atleta_dev OWNER atleta_dev;

-- Otorgar permisos
GRANT ALL PRIVILEGES ON DATABASE atleta_dev TO atleta_dev;

-- Conectar a la nueva base de datos
\c atleta_dev

-- Habilitar extensiones necesarias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Otorgar permisos en el esquema public
GRANT ALL ON SCHEMA public TO atleta_dev;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO atleta_dev;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO atleta_dev;

-- Salir
\q
```

2. **Verificar la instalación:**

```bash
# Verificar que PostgreSQL esté ejecutándose
pg_isready -h localhost -p 5432

# Probar conexión
psql -h localhost -U atleta_dev -d atleta_dev -c "SELECT version();"
```

## Variables de Entorno

### Desarrollo Local

Crear un archivo `.env` en la raíz del proyecto (no commitear):

```bash
# Base de datos de desarrollo
DB_HOST=localhost
DB_PORT=5432
DB_NAME=atleta_dev
DB_USERNAME=atleta_dev
DB_PASSWORD=dev_password

# Configuración opcional
SPRING_PROFILES_ACTIVE=dev
```

### Configuración por Ambiente

#### Desarrollo
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=atleta_dev
export DB_USERNAME=atleta_dev
export DB_PASSWORD=dev_password
```

#### Testing
```bash
# Testcontainers maneja automáticamente la configuración
# No se requieren variables de entorno adicionales
```

#### Staging
```bash
export DB_HOST=staging-db.atleta.com
export DB_PORT=5432
export DB_NAME=atleta_staging
export DB_USERNAME=atleta_staging
export DB_PASSWORD=<password_seguro_staging>
```

#### Producción
```bash
export DB_HOST=<prod-db-host>
export DB_PORT=5432
export DB_NAME=<prod-db-name>
export DB_USERNAME=<prod-username>
export DB_PASSWORD=<password_seguro_prod>
```

## Migraciones Flyway

### Convenciones de Nomenclatura

- **Formato:** `V{VERSION}__{DESCRIPTION}.sql`
- **Versión:** Número secuencial (001, 002, 003...)
- **Descripción:** Snake_case, descriptiva y concisa

**Ejemplos:**
```
V001__create_initial_schema.sql
V002__add_indexes_performance.sql
V003__insert_initial_data.sql
V004__add_trust_score_constraints.sql
V005__create_match_events_table.sql
```

### Estructura de Migraciones

#### V001 - Esquema Inicial
- Crea todas las tablas del dominio Atleta
- Define relaciones y constraints
- Habilita extensiones PostgreSQL necesarias

#### V002 - Índices de Performance
- Índices básicos para búsquedas frecuentes
- Índices compuestos para consultas complejas
- Índices de texto completo (GIN)

#### V003 - Datos Iniciales
- Posiciones de fútbol (catálogo)
- Usuario administrador por defecto
- Configuraciones del sistema

### Comandos Útiles

```bash
# Ver estado de migraciones
mvn flyway:info

# Ejecutar migraciones pendientes
mvn flyway:migrate

# Validar migraciones
mvn flyway:validate

# Reparar checksums (solo desarrollo)
mvn flyway:repair

# Limpiar base de datos (solo desarrollo)
mvn flyway:clean
```

## Configuración de Conexión

### HikariCP (Pool de Conexiones)

La configuración varía por ambiente:

| Ambiente | Max Pool | Min Idle | Connection Timeout |
|----------|----------|----------|--------------------|
| dev      | 10       | 5        | 20s               |
| test     | 5        | 2        | 10s               |
| staging  | 20       | 10       | 30s               |
| prod     | 50       | 20       | 30s               |

### Configuración SSL (Producción)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?ssl=true&sslmode=require
    hikari:
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000
```

## Testing con Testcontainers

### Configuración Automática

Los tests usan Testcontainers automáticamente:

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class MyIntegrationTest extends BaseIntegrationTest {
    // Los contenedores se manejan automáticamente
}
```

### Datos de Prueba

Los archivos en `test-data/` se cargan automáticamente en tests:

- `V900__insert_test_athletes.sql` - Atletas de prueba
- `V901__insert_test_teams.sql` - Equipos de prueba

## Monitoreo y Métricas

### Health Checks

```bash
# Verificar salud de la base de datos
curl http://localhost:8080/actuator/health/db

# Métricas de HikariCP
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### Métricas Disponibles

- `hikaricp.connections.active` - Conexiones activas
- `hikaricp.connections.idle` - Conexiones inactivas
- `hikaricp.connections.pending` - Conexiones pendientes
- `hikaricp.connections.timeout` - Timeouts de conexión

## Backup y Recuperación

### Scripts Disponibles

```bash
# Backup completo
./scripts/backup-database.sh dev

# Restaurar backup
./scripts/restore-database.sh dev full /path/to/backup.dump

# Ver ayuda
./scripts/backup-database.sh --help
```

### Tipos de Backup

1. **Completo** - Esquema + datos
2. **Esquema** - Solo estructura
3. **Datos** - Solo información

## Troubleshooting

### Problemas Comunes

#### 1. "Connection refused"
```bash
# Verificar que PostgreSQL esté ejecutándose
sudo systemctl status postgresql  # Linux
brew services list | grep postgresql  # macOS

# Verificar puerto
netstat -an | grep 5432
```

#### 2. "Authentication failed"
```bash
# Verificar usuario y contraseña
psql -h localhost -U atleta_dev -d atleta_dev

# Recrear usuario si es necesario
sudo -u postgres psql -c "DROP USER IF EXISTS atleta_dev;"
sudo -u postgres psql -c "CREATE USER atleta_dev WITH PASSWORD 'dev_password';"
```

#### 3. "Database does not exist"
```bash
# Crear base de datos
sudo -u postgres createdb -O atleta_dev atleta_dev
```

#### 4. "Migration checksum mismatch"
```bash
# Reparar checksums (solo desarrollo)
mvn flyway:repair

# O limpiar y re-migrar
mvn flyway:clean flyway:migrate
```

### Logs Útiles

```bash
# Logs de aplicación
tail -f logs/application.log | grep -E "(flyway|hikari|postgresql)"

# Logs de PostgreSQL
# Linux: /var/log/postgresql/
# macOS: /usr/local/var/log/
# Windows: C:\Program Files\PostgreSQL\16\data\log\
```

## Configuración de IDE

### IntelliJ IDEA

1. **Database Tool Window:**
   - Host: localhost
   - Port: 5432
   - Database: atleta_dev
   - User: atleta_dev
   - Password: dev_password

2. **Data Source:**
   - Driver: PostgreSQL
   - URL: `jdbc:postgresql://localhost:5432/atleta_dev`

### VS Code

Instalar extensión PostgreSQL y configurar:

```json
{
  "postgresql.connections": [
    {
      "name": "Atleta Dev",
      "host": "localhost",
      "port": 5432,
      "database": "atleta_dev",
      "username": "atleta_dev",
      "password": "dev_password"
    }
  ]
}
```

## Comandos de Desarrollo Rápido

```bash
# Setup completo desde cero
sudo -u postgres createuser -P atleta_dev  # Ingresar password: dev_password
sudo -u postgres createdb -O atleta_dev atleta_dev
psql -h localhost -U atleta_dev -d atleta_dev -c "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"; CREATE EXTENSION IF NOT EXISTS pg_trgm;"

# Ejecutar aplicación
export DB_USERNAME=atleta_dev DB_PASSWORD=dev_password
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Reset completo (desarrollo)
mvn flyway:clean flyway:migrate -Dspring.profiles.active=dev

# Verificar todo funciona
mvn test -Dtest="FlywayIntegrationTest"
```

## Recursos Adicionales

- [Documentación PostgreSQL](https://www.postgresql.org/docs/)
- [Documentación Flyway](https://flywaydb.org/documentation/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [Testcontainers PostgreSQL](https://www.testcontainers.org/modules/databases/postgres/)

## Contacto

Para problemas específicos de configuración de base de datos, revisar:
1. Este README
2. `docs/database-migration-guide.md` - Guía completa de migración
3. `docs/database-security-guide.md` - Configuraciones de seguridad
4. Logs de aplicación en `logs/`