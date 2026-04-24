# Deployment Backend Atleta

## Resumen tecnico

- Stack detectado: Java 21, Spring Boot 3.3.2, Maven Wrapper, Spring Security, OAuth2 Resource Server, PostgreSQL, Flyway, Actuator.
- Perfiles disponibles: `dev`, `test`, `staging`, `prod`.
- Autenticacion: JWT HS256 emitido por backend y login Google OAuth.
- Health checks: `GET /actuator/health` y `GET /actuator/health/readiness`.
- Migraciones: Flyway sobre `classpath:db/migration`, tabla `flyway_schema_history`.

## Estado actual preparado para despliegue

- La app ya no fuerza `dev` como perfil activo; ahora usa `dev` solo como perfil por defecto.
- La configuracion puede importar `/.env` local con `spring.config.import`.
- `dev`, `staging` y `prod` usan PostgreSQL por variables de entorno.
- CORS ahora sale de propiedades y en `staging`/`prod` exige origenes explicitos.
- El backend expone `health` y `health/readiness` para probes.
- Se agrego `Dockerfile`, `.dockerignore` y `docker-compose.yml` para desarrollo local.
- Los handlers de error ahora evitan exponer detalles sensibles fuera de `dev`/`test`.

## Variables de entorno necesarias

### Minimas para desarrollo

```env
SPRING_PROFILES_ACTIVE=dev
DB_HOST=localhost
DB_PORT=5432
DB_NAME=atleta_dev
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=replace-with-a-secure-secret-of-at-least-32-bytes
JWT_ISSUER=atleta-dev
JWT_EXPIRATION=PT1H
SERVER_PORT=8080
CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:8100,http://localhost:4200,http://localhost:4201,http://localhost:3000
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=true
FLYWAY_CLEAN_DISABLED=false
```

### Minimas para staging

```env
SPRING_PROFILES_ACTIVE=staging
DB_HOST=<staging-db-host>
DB_PORT=5432
DB_NAME=<staging-db-name>
DB_USERNAME=<staging-db-user>
DB_PASSWORD=<staging-db-password>
JWT_SECRET=<32+-bytes-or-base64>
JWT_ISSUER=atleta-staging
JWT_EXPIRATION=PT1H
SERVER_PORT=8080
CORS_ALLOWED_ORIGIN_PATTERNS=https://staging.atleta.example
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false
```

### Minimas para produccion

```env
SPRING_PROFILES_ACTIVE=prod
DB_HOST=<prod-db-host>
DB_PORT=5432
DB_NAME=<prod-db-name>
DB_USERNAME=<prod-db-user>
DB_PASSWORD=<prod-db-password>
JWT_SECRET=<32+-bytes-or-base64>
JWT_ISSUER=atleta-prod
JWT_EXPIRATION=PT1H
SERVER_PORT=8080
CORS_ALLOWED_ORIGIN_PATTERNS=https://api.atleta.example,https://app.atleta.example
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false
SSL_CERT_PATH=/run/secrets/db-client-cert.pem
SSL_KEY_PATH=/run/secrets/db-client-key.pem
SSL_ROOT_CERT_PATH=/run/secrets/db-root-ca.pem
SSL_PASSWORD=<optional-if-key-encrypted>
```

### Opcionales

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `MVP_XP_BONUS_ENABLED`
- `CORS_ALLOWED_METHODS`
- `CORS_ALLOWED_HEADERS`
- `CORS_EXPOSED_HEADERS`
- `CORS_ALLOW_CREDENTIALS`
- `CORS_MAX_AGE`
- `JAVA_OPTS`

## Levantar backend en local

### Opcion A: Maven + PostgreSQL local

1. Copia `.env.example` a `.env`.
2. Ajusta `DB_*`, `JWT_SECRET` y opcionalmente Google OAuth.
3. Crea la base si aun no existe.
4. Ejecuta:

```powershell
cd atleta-server
./mvnw.cmd spring-boot:run
```

### Opcion B: Docker Compose local

```powershell
cd atleta-server
docker compose up --build
```

Servicios disponibles:

- Backend: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Readiness: `http://localhost:8080/actuator/health/readiness`
- PostgreSQL: `localhost:5432`

## Migraciones

### Estrategia definida

- `dev`: Flyway habilitado, `baseline-on-migrate=true`, `clean-disabled=false`.
- `staging`: Flyway habilitado, `baseline-on-migrate=false`, `clean-disabled=true`.
- `prod`: Flyway habilitado, `baseline-on-migrate=false`, `clean-disabled=true`, SSL obligatorio en JDBC.
- `test`: ejecuta migraciones automaticamente sobre H2/Testcontainers.

### Comandos utiles

```powershell
./mvnw.cmd flyway:validate
./mvnw.cmd flyway:info
./mvnw.cmd flyway:migrate
```

### Recomendacion operativa

