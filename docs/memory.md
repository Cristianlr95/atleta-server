# Memoria Viva del Backend Atleta

## Vision general

Backend monolitico en Java 21 con Spring Boot 3.3.2 orientado a gestionar identidad de atletas, perfiles futbolisticos, equipos, partidos, rating/OVR, XP, invitaciones sociales y notificaciones internas.

El repositorio implementa una API REST sobre PostgreSQL usando Spring Data JPA y Flyway. La seguridad combina registro local con BCrypt, autenticacion Google OAuth y JWT firmado con HS256.

## Avance porcentual

- Avance estimado del proyecto Atleta: 100%.
- Avance anterior registrado: 99%.
- Delta de esta tarea: +1 punto porcentual por extraer consultas/listados de partidos a `MatchQueryService`, dejando `MatchService` enfocado en comandos/orquestacion y cubriendo el servicio con tests unitarios.

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
- `mvnw test` queda verde en backend despues de adaptar tests protegidos a JWT y limpiar tablas nuevas de social/notificaciones entre casos.
- `ApiContractSmokeTest` cubre contratos HTTP backend consumidos por el frontend para auth, player profiles/trust score, teams, matches/MVP y ratings sin levantar base de datos.
- `ApiContractSmokeTest` tambien verifica que creacion/cierre/asignacion/eventos/MVP usen el `sub` del JWT, que crear perfil/trust score usen el usuario autenticado, que rutas personales de perfil y ratings rechacen IDs de terceros, que borrar equipo rechace un `actorUuid` ajeno y que las invitaciones por partido usen visor/requester autenticado.
- `JwtAuthenticationIntegrationTest` verifica que `ratings/leaderboard` y `ratings/update` rechacen requests sin JWT, y que leaderboard acepte un token valido.
- `JwtAuthenticationIntegrationTest` tambien protege que lecturas globales (`positions`, `fields`, `matches/upcoming`, busquedas de perfiles y rango de trust score) rechacen requests sin JWT y acepten token valido.
- `EnvExampleContractTest` valida que `.env.example` documente variables runtime criticas, que `DB_PASSWORD`/`JWT_SECRET` no sean triviales y que `docker-compose.yml` falle rapido si faltan variables de base de datos.
- `PlayerProfileControllerIntegrationTest` verifica que `PUT /api/v1/player-profiles/trust-score` persiste `matchId` en `trust_logs` y que el historial devuelve `match.id`.
- `MatchStatusSchedulerTest` verifica que el scheduler delega en `MatchService.refreshAutomatedMatchStates()`; el job queda deshabilitado en tests para evitar flakiness.
- `MatchStatusPolicyTest` cubre transiciones validas/invalidas, metadata de inicio/invalidez manual y ventana de cierre pendiente sin depender de repositorios.
- `MatchFinalScoreServiceTest` cubre el conteo de goles confirmados por lado y la persistencia del snapshot de marcador en `MatchTeam`/`Match`.
- `MatchPlayerHistoryServiceTest` cubre generacion de `PlayerHistory`, resultado del jugador y acumulacion de XP por posicion al finalizar partido.
- `MatchPendingEventClosureServiceTest` cubre confirmacion automatica home/away de eventos pendientes y aplicacion de goles nuevos al marcador del equipo.
- `MatchRosterPolicyTest` cubre cupos por modalidad, bloqueo de asignaciones al iniciar/finalizar y validaciones de convocatoria por genero.
- `MatchPostMatchRatingServiceTest` cubre armado de `PlayerPerformanceDto` para jugadores confirmados y omision de rating cuando el partido no tiene dos equipos.
- `MatchAutomatedStatusServiceTest` cubre inicio automatico, invalidacion de finalizados inconsistentes y preservacion cuando ya existe historial.
- `MatchStatusSchedulerTest` verifica que el scheduler delega en `MatchAutomatedStatusService`.
- `MatchResponseMapperTest` cubre armado de `MatchResponse`, datos anidados, fallback del creador como capitan y `closePending`.
- `MatchQueryServiceTest` cubre consultas/listados de partidos, merge por jugador/creador, refresh automatico y eventos sin refresh.
- `TeamServiceTest` cubre upload de logo PNG valido y rechazo de MIME falsificado cuando la firma binaria no coincide.
- `TrustScoreServiceTest` cubre limites inferiores/superiores de trust score y que `trust_logs.cambio` guarde el delta efectivo; `PlayerProfileControllerIntegrationTest` valida que el endpoint usa el JWT aunque el body omita `playerUuid`.

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
- Cierre de partidos orquestado por `MatchService`, con snapshot final, historial/XP, cierre de eventos y rating post-partido delegados a servicios dedicados.
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
- `ratings/**` ya no queda publico por defecto: las reglas reales terminan en `anyRequest().authenticated()` y hay cobertura de integracion que lo confirma.
- Politica de privacidad vigente: solo registro/login/Google auth, health check y docs/actuator en dev/test son publicos; catalogos, busquedas, listados, leaderboard y recursos deportivos requieren JWT.
- Las rutas personales de ratings (`player/{playerProfileId}`, history, stats, overall e initialize-base) validan que el `playerProfileId` coincida con el `sub` del JWT.
- La actualizacion manual de ratings via controller rechaza performances cuyo `playerProfileId` no coincide con el usuario autenticado.
- La consulta de invitaciones por partido valida que el visor autenticado sea creador, participante o actor de alguna invitacion.
- La creacion de invitaciones de partido valida que el requester autenticado sea creador o participante del partido, y rechaza `teamId` que no pertenezca al match.

