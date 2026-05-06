# Memoria Viva del Backend Atleta

## Vision general

Backend monolitico en Java 21 con Spring Boot 3.3.2 orientado a gestionar identidad de atletas, perfiles futbolisticos, equipos, partidos, rating/OVR, XP, invitaciones sociales y notificaciones internas.

El repositorio implementa una API REST sobre PostgreSQL usando Spring Data JPA y Flyway. La seguridad combina registro local con BCrypt, autenticacion Google OAuth y JWT firmado con HS256.

## Proposito del repo

- Centralizar identidad y autenticacion de usuarios.
- Modelar el dominio futbolistico de Atleta.
- Persistir historial de participacion, rating y XP.
- Exponer contratos REST consumidos por `atleta-app`.

## Estado actual real

- Estado general: funcional pero con deuda tecnica visible.
- Arquitectura real: monolito por capas `controller -> service -> repository -> entity`.
- Persistencia: PostgreSQL como objetivo principal; H2 para parte del perfil `test`.
- Migraciones: 19 migraciones Flyway activas, desde esquema base hasta genero de atleta.
- Observabilidad: Actuator, health check custom, metricas Micrometer/Prometheus y logging con MDC.
- Testing: suite amplia de unit, integration, migration, property-based y seguridad en `src/test/java`.
- `ApiContractSmokeTest` cubre contratos HTTP backend consumidos por el frontend para auth, teams, matches/MVP y ratings sin levantar base de datos.

## Modulos reales detectados

1. Identidad y autenticacion
2. Perfil de jugador y posiciones
3. Equipos y membresias
4. Partidos, convocatoria y cierre
5. Eventos en vivo y SSE
6. Ratings, historial y OVR
7. XP por partido
8. Sistema social: amistades, invitaciones y notificaciones
9. Catalogos: posiciones y canchas
10. Plataforma: seguridad, health, metricas, logging, migraciones y CI

## Decisiones tecnicas detectadas

- UUID como PK de `Athlete` y tambien PK/FK de `PlayerProfile`.
- `PlayerHistory` actua como fuente de verdad historica para goles, asistencias y XP.
- Rating separado de historial: `player_ratings` guarda estado actual y `rating_history` el detalle.
- Cierre de partidos centralizado en `MatchService`, que tambien persiste snapshot final y gatilla rating.
- SSE usado solo para cambios de invitaciones de partido.
- Flyway con `ddl-auto=validate` en todos los perfiles, lo que obliga coherencia entre entidades y esquema.
- Health check custom revisa tablas criticas y estado de Flyway, no solo conectividad.

## Decisiones de seguridad detectadas

- API stateless con `oauth2ResourceServer().jwt(...)`.
- JWT HS256 con issuer configurable y expiracion por propiedad.
- BCrypt para password local.
- Google OAuth validado contra `tokeninfo` remoto de Google.
- CORS permitido solo para orígenes localhost conocidos.
- Swagger y actuator completos quedan abiertos en `dev` y `test`.

## Aprendizajes tecnicos relevantes

- El backend evoluciono por capas sin romper el modelo base: atletas -> player profile -> equipos/partidos -> rating/social.
- Hay esfuerzo fuerte en testing y migraciones, pero menos rigor en autorizacion fina.
- El dominio de partido ya contempla ventanas temporales, validacion de cierre, MVP y categoria por genero.
- Se detecta coexistencia de documentacion antigua y codigo nuevo; el codigo es la fuente confiable.
- Los contratos FE-BE criticos ya tienen doble proteccion: smoke unitario en `atleta-app` para rutas cliente y smoke MVC en backend para rutas/respuestas principales.

## Errores, riesgos y hallazgos

### Riesgos prioritarios

- Alto: `SecurityConfig` deja `"/api/v1/ratings/**"` completamente publico, incluyendo endpoints de escritura manual.
- Alto: la mayoria de endpoints protegidos aceptan `actorUuid`, `playerUuid` o `atletaUuid` por request sin cruzarlo con el `sub` del JWT; un usuario autenticado podria operar sobre terceros si conoce IDs validos.
- Alto: `application-dev.yaml` trae credenciales locales hardcodeadas (`postgres` / `12345`).
- Medio: `TeamService.storeTeamLogo` valida por `contentType` pero no inspecciona firma binaria ni antivirus, y expone archivos desde `/uploads/**`.
- Medio: el pipeline de deploy es parcial; los pasos reales de despliegue siguen siendo `echo`.
- Medio: `docker-compose.ci.yml` asume `build: .` pero el repo no tiene `Dockerfile`.

### Riesgos funcionales y de consistencia

- `TrustScoreService` y `PlayerProfileService.updateTrustScore(...)` duplican logica de trust score.
- `DataInitializationService` y la migracion `V001` no comparten exactamente el mismo catalogo de posiciones.
- `MatchService` implementa auto-start y auto-invalidation, pero no via scheduler; se ejecutan solo cuando se consultan partidos.
- `PlayerProfileService` deja `matchId` del trust log sin asociar aun cuando el request lo trae.
- `registerEvent` cierra eventos inmediatamente con confirmacion home/away para evitar bloqueos, reduciendo el valor real del flujo de confirmacion.
- Existen docs antiguas que contradicen el codigo actual; por ejemplo, partes del README y de `src/main/resources/db/README.md`.

## Deuda tecnica detectada

- Falta autorizacion por identidad del actor y por rol de negocio.
- Falta separar casos de uso grandes: `MatchService` y `RatingService` concentran demasiada responsabilidad.
- Falta automatizacion real de estados de partido via scheduler o job dedicado.
- Falta Dockerfile productivo.
- Falta CI/CD operativo de despliegue.
- Falta estrategia centralizada de manejo de errores para todos los modulos sociales/equipos.
- Falta normalizacion documental: hay varios `.md` desactualizados.

## Proximos pasos recomendados

1. Cerrar la brecha de seguridad: exigir JWT valido en ratings y cruzar `actorUuid` con `Authentication`.
2. Introducir autorizacion de dominio por casos de uso sensibles: borrar equipo, cerrar partido, registrar evento, votar MVP.
3. Agregar smoke E2E opcional contra frontend y backend levantados cuando existan credenciales/seed estables.
4. Extraer `MatchService` en sub-servicios: convocatoria, cierre, eventos, validacion automatica.
5. Consolidar trust score en un solo servicio.
6. Implementar scheduler para refresco de estados de partido.
7. Eliminar secretos hardcodeados de `application-dev.yaml` y estandarizar `.env.example`.
8. Agregar Dockerfile y convertir CI/deploy en pipeline ejecutable.
