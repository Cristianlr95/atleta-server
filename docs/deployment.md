# Deployment y Operacion del Backend Atleta

## Levantar backend en desarrollo

### Opcion 1: Maven Wrapper

```powershell
cd atleta-server
./mvnw.cmd spring-boot:run
```

### Opcion 2: script Windows del repo

```powershell
.\scripts\setup-local.ps1 -CreateDb
```

Ese script:

- valida Java
- opcionalmente crea DB y usuario en PostgreSQL
- setea variables `DB_*`
- ejecuta `mvnw.cmd spring-boot:run`

## Variables de entorno y perfiles

### Perfiles reales detectados

- `dev`: activo por defecto
- `test`
- `staging`
- `prod`

### Variables relevantes

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ISSUER`
- `JWT_EXPIRATION`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `SERVER_PORT`
- `SSL_CERT_PATH`
- `SSL_KEY_PATH`
- `SSL_ROOT_CERT_PATH`
- `SSL_PASSWORD`
- `MVP_XP_BONUS_ENABLED`

### Observaciones reales

- `application-dev.yaml` aun trae defaults locales hardcodeados para DB.
- `JWT_SECRET` es obligatorio fuera de `dev`/`test`.
- En `prod`, el validador obliga SSL en la URL JDBC.

## Base de datos local

Configuracion real esperada en `dev`:

- host: `localhost`
- puerto: `5432`
- base: `atleta_dev`
- usuario por defecto en YAML: `postgres`

Recomendacion:

- no depender del password hardcodeado del YAML
- usar `.env` local no versionado o variables del shell

## Migraciones

- Flyway habilitado en todos los perfiles.
- Tabla de control: `flyway_schema_history`
- En `dev`: `baseline-on-migrate=true`, `clean-disabled=false`
- En `staging` y `prod`: `baseline-on-migrate=false`, `clean-disabled=true`
- En `test`: usa `classpath:db/migration` y `classpath:db/test-data`

## Estrategia segura dev/prod

### Desarrollo

- Permitir Swagger y actuator completos.
- Usar base local aislada.
- No reutilizar secretos de staging/prod.

### Produccion

- Definir `SPRING_PROFILES_ACTIVE=prod`
- Inyectar secretos por variables o secret manager
- Exigir `JWT_SECRET` de 32+ bytes
- Mantener SSL DB activo
- Deshabilitar detalle de health como ya hace `application-prod.yaml`

## Secretos

Recomendado para produccion:

- GitHub Actions Secrets o secret manager del cloud
- nunca commitear `.env`
- rotacion de `JWT_SECRET`, credenciales DB y Google client secret

Hallazgo:

- `.gitignore` excluye `.env`, pero existe configuracion sensible hardcodeada en `application-dev.yaml`.

## Docker

- Existe `docker-compose.ci.yml`
- No existe `Dockerfile`

Conclusion:

- `docker-compose.ci.yml` esta parcial y hoy no sirve como estrategia completa de despliegue.
- Para producción hay que agregar un `Dockerfile` multi-stage o documentar un despliegue directo del JAR.

## CI/CD recomendado

### Lo que ya existe

- workflow `.github/workflows/ci.yml`
- jobs de test, build, validacion Flyway y artefacto JAR
- intentos de deploy a staging/prod

### Limites reales

- pasos de deploy siguen siendo placeholder (`echo`)
- health check productivo apunta a `http://your-prod-server/...`

### Recomendacion

1. Mantener build del JAR en GitHub Actions.
2. Ejecutar Flyway validate y backup antes del deploy.
3. Publicar imagen o artefacto firmado.
4. Desplegar con rollback automatizado.
5. Correr smoke test contra `/actuator/health` real.

## Monitoreo

Ya disponible:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/flyway`
- `/actuator/prometheus`

Adicional:

- health indicator custom de DB
- metricas de Hikari
- metricas de dominio `atleta.*`

Recomendado:

- dashboard Prometheus/Grafana
- alertas sobre errores 5xx, pool Hikari, partidos invalidos y latencia de invitaciones

## Backups

Scripts existentes:

- `scripts/backup-database.sh`
- `scripts/restore-database.sh`

Capacidades detectadas:

- backup full/schema/data
- gzip
- checksum
- retencion
- restore bloqueado para prod

Observacion:

- la documentacion de scripts usa variables `DB_USER` en partes, mientras la app usa `DB_USERNAME`; conviene unificar.

## Hardening basico

1. Cerrar `ratings/**` en SecurityConfig.
2. Cruzar subject JWT con `actorUuid` y `playerUuid`.
3. Quitar credenciales del YAML dev.
4. Validar contenido real de archivos subidos.
5. Ejecutar la app detras de reverse proxy con HTTPS y limites de tamaño.
6. Revisar exposición de actuator fuera de ambientes internos.

## Hosting recomendado para produccion

Opciones razonables para este backend:

- App runner / container service gestionado con PostgreSQL administrado
- VM o Kubernetes ligero con JAR o imagen Docker

Recomendacion principal:

- backend stateless en contenedor
- PostgreSQL administrado con backups automaticos
- reverse proxy con TLS
- Prometheus/Grafana o equivalente cloud