## Aprendizajes tecnicos relevantes

- El backend evoluciono por capas sin romper el modelo base: atletas -> player profile -> equipos/partidos -> rating/social.
- Hay esfuerzo fuerte en testing y migraciones, pero menos rigor en autorizacion fina.
- El dominio de partido ya contempla ventanas temporales, validacion de cierre, MVP y categoria por genero.
- Se detecta coexistencia de documentacion antigua y codigo nuevo; el codigo es la fuente confiable.
- Los contratos FE-BE criticos ya tienen doble proteccion: smoke unitario en `atleta-app` para rutas cliente y smoke MVC en backend para rutas/respuestas principales.
- Los tests de integracion legacy ya inyectan JWT real o `SecurityMockMvcRequestPostProcessors.jwt()` en endpoints protegidos, evitando falsos 500 por `@AuthenticationPrincipal Jwt` nulo.

## Errores, riesgos y hallazgos

### Riesgos prioritarios post-100

- Medio: quedan endpoints protegidos con parametros UUID heredados y autorizacion fina por rol de negocio; la politica publico/privado para lecturas globales ya quedo fijada como privada bajo JWT.
- Bajo: `application-dev.yaml` mantiene defaults de desarrollo para usuario/base local, pero `.env.example` y `docker-compose.yml` ya tienen contrato automatizado para variables runtime y secretos no triviales.
- Bajo: `TeamService.storeTeamLogo` valida tamano, MIME permitido y firma binaria PNG/JPEG/WEBP; sigue pendiente antivirus/re-encode y politica de exposicion de `/uploads/**`.
- Bajo: el pipeline mantiene test/build automaticos; deploy staging/produccion queda deshabilitado salvo ejecucion manual con `ATLETA_ENABLE_REAL_DEPLOY=true` hasta implementar proveedor, URL y rollback reales.
- Bajo: Dockerfile y compose local/CI existen; falta validar el flujo con Docker instalado en el entorno de desarrollo/CI.

### Riesgos funcionales y de consistencia post-100

- `DataInitializationService` y la migracion `V001` no comparten exactamente el mismo catalogo de posiciones.
- `registerEvent` cierra eventos inmediatamente con confirmacion home/away para evitar bloqueos, reduciendo el valor real del flujo de confirmacion.
- Existen docs antiguas que contradicen el codigo actual; por ejemplo, partes del README y de `src/main/resources/db/README.md`.

## Deuda tecnica detectada

- Autorizacion por identidad esta reforzada en flujos principales de equipos, partidos, eventos, MVP, social por partido y ratings personales; queda como hardening post-100 completar roles de negocio avanzados en operaciones secundarias.
- `MatchService` ya delega politica de estado, snapshot final, historial/XP, cierre de eventos pendientes, politica de convocatoria/equipos, rating post-partido, automatizacion de estados, DTO mapping de respuestas y consultas/listados; `RatingService` queda como candidato de optimizacion post-100.
- Automatizacion de estados ya existe via scheduler y `MatchAutomatedStatusService`; resta validarla con datos reales de entorno.
- Falta implementar el despliegue real; CI ya evita ejecutar migraciones/deploy productivo automaticamente mientras no exista proveedor configurado.
- Falta estrategia centralizada de manejo de errores para todos los modulos sociales/equipos.
- Falta normalizacion documental: hay varios `.md` desactualizados.

## Proximos pasos post-100 recomendados

1. Validar con datos reales de entorno la automatizacion temporal y los flujos sociales completos.
2. Agregar smoke E2E opcional contra frontend y backend levantados cuando existan credenciales/seed estables.
3. Implementar deploy real con proveedor, URL de health check y rollback antes de habilitar `ATLETA_ENABLE_REAL_DEPLOY`.
4. Completar autorizacion por rol de negocio en lecturas/administracion donde no baste con identidad JWT.
5. Reducir docs heredadas contradictorias y mantener una fuente de verdad por flujo operativo.
