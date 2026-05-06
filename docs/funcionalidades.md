# Funcionalidades del Backend Atleta

## Mapa funcional

### Autenticacion e identidad

- `Implementada` Registro local: `POST /api/v1/athletes/register`
- `Implementada` Login local con JWT: `POST /api/v1/athletes/login`
- `Implementada` Login/registro con Google: `POST /api/v1/athletes/auth/google`
- `Implementada` Consulta de atleta por UUID/email, busqueda y metricas simples:
  - `GET /api/v1/athletes/{atletaUuid}`
  - `GET /api/v1/athletes/by-email/{email}`
  - `GET /api/v1/athletes/search`
  - `GET /api/v1/athletes/registered-after`
  - `GET /api/v1/athletes/email-exists/{email}`
  - `GET /api/v1/athletes/stats`
- `Implementada` Actualizacion basica y cambio de password:
  - `PUT /api/v1/athletes/{atletaUuid}`
  - `PUT /api/v1/athletes/{atletaUuid}/password`
- `Recomendada` Reset de password, refresh token y revocacion de sesiones.

### Perfil futbolistico y posiciones

- `Implementada` Crear perfil de jugador: `POST /api/v1/player-profiles`
- `Implementada` Obtener perfil por UUID o alias:
  - `GET /api/v1/player-profiles/{atletaUuid}`
  - `GET /api/v1/player-profiles/by-alias/{alias}`
- `Implementada` Actualizar alias: `PUT /api/v1/player-profiles/{atletaUuid}`
- `Implementada` Gestionar posiciones:
  - `POST /api/v1/player-profiles/positions`
  - `GET /api/v1/player-profiles/{atletaUuid}/positions`
  - `DELETE /api/v1/player-profiles/{atletaUuid}/positions/{positionId}`
  - `PUT /api/v1/player-profiles/{atletaUuid}/positions/{positionId}/experience`
- `Implementada` Trust score basico:
  - `PUT /api/v1/player-profiles/trust-score`
  - `GET /api/v1/player-profiles/{atletaUuid}/trust-history`
  - `GET /api/v1/player-profiles/by-trust-score`
- `Implementada` Busqueda por nombre de atleta: `GET /api/v1/player-profiles/search`
- `Parcial` Trazabilidad completa de trust score con match asociado: el request acepta `matchId`, pero `PlayerProfileService` no lo persiste.
- `Recomendada` Endpoints especificos para estadisticas avanzadas de trust score hoy encapsuladas solo en `TrustScoreService`.

### Catalogos

- `Implementada` Catalogo de posiciones:
  - `GET /api/v1/positions`
  - `GET /api/v1/positions/{id}`
  - `GET /api/v1/positions/search`
- `Implementada` Catalogo de canchas:
  - `GET /api/v1/fields`
  - `POST /api/v1/fields`
  - `PUT /api/v1/fields/{id}`
- `Parcial` Semilla de posiciones inconsistente entre migracion inicial y `DataInitializationService`.

### Equipos

- `Implementada` Crear equipo: `POST /api/v1/teams`
- `Implementada` Upload de logo: `POST /api/v1/teams/logo`
- `Implementada` Listado por jugador o creador:
  - `GET /api/v1/teams/by-player/{playerUuid}`
  - `GET /api/v1/teams/by-creator/{creatorUuid}`
- `Implementada` Miembros activos del equipo: `GET /api/v1/teams/{teamId}/members/active`
- `Implementada` Archivado logico de equipo: `DELETE /api/v1/teams/{teamId}?actorUuid=...`
- `Parcial` No existen endpoints propios para editar equipo, reactivar archivo, remover miembro o cambiar roles.
- `Recomendada` Gestion completa de membresias y admin de equipo.

### Sistema social

- `Implementada` Solicitudes de amistad:
  - `POST /api/v1/social/friendships/requests`
  - `PUT /api/v1/social/friendships/requests/{requestId}/decision`
  - `GET /api/v1/social/friendships/{playerUuid}`
- `Implementada` Invitaciones de equipo:
  - `POST /api/v1/social/team-invites`
  - `PUT /api/v1/social/team-invites/{inviteId}/decision`
  - `GET /api/v1/social/team-invites/{playerUuid}`
- `Implementada` Invitaciones de partido:
  - `POST /api/v1/social/match-invites`
  - `POST /api/v1/social/match-invites/batch`
  - `PUT /api/v1/social/match-invites/{inviteId}/decision`
  - `GET /api/v1/social/match-invites/{playerUuid}`
  - `GET /api/v1/social/match-invites/by-match/{matchId}`
- `Implementada` Notificaciones:
  - `GET /api/v1/social/notifications/{playerUuid}`
  - `PUT /api/v1/social/notifications/{notificationId}/read`
  - `POST /api/v1/social/notifications/reminders/forms/{playerUuid}`
- `Implementada` Busqueda social de jugadores: `GET /api/v1/social/players/search?q=...`
- `Parcial` No hay websocket general ni preferencias de notificacion; solo SSE para invitaciones de match.

### Partidos

- `Implementada` Crear partido: `POST /api/v1/matches`
- `Implementada` Consultar partidos:
  - `GET /api/v1/matches/{matchId}`
  - `GET /api/v1/matches`
  - `GET /api/v1/matches/upcoming`
  - `GET /api/v1/matches/by-player/{playerUuid}`
  - `GET /api/v1/matches/by-player-or-creator/{playerUuid}`
  - `GET /api/v1/matches/by-team/{teamId}`