- Ejecutar `flyway:validate` en CI antes de desplegar.
- Mantener `FLYWAY_ENABLED=true` solo en instancias o jobs que deban migrar.
- Si el proveedor lo permite, usar una sola instancia de migracion por release para evitar carreras.
- No usar `flyway:clean` fuera de `dev`.

## Construir imagen Docker

```powershell
docker build -t atleta-backend:local .
docker run --rm -p 8080:8080 --env-file .env atleta-backend:local
```

Notas:

- El `Dockerfile` compila con Java 21 y ejecuta como usuario no root.
- La imagen final solo contiene el JAR generado.

## Despliegue en Render

1. Crear un servicio web desde este directorio o desde la imagen Docker.
2. Definir `SPRING_PROFILES_ACTIVE=prod`.
3. Configurar variables `DB_*`, `JWT_*`, `CORS_ALLOWED_ORIGIN_PATTERNS`.
4. Si la base requiere SSL cliente, montar certificados como secretos/volumen.
5. Configurar health check:

```text
/actuator/health/readiness
```

6. Asegurar que solo una replica haga migraciones al inicio si no hay job dedicado.

## Despliegue en Railway

1. Crear servicio con Dockerfile.
2. Conectar PostgreSQL gestionado o externo.
3. Cargar las mismas variables de `prod`.
4. Configurar dominio publico y usarlo tambien en `CORS_ALLOWED_ORIGIN_PATTERNS`.
5. Verificar `GET /actuator/health/readiness` despues del deploy.

## Despliegue en VPS

### Recomendacion

- Ejecutar el backend como contenedor.
- Ubicarlo detras de Nginx o Caddy con TLS.
- Mantener PostgreSQL fuera del contenedor app, idealmente gestionado o en host separado.

### Flujo sugerido

```powershell
docker build -t atleta-backend:release .
docker run -d --name atleta-backend --restart unless-stopped -p 8080:8080 --env-file .env.prod atleta-backend:release
```

Checklist de reverse proxy:

- TLS valido
- `X-Forwarded-*` habilitado
- limites de tamano razonables
- rate limiting basico
- acceso restringido a endpoints administrativos

## Configuracion recomendada de produccion

- `SPRING_PROFILES_ACTIVE=prod`
- `JWT_SECRET` rotado y almacenado en secret manager
- `CORS_ALLOWED_ORIGIN_PATTERNS` solo con dominios reales
- DB SSL habilitado
- Actuator expuesto solo para `health`; el resto protegido por auth/red privada
- logs centralizados y sin SQL debug
- backups automaticos de PostgreSQL
- una estrategia clara de rollback

## Seguridad minima obligatoria

- No commitear `.env`, certificados ni secretos.
- JWT con 32+ bytes reales.
- Nada de wildcard en CORS con credenciales.
- `ratings/**` ya no queda publico por defecto.
- `Flyway clean` deshabilitado fuera de `dev`.
- TLS extremo a extremo entre cliente y proxy; SSL de DB en `prod`.
- Revisar autorizacion de dominio en endpoints que reciben UUIDs del cliente.

## Logs y observabilidad

Ya disponible:

- MDC con `userId` y `transactionId`
- Actuator `health`, `info`, `metrics`, `flyway`, `prometheus`
- custom DB health indicator

Recomendado:

- centralizar logs en la plataforma
- alertas por `5xx`, tiempos de respuesta y fallos de DB
- tablero minimo de Hikari, JVM, HTTP y Flyway

## Backups recomendados

Scripts existentes:

- `scripts/backup-database.sh`
- `scripts/restore-database.sh`

Politica sugerida:

- backup diario completo
- backup logico antes de cada release con migraciones
- retencion minima de 7 a 30 dias
- prueba de restore al menos una vez por sprint

## Riesgos detectados

- `application-dev.yaml` tenia credenciales hardcodeadas; ya se removieron del config versionado.
- El repo sigue teniendo un `.env` local con password simple; no se versiona, pero conviene rotarlo.
- Hay endpoints que aceptan UUIDs del cliente y requieren auditoria de autorizacion de negocio.
- `staging` y `prod` dependian de defaults implicitos para CORS/DB; ahora exigen variables explicitas.
- La estrategia de migracion sigue embebida al arranque; para escalar conviene separar un job de migracion.
- No hay evidencia de trazas distribuidas ni alertado activo fuera de Actuator.

## Checklist previo a produccion

1. Confirmar `SPRING_PROFILES_ACTIVE=prod`.
2. Verificar `JWT_SECRET` seguro y rotado.
3. Confirmar `CORS_ALLOWED_ORIGIN_PATTERNS` sin comodines.
4. Validar conectividad PostgreSQL con SSL.
5. Ejecutar `flyway:validate`.
6. Tomar backup previo al release.
7. Desplegar y revisar `/actuator/health/readiness`.
8. Verificar login local o Google y un endpoint autenticado.
9. Confirmar centralizacion de logs.
10. Definir rollback o imagen previa disponible.
