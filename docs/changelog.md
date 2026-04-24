# Changelog de Documentacion Viva

## 2026-04-23 - Preparacion de despliegue backend

### Objetivo cubierto

- Se dejo el backend listo para correr en `dev` y para desplegarse en `staging`/`prod` con configuracion por ambiente, Docker y documentacion operativa.

### Archivos tocados

- `.env.example`
- `.gitignore`
- `Dockerfile`
- `.dockerignore`
- `docker-compose.yml`
- `docs/deployment.md`
- `docs/changelog.md`
- `src/main/resources/application.yaml`
- `src/main/resources/application-dev.yaml`
- `src/main/resources/application-staging.yaml`
- `src/main/resources/application-prod.yaml`
- `src/main/java/com/atleta/demo/config/SecurityConfig.java`
- `src/main/java/com/atleta/demo/config/AppCorsProperties.java`
- `src/main/java/com/atleta/demo/validation/DatabaseConfigurationValidator.java`
- `src/main/java/com/atleta/demo/exception/DatabaseExceptionHandler.java`
- `src/main/java/com/atleta/demo/exception/RatingExceptionHandler.java`
- `src/main/java/com/atleta/demo/exception/ApiExceptionHandler.java`

### Cambios principales

- `dev` pasa a ser perfil por defecto, no perfil forzado.
- Se habilita carga opcional de `/.env`.
- PostgreSQL queda parametrizado por variables en `dev`, `staging` y `prod`.
- CORS sale de Java hardcodeado y pasa a propiedades configurables.
- En `staging` y `prod` se exigen origenes CORS explicitos.
- `ratings/**` deja de estar publico en `SecurityConfig`.
- Se endurece el manejo de errores para no filtrar detalles sensibles fuera de `dev`/`test`.
- Se agregan probes de salud y readiness.
- Se agrega contenedorizacion base para build y ejecucion.

### Riesgos detectados

- Persisten riesgos de autorizacion de negocio en endpoints que aceptan UUIDs arbitrarios.
- El `.env` local existente usa una password debil; no esta versionado, pero conviene rotarlo.
- Las migraciones siguen acopladas al arranque de la app; en alta concurrencia es mejor separarlas en un job dedicado.
- Actuator ofrece observabilidad basica, pero aun falta alertado y trazabilidad centralizada.
- No se audito en esta pasada cada endpoint para ownership checks entre `JWT.sub` y recursos mutables.

### Siguiente paso recomendado

- Implementar una capa de autorizacion por dominio para comparar `JWT.sub` con el recurso mutado y definir un job de migracion separado para releases productivos.