- `Implementada` Cambiar estado: `PUT /api/v1/matches/{matchId}/status`
- `Implementada` Agregar equipo al partido: `POST /api/v1/matches/{matchId}/teams/{teamId}?esLocal=...`
- `Implementada` Agregar o quitar jugadores:
  - `POST /api/v1/matches/join`
  - `POST /api/v1/matches/{matchId}/teams/{teamId}/players/import`
  - `DELETE /api/v1/matches/{matchId}/players/{playerUuid}`
  - `PUT /api/v1/matches/{matchId}/players/{playerUuid}/confirm`
- `Implementada` Asignacion home/away por jugador: `PUT /api/v1/matches/{matchId}/teams/assignment`
- `Implementada` Eventos del partido:
  - `POST /api/v1/matches/events`
  - `PUT /api/v1/matches/events/{eventId}/confirm`
  - `GET /api/v1/matches/{matchId}/events`
- `Implementada` Vista previa de cierre: `POST /api/v1/matches/{matchId}/close/preview`
- `Implementada` Votacion MVP:
  - `GET /api/v1/matches/{matchId}/mvp`
  - `POST /api/v1/matches/{matchId}/mvp/vote`
- `Implementada` SSE de invitaciones: `GET /api/v1/matches/{matchId}/live`
- `Parcial` Auto-start y auto-invalidacion existen, pero dependen de lecturas de partido; no hay scheduler.
- `Parcial` Confirmacion dual de eventos existe en API, pero `registerEvent` ya cierra los eventos inmediatamente.
- `Recomendada` Endpoints explicitos para cancelacion/reprogramacion de partidos y auditoria de cierres.

### Ratings, OVR y leaderboard

- `Implementada` Actualizacion manual de ratings: `POST /api/v1/ratings/update`
- `Implementada` Modo arquero rotativo: `POST /api/v1/ratings/update-rotative-goalkeeper`
- `Implementada` Inicializacion base por jugador: `POST /api/v1/ratings/player/{playerProfileId}/initialize-base`
- `Implementada` Consulta de ratings:
  - `GET /api/v1/ratings/player/{playerProfileId}`
  - `GET /api/v1/ratings/player/{playerProfileId}/role/{roleType}`
  - `GET /api/v1/ratings/player/{playerProfileId}/priority/{priorityLevel}`
- `Implementada` Historial y estadisticas:
  - `GET /api/v1/ratings/player/{playerProfileId}/history`
  - `GET /api/v1/ratings/player/{playerProfileId}/history/role/{roleType}`
  - `GET /api/v1/ratings/player/{playerProfileId}/history/period`
  - `GET /api/v1/ratings/player/{playerProfileId}/statistics`
  - `GET /api/v1/ratings/player/{playerProfileId}/statistics/role/{roleType}`
- `Implementada` OVR completo: `GET /api/v1/ratings/player/{playerProfileId}/overall`
- `Implementada` Leaderboard: `GET /api/v1/ratings/leaderboard`
- `Implementada` Actualizacion automatica al finalizar partido desde `MatchService`.
- `Implementada` La ruta `/api/v1/ratings/**` requiere JWT por `SecurityConfig`; `JwtAuthenticationIntegrationTest` cubre lectura y escritura sin token.
- `Recomendada` Versionado de formulas de rating y trazabilidad de reglas activas por fecha.

### Smoke de contratos HTTP backend

- `Implementada parcial` Existe `ApiContractSmokeTest` con `@WebMvcTest` para contratos HTTP usados por frontend.
- Cubre rutas principales de auth, teams, matches/MVP y ratings, validando status y campos JSON criticos.
- Pendiente opcional: smoke E2E contra frontend y backend levantados con datos/credenciales estables.

## Reglas de negocio detectadas

- Email unico global por atleta.
- `PlayerProfile` es 1:1 con `Athlete`.
- Alias de jugador unico si se informa.
- Un jugador no puede repetir prioridad ni posicion en `player_positions`.
- Equipo con nombre unico.
- Al crear equipo se crea `TeamStats` y membresia activa del creador como `CAPITAN`.
- Un partido admite maximo 2 equipos y cupos por modalidad 5v5, 6v6 o 7v7.
- No se puede unir un jugador dos veces al mismo partido.
- Creador o capitanes son los responsables autorizados a registrar/confirmar/finalizar eventos a nivel de servicio.
- Cierre de partido invalida si no se alcanza el minimo de confirmados.
- Votacion MVP solo para participantes confirmados y dentro de ventana de 3 horas.
- En categoria `MIXTO`, la asignacion home/away exige balance de genero con diferencia maxima de 1.

## Integraciones detectadas

- Google OAuth `tokeninfo`
- PostgreSQL
- Flyway
- OpenAPI/Swagger
- Spring Actuator
- Prometheus/Micrometer
- SSE para cambios de invitaciones

## Consideraciones de seguridad

- El backend autentica, pero no siempre autoriza por identidad real del token.
- `ratings/**` no queda publico por defecto y tiene cobertura de integracion; sigue pendiente autorizacion fina por identidad/dominio en endpoints sensibles.
- En `dev` y `test`, Swagger y actuator quedan abiertos.
- El upload de logos no valida contenido binario real.
