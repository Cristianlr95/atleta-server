# Changelog de Documentacion Viva

## Estado inicial documentado - 2026-04-23

### Backend detectado

- Monolito Spring Boot 3.3.2 sobre Java 21
- Persistencia principal en PostgreSQL con Flyway
- Seguridad con BCrypt + Google OAuth + JWT HS256
- Modulos activos: atletas, perfiles, posiciones, equipos, partidos, ratings, social, canchas, observabilidad

### Modulos principales

- Identidad y autenticacion
- Perfil de jugador y trust score
- Equipos y membresias
- Partidos y cierre con XP/rating
- MVP y SSE
- Ratings, OVR y leaderboard
- Amistades, invitaciones y notificaciones
- Catalogo de canchas y posiciones

### Hallazgos importantes

- `ratings/**` esta publico en `SecurityConfig`, incluyendo escrituras.
- La autorizacion por identidad es debil: varios endpoints reciben UUIDs arbitrarios sin verificar el `sub` del JWT.
- `MatchService` ya soporta auto-start y auto-invalidacion, pero solo se ejecutan durante lecturas.
- Hay duplicidad entre `PlayerProfileService` y `TrustScoreService`.
- El pipeline CI existe, pero deploy sigue siendo placeholder.
- `docker-compose.ci.yml` no es suficiente porque falta `Dockerfile`.
- Parte de la documentacion heredada no refleja el estado real del codigo.

### Deuda tecnica inicial

- God services en `MatchService` y `RatingService`
- Autorizacion de dominio ausente
- Credenciales locales hardcodeadas en `application-dev.yaml`
- Inconsistencias en semillas/catalogos de posiciones
- Falta scheduler para automatizaciones temporales

### Riesgos principales

- Riesgo alto de mutaciones no autorizadas
- Riesgo alto por endpoints de rating expuestos sin auth
- Riesgo medio por manejo de archivos subidos
- Riesgo medio por despliegue no automatizado realmente

### Archivos creados en esta pasada

- `docs/memory.md`
- `docs/funcionalidades.md`
- `docs/architecture.md`
- `docs/deployment.md`
- `docs/feedback-system.md`
- `docs/changelog.md`
