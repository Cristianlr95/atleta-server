# Memoria Viva del Backend Atleta

## CAP-03A · Archivado manual por el capitán vigente · 23/11/2026

- Base de beta `adcdfa8`, con CAP-03 y CAP-05. Los commits locales SEC-01 y SEC-02 quedan en sus ramas. `DELETE /api/v1/teams/{teamId}?actorUuid=...` conserva el parámetro y exige que coincida con el JWT. El servicio bloquea el equipo y autoriza por membresía activa `CAPITAN`.
- El capitán activo recibe `204`, incluido un reintento sobre el equipo ya archivado. La autorización se comprueba antes del reintento. El equipo inexistente devuelve `404`, la ausencia de sesión `401` y un actor autenticado sin el cargo `403`.
- La transferencia conserva las invitaciones pendientes. El archivado conserva el historial deportivo y mantiene la limpieza existente de invitaciones de equipo y de partido. El archivado por baja de cuenta conserva su regla.
- En PostgreSQL 18.1 aislado, RED dio tres fallos esperados de cinco casos. GREEN pasó 157 pruebas, sin fallos ni omisiones, y generó el JAR con `clean package`. Se verificaron la matriz con JWT firmado, la concurrencia y el rollback del archivado. La regresión completa H2 pasó 798 pruebas con 4 omisiones dependientes de PostgreSQL.
- CAP-03C debe actualizar el botón y la validación en Perfil (`profile-teams` y `PlayerProfilePage.onDeleteTeam`), Social (`TeamsPanelComponent` y `SocialFacadeService.createdTeams`), y la variante de Inicio que reutiliza el panel de equipos. Los controles deben consultar el cargo por equipo, actualizarse tras la transferencia y recuperar el estado ante un `403`.

## CAP-06 · QA con sesión autenticada · 21/11/2026

- Bases de beta verificadas en GitHub: backend `64b92f8` y frontend `7033ffd`. La prueba local inicia sesión con tres cuentas sintéticas en `/api/v1/athletes/login` y usa los JWT firmados emitidos por Atleta a través de los filtros de seguridad de MockMvc.
- El caso pasó en H2 y PostgreSQL 18.1. Comprueba el rechazo sin token, la transferencia del fundador al sucesor, los permisos de invitación antes y después del cambio, la expulsión, el abandono y el registro del actor en V030.
- CI ejecuta `CaptainMigrationPostgresTest` contra su servicio PostgreSQL desechable. El nuevo caso parte de V029 con un capitán, un jugador y dos DT (uno inactivo), aplica V030 y V031 y comprueba que se conservan los roles y estados, se marca a ambos DT y el historial de transferencias empieza vacío. Pasaron los siete casos de migración y el recorrido JWT en PostgreSQL 18.1.
- La página pública de Cloudflare respondió HTTP 200 y sirvió los módulos de Plantel y Ajustes con abandono, función de DT y el aviso al eliminar una cuenta. Render devolvió HTTP 200 y `UP` en salud y readiness tras el primer intento agotado. Estas respuestas públicas no identifican el SHA desplegado ni comprueban el estado de Flyway en Neon. GitHub Actions de frontend pasó y el de backend seguía ejecutándose.
- Quedan pendientes la verificación de la revisión desplegada y Flyway en Neon, la QA autenticada en beta y la revisión visual en navegador y teléfono.

## CAP-05 · Abandono, auditoría de transferencias y función de DT · 21/11/2026

- `DELETE /api/v1/teams/{teamId}/members/me` desactiva la membresía del actor autenticado. El capitán recibe `409` hasta transferir la capitanía. El historial deportivo se conserva.
- V030 añade `team_captain_transfers`. La transferencia y su registro se confirman en la misma transacción.
- Decisión del usuario: al ceder el cargo, el ex-capitán pasa a `JUGADOR` y conserva su función de DT si la tenía. El listado activo devuelve ambas cosas con `rol` y `esDt`. La protección de DT frente a la expulsión sigue aplicando con ambos valores.
- V031 añade la función independiente y marca las membresías existentes con rol `DT`. Las migraciones se entregan juntas para aplicar V030 antes de V031. La validación integral de beta queda pendiente.
- Pasaron 62 pruebas dirigidas del paquete en PostgreSQL 18.1.

## CAP-05 · Anonimización de una cuenta con capitanía · 21/11/2026

- Decisión confirmada por el usuario: transferir la capitanía antes de eliminar la cuenta si existe otro integrante activo con cuenta disponible. En un equipo sin otro integrante disponible, el borrado archiva el equipo en la misma transacción. Una membresía de una cuenta ya anonimizada sigue en el historial.
- `DELETE /api/v1/athletes/{atletaUuid}` devuelve `409 CAPTAIN_TRANSFER_REQUIRED` antes de archivar o anonimizar si queda una capitanía que transferir. Alta, transferencia y borrado coordinan el acceso a la cuenta. La aceptación de invitaciones y el archivado coordinan los cambios de plantel mediante el bloqueo de equipo.
- Pasaron 37 pruebas dirigidas en H2 y 62 contra PostgreSQL 18.1, incluidos los casos de varios equipos y de concurrencia.

## CAP-05 · Archivado concurrente de equipo · 21/11/2026

- Base local `f70cf81`. El archivado toma el mismo bloqueo de fila que la transferencia de capitanía y los cambios de membresía, antes de comprobar el estado del equipo y limpiar las invitaciones. Si una invitación se confirma primero, el archivado la limpia. Si el archivado se confirma primero, una nueva transferencia o invitación se rechaza al comprobar el estado bajo el bloqueo.
- El fundador conserva el permiso de archivar incluso después de transferir la capitanía. El cambio queda preparado para revisión local.
- Pasaron 21 pruebas dirigidas en H2 y 23 en PostgreSQL 18.1, incluidos los dos órdenes entre archivado e invitación y el rechazo de una transferencia que espera al archivado.

## CAP-02 · Protección local preparada para revisión · 20/11/2026

Base `3aa2785`. El [inventario de lectura](../scripts/inventory-team-captains.sql) incluye los equipos archivados con más de una membresía de capitán activa. En `atleta_dev`, PostgreSQL 18.1 en V028, los 8 equipos activos tienen un capitán. Hay 0 equipos activos sin capitán, 0 activos con duplicados y 0 archivados con duplicados. El inventario manual de Neon beta (production / neondb) informó 0 activos sin capitán, 0 activos con duplicados y 0 archivados con duplicados.

La [migración Flyway V029](../src/main/java/db/migration/V029__protect_active_team_captains.java) toma un bloqueo de escritura en las dos tablas durante la comprobación y la creación del índice. Informa por separado los tres recuentos anteriores y aborta si cualquiera es positivo. Las asignaciones incompatibles requieren decisión antes de reintentar. El índice PostgreSQL `uq_team_active_captain` da unicidad a `team_members(team_id) WHERE activo AND rol = 'CAPITAN'`, incluso para equipos archivados. Un equipo archivado puede conservar cero o un capitán activo y cualquier número de membresías inactivas. Flyway descubre la versión Java compilada en la ubicación habitual; en H2 la misma migración usa una columna generada anulable con índice único para mantener la prueba de repositorio.

**Escrituras.** El alta ordinaria guarda equipo y fundador en una transacción. La aceptación de invitaciones incorpora jugadores y el archivado excluye al equipo del conjunto activo. La reactivación existente pertenece al [seed de desarrollo](../src/main/java/com/atleta/demo/config/DevDataInitializer.java): ahora bloquea la fila del equipo existente para escritura y comprueba su capitán antes de cambiar roles o reabrirlo. Si el capitán está ausente o cambió, se rechaza la transacción completa. Para dos altas simultáneas del mismo equipo de desarrollo, la unicidad existente del nombre protege la creación. El índice serializa las promociones competidoras para el mismo equipo. Una escritura SQL directa aún puede retirar al último capitán.

**Requisito para CAP-05.** Transferencia, abandono y expulsión deberán adquirir el mismo bloqueo de fila de equipo, comprobar de nuevo actor y membresías, escribir la sustitución y comprobar la existencia del capitán antes de confirmar. Una operación que deje al equipo activo sin capitán deberá revertirse. No existe actualmente un endpoint para esas tres operaciones.

**Evidencia PostgreSQL.** Base local desechable creada desde `template0`, con el driver y dialecto PostgreSQL, perfil `test`, Flyway habilitado y `SPRING_FLYWAY_LOCATIONS=classpath:db/migration`. Se configuraron las variables de datasource de Spring para esa base y `CAP02_TEST_JDBC_URL` con la misma URL JDBC. Desde la raíz de `atleta-server`:

```powershell
.\mvnw.cmd -B '-Dtest=CaptainMigrationPostgresTest,TeamControllerIntegrationTest#testCreateTeam_Success,DevTeamCaptainTest' test
```

Pasaron 9 casos: seis combinaciones de equipo activo/archivado con cero, uno o dos capitanes en V028; un alta autenticada del fundador; dos rechazos del seed al reabrir un equipo con el capitán ausente o sustituido. Los casos incompatibles conservaron V028. En los compatibles se aplicó V029, se admitieron capitanes inactivos y el segundo capitán activo dio `23505`. En dos sesiones PostgreSQL, un segundo bloqueo de la misma fila con `NOWAIT` dio `55P03`; una inserción concurrente de capitán esperó el commit de la primera, recibió `23505` y quedó un solo capitán activo. Flyway advierte que su versión se probó hasta PostgreSQL 16. La validación realizada aquí corresponde a PostgreSQL 18.1.

El comando `mvnw.cmd -B -Dflyway.locations=classpath:db/migration flyway:validate`, con la conexión Flyway apuntando a la base desechable, validó las 29 migraciones. La suite habitual `mvnw.cmd -B test` informó 747 casos, cero fallos, cero errores y una omisión correspondiente a la clase PostgreSQL que se ejecutó por separado. Se retiró la base desechable y se verificó que `atleta_dev` continuaba en V028.

## CAP-02 · Inventario y reproducción PostgreSQL · 20/11/2026

Revisión local de `main` en `3aa2785`. El inventario considera activo un equipo con `teams.archived IS FALSE` y cuenta filas de membresía con `activo IS TRUE` y `rol = 'CAPITAN'`. El [inventario SQL](../scripts/inventory-team-captains.sql) usa una transacción de solo lectura y devuelve recuentos agregados.

| Entorno | Equipos activos | Cero | Uno | Más de uno | Inválidos con fundador activo | Candidato inequívoco | Revisar asignación |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `atleta_dev`, PostgreSQL 18.1, Flyway V028 | 8 | 0 | 8 | 0 | 0 | 0 | 0 |
| Base desechable, PostgreSQL 18.1, tres equipos sintéticos | 3 | 1 | 1 | 1 | 2 | 1 | 1 |

El inventario beta queda pendiente. La sesión tenía configurada la conexión local. Con un servicio libpq de lectura de beta ya configurado, ejecutar `psql -X -d 'service=<servicio-de-lectura>' -f scripts/inventory-team-captains.sql`. La consulta clasifica filas de membresía activas. Para proponer al fundador comprueba que exista una sola membresía activa suya, con rol `JUGADOR`, y que su cuenta no esté anonimizada.

- **Aplicación.** El alta persiste equipo, estadísticas y fundador activo como `CAPITAN` dentro de la transacción de `TeamService`. Aceptar una invitación añade un `JUGADOR` y deja intacto el número de capitanes. Archivar retira al equipo del conjunto activo. El inicializador exclusivo de desarrollo escribe roles directamente, puede reabrir el equipo que siembra y puede sumar un segundo capitán si ya hay otro fuera de las membresías gestionadas por el seed. Anonimizar una cuenta conserva su membresía, por lo que el capitán puede seguir contando aunque ya no pueda actuar. [Alta y archivado](../src/main/java/com/atleta/demo/service/TeamService.java#L79) · [Aceptación](../src/main/java/com/atleta/demo/service/SocialService.java#L173) · [Seed](../src/main/java/com/atleta/demo/config/DevDataInitializer.java#L116) · [Anonimización](../src/main/java/com/atleta/demo/service/AthleteService.java#L218)
- **Persistencia.** La V001 define claves externas y el catálogo de roles. La V017 añade `archived`. En `atleta_dev` se inspeccionaron restricciones, índices y triggers: `team_members` solo tiene su índice primario y no hay triggers de usuario en las dos tablas. SQL directo puede insertar un equipo activo sin membresías o dejarlo sin capitán al eliminar, desactivar, mover o degradar al único. Insertar, promover, mover o reactivar otra membresía puede sumar un segundo capitán. La [prueba de repositorio existente](../src/test/java/com/atleta/demo/repository/TeamMemberRepositoryTest.java#L391) modifica un rol y acepta dos capitanes. Usa por defecto H2 en modo PostgreSQL y no pasa por el alta de `TeamService`.
- **Prueba dirigida.** Se copió solo el esquema `public` de `atleta_dev` con `pg_dump --schema-only --schema=public --no-owner --no-privileges` y se restauró en una base local desechable creada desde `template0`, omitiendo la declaración redundante de `public`. Se ejecutó `mvnw.cmd -B -Dtest=Cap02PostgresProbeTest test` con `@SpringBootTest`, perfil `test`, el driver y dialecto PostgreSQL y el datasource apuntando a esa base. Flyway estuvo deshabilitado y su bean fue simulado solo para satisfacer la dependencia de arranque. El historial Flyway de esa base quedó en cero filas. Pasaron **3 pruebas**. Las tres invocaron `TeamService.createTeam` y comprobaron con JDBC que la única membresía de capitán activa pertenecía al fundador. En dos pruebas se insertó por SQL un segundo capitán de otro jugador o se degradó al único a `JUGADOR`, verificando respectivamente dos y cero capitanes activos. El caso de cero comprobó además que el equipo seguía activo. El código de la prueba temporal se retiró y el resultado local quedó en `target/surefire-reports/com.atleta.demo.service.Cap02PostgresProbeTest.txt`.

**Siguiente cambio mínimo.** Completar el inventario beta antes de diseñar el saneamiento. Conservar al capitán único existente. Para equipos sin capitán, el fundador es un candidato determinista cuando su cuenta sigue disponible y tiene una única membresía activa como jugador. Resolver explícitamente los equipos con varios capitanes, fundador ausente, inactivo, anonimizado, duplicado o con rol `DT`. Un índice único parcial sobre `team_members(team_id)` con predicado `activo IS TRUE AND rol = 'CAPITAN'` impediría dos capitanes activos. Abarcaría también las membresías que sigan activas en equipos archivados, por lo que habrá que comprobar esos datos antes de crearlo. Para impedir cero desde la aplicación, serializar por equipo y comprobar el invariante al cerrar la transacción de alta, transferencia y cambios de membresía o reapertura. Si se exige la garantía también para escrituras SQL directas, hace falta un control adicional diferido al cierre de la transacción. Quedan pendientes el diseño, la prueba de rollback del alta y la validación de concurrencia.

## Integración local de Ranking · 17/11/2026

- La rama `feature/ranking-stats-20261117` se incorporó por avance directo a `main` local en `9a913c5`; frontend también integrado. Tras la integración pasaron 36 pruebas dirigidas de repositorio y servicios de Ranking. Sin push ni despliegue; validación beta y datos reales pendientes.

## Revisión de XP independiente · 17/11/2026

- `XPService` no tiene consumidores de producción identificados. El cierre y su preview usan `MatchXpCalculator`; las fórmulas difieren (victoria y gol delantero: 30 frente a 14 XP), y el motor independiente no aplica el límite 25. Límites numéricos, negativos y posiciones requieren resolución antes de conectarlo. [Detalle](revision-xp-independiente-2026-11-17.md). No se cambiaron reglas ni datos; balance pendiente con datos reales.

## Ranking: MVP y resultados · 17/11/2026

- Implementación local en `feature/ranking-stats-20261117`: agrega goles, ganador MVP oficial y hasta cinco resultados por trayectoria/posición/equipo desde `PlayerHistory` de partidos finalizados. Dos consultas en lote, con ventana por jugador y posición para limitar resultados. Histórico sin clasificación deja estadísticas por posición desconocidas. 739 pruebas backend H2 y dos pruebas de repositorio PostgreSQL 18 con datos sintéticos aprobadas; Flyway advierte que su soporte probado llega a PostgreSQL 16. Sin auditoría de datos históricos, beta ni publicación.

## Ranking: goles · 17/11/2026

- Avance local BE-RANK-01 en `feature/ranking-stats-20261117`: suma agrupada por jugador y posición de `PlayerHistory` finalizado, global o por equipo; rating por múltiples roles no duplica eventos. En posición se informa trayectoria y posición; posición histórica no clasificable conserva null para ese alcance. 66 pruebas backend dirigidas correctas en H2. MVP, resultados, PostgreSQL y beta pendientes.

## BE-PLANTEL-03 · 17/11/2026

- Se comprobó el contrato existente: historial de partidos finalizados, generado al cierre para participantes confirmados y eventos confirmados; cero, ausencia y errores separados. Lectura de integrantes activos requiere JWT, sin restricción de membresía en la política actual. Pruebas añadidas para jugador/evento sin confirmar y rechazo sin sesión; registros antiguos y beta no auditados.

## PJ con el equipo · 17/11/2026

- El DTO de integrantes activos añade `teamMatches`: cuenta partidos finalizados distintos con historial del jugador para el equipo solicitado. Sin historial de ese equipo devuelve null. Se reutiliza el agregado existente, sin migración ni escritura histórica; incluye las participaciones históricas registradas, incluso anteriores a una salida y reingreso. Beta y PostgreSQL pendientes.

## Asistencias del plantel · 17/11/2026

- El agregado de integrantes activos incorpora asistencias con el equipo y de trayectoria desde el mismo historial de partidos finalizados; respeta cero y ausencia. Sin migración ni escritura histórica. Verificación beta pendiente.

## Plantel: goles por equipo y trayectoria · 17/11/2026

- Suite completa en esta rama: 731 pruebas, cero fallos, errores u omisiones (JDK 21/H2). La cifra corresponde a esta base y no se suma a inventarios históricos.

- El usuario eligió ambos valores. `GET /api/v1/teams/{teamId}/members/active` añade `teamGoals`/`totalGoals`, sumados en una consulta de PlayerHistory para integrantes activos y partidos FINALIZADO. Equipo = equipo registrado en cada participación, incluso antes de una salida/reingreso; trayectoria = todos los equipos. Sin historial para el alcance, null; cero registrado se conserva.
- Se mantiene la política JWT existente para lecturas deportivas; no se entregan historiales privados ni se cambian permisos de administración. Sin migraciones, escrituras históricas o recálculos de OVR. El plantel vacío evita consultar agregados.
- Pruebas dirigidas de repositorio H2, servicio y contrato MVC aprobadas (54). Frontend derivado de 1e1ddd8 consume ambos valores en tabla/modal; 272 unitarias y dos E2E simulados aprobados. Rama backend `feature/team-goal-totals-20261117`, derivada de 7cf72ef. Sin publicación; PostgreSQL y datos reales no verificados. Asistencias y beta permanecen pendientes.

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

- El inicializador de posiciones vacias coincide con el catalogo creado por `V001` y `V009`; no modifica catalogos existentes.
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

## Actualizacion 2026-10-29
- Se sustituyo JUnit DisplayName por jqwik Label en 32 propiedades que se omitian; las ocho clases afectadas ejecutaron 32 pruebas sin fallos ni omisiones con JDK 21.
- Suite completa Maven en JDK 21: 700 pruebas, cero fallos, errores u omisiones. Se ejecuto fuera del sandbox por la restriccion de loopback; Docker no estaba disponible y el alcance de las integraciones depende de su configuracion de prueba.
- Catalogo de posiciones: el fallback para una tabla vacia ahora coincide con V001 y V009; dos pruebas cubren inicializacion y preservacion del catalogo existente. No se alteraron datos historicos.
- Suite completa tras alinear el catalogo: 702 pruebas aprobadas, cero fallos, errores u omisiones (Maven offline, JDK 21).
- Recuperacion de contrasena: prueba integrada con correo simulado y solicitudes separadas verifica respuesta generica, enlace de un uso, cambio de contrasena y revocacion de access/refresh. Suite completa: 703 pruebas aprobadas, cero fallos, errores u omisiones. No verifica entrega SMTP real ni navegador.
- Arquero rotativo: usuario autorizo bloquear temporalmente POST /ratings/update-rotative-goalkeeper; una llamada valida autenticada recibe 403 sin invocar RatingService. El servicio anterior consulta ratings globales y no debe reconectarse hasta validar autorizacion, participantes confirmados, resultado, reprocesos y concurrencia. No se modificaron ratings existentes. Suite Maven: 705 pruebas aprobadas, cero fallos, errores u omisiones.
- Decision del usuario 2026-10-29: el modo de arquero rotativo se debe elegir al crear el partido. Aun falta persistir la opcion e implementar el calculo seguro por participantes confirmados y resultado verificado; el endpoint permanece bloqueado.
- Ratings tras cierre entre dos equipos: se usa el resultado correspondiente al equipo del jugador y se omite con aviso una asignacion ajena a los equipos del partido. Pruebas de victoria local/visitante y empate, mas cierre con servicio simulado; suite completa: 708 pruebas aprobadas sin fallos, errores ni omisiones. No se recalcularon datos historicos.

## Actualización 2026-10-29: arquero rotativo

La modalidad se elige al crear el partido y queda persistida con valor predeterminado falso (migración V026). Al finalizar un partido marcado, el cálculo usa el marcador y los participantes confirmados, respeta el resultado de cada lado y evita duplicar el conteo del partido; los cierres concurrentes se serializan. El endpoint manual inseguro permanece bloqueado. No se recalcularon datos históricos.

Verificación local: 715 pruebas backend sin fallos ni omisiones, incluidas pruebas de persistencia, cierre y concurrencia con H2. PostgreSQL, producción y dispositivos físicos siguen sin verificar.

## Actualización 2026-10-30: PostgreSQL local

Se aplicaron las migraciones a una base temporal PostgreSQL 18.1; V026 se registró correctamente y la columna `rotative_goalkeeper` quedó no nula con valor predeterminado falso. Las dos pruebas de integración de cierre de arquero rotativo, incluida la concurrencia, pasaron contra esa base. Flyway advirtió que su soporte probado llega a PostgreSQL 16. La instancia y los datos temporales se eliminaron al terminar. No se verificó producción ni PostgreSQL remoto.

## Actualización 2026-10-30: cierre real desde navegador

El recorrido local de arquero rotativo creó y finalizó un partido 1–0 en PostgreSQL temporal: cinco ratings rotativos con resultado GANADO para LOCAL y cinco con PERDIDO para VISITA, diez historiales de jugador y un gol confirmado por ambos lados. Las invitaciones se aceptaron por API; el inicio, marcador y cierre se realizaron desde la UI. Solo el inicio del partido sintético se ajustó a dos horas antes para probar la ventana de cierre. Base temporal eliminada después de la comprobación.

## Actualización 2026-10-30: diagnóstico de rating histórico

Actualización 2026-11-02: la búsqueda de archivos de respaldo en el proyecto y de nombres de respaldo de Atleta bajo Proyectos no identificó una copia de datos históricos. El análisis real sigue pendiente de una copia PostgreSQL autorizada y acceso de solo lectura; los diagnósticos sintéticos previos no lo sustituyen.

Se preparó `scripts/audit-internal-rating-results.sql`, consulta agregada en transacción de solo lectura para señalar posibles discrepancias de resultado en partidos internos y casos sin lado determinable. Se probó con datos sintéticos en PostgreSQL 18: cuatro registros examinados, uno indeterminable y uno discrepante; se excluyeron un partido externo y un rating rotativo. No se consultó ninguna base real ni se recalcularon ratings. Sus límites y ejecución en copia están documentados en `docs/auditoria-ovr-historico.md`.

## Actualización 2026-10-30: diagnóstico de equipos opuestos

Se añadió `scripts/audit-opposing-team-rating-results.sql` para contar posibles discrepancias de resultados en amistosos y partidos por puntos, con resultados indeterminables separados. PostgreSQL temporal: cinco registros sintéticos examinados, dos indeterminables y uno discrepante; partido interno y rating rotativo excluidos. No se consultó una base real ni se modificaron calificaciones. Límites en `docs/auditoria-ovr-historico.md`.

- 2026-10-31: diagnosticado error 500 en lectura de calificaciones tras onboarding: conversión de alias accedía a PlayerProfile diferido con open-in-view desactivado. Las consultas de ratings por jugador, rol y prioridad ahora cargan el perfil en la consulta. Prueba de integración sin sesión abierta reprodujo el fallo y pasó tras la corrección; pruebas dirigidas: 31 aprobadas.
La suite completa del backend se inició después de la corrección y se interrumpió durante las integraciones por su duración; no se declara aprobada. La evidencia de 31 pruebas dirigidas sigue vigente.

- 2026-11-01: revisión acotada de permisos con dos cuentas sintéticas y H2 temporal confirmó: rating propio alterable con partido ajeno en estado CREADO, trust score y XP propios modificables por el usuario, y edición/desactivación de una cancha por otra cuenta. Evidencia y límites en docs/revision-permisos-2026-11-01.md. No se aplicaron restricciones; corrección pendiente. Backend temporal detenido.

- 2026-11-01: se bloquean temporalmente en SecurityConfig POST ratings/update y PUT trust-score, XP por posición y fields/{id}: 403 autenticado, 401 sin sesión. El cierre usa servicios internos; creación y lectura de canchas continúan. 21 pruebas dirigidas aprobadas, incluida seguridad y cierre rotativo. Pendientes reglas definitivas y despliegue; integraciones de las rutas bloqueadas se verán afectadas.

- 2026-11-02: verificación de recuperación de contraseña con backend Spring/H2 y receptor SMTP locales: cuenta desconocida no generó mensaje; cuenta ficticia recibió un mensaje con enlace, que permitió cambiar la contraseña una vez. Se rechazó la reutilización y se revocó la sesión anterior; ingreso con contraseña nueva correcto. Los servicios temporales se detuvieron. No se envió correo externo ni se verificó proveedor SMTP real, TLS o notificación remota.

- 2026-11-02: la suite completa reveló datos residuales de la prueba de lectura de calificaciones que impedían limpiar perfiles en pruebas de repositorio. Se añadió limpieza por cuenta sintética a esa prueba, sin envolver la lectura en una transacción. Reproducción dirigida: 28 pruebas correctas; suite completa Maven: 717 pruebas, cero fallos, errores y omisiones en H2 local. No verifica producción ni PostgreSQL remoto.

- 2026-11-02: se documentó una propuesta de permisos para rating, XP, confianza y canchas en `docs/propuesta-permisos-operaciones-sensibles.md`. Cierre y MVP ya usan procesos internos; los métodos automáticos de confianza no tienen invocaciones identificadas fuera de su servicio. Pendientes decisiones sobre canchas y reglas de confianza, implementación y pruebas. Los bloqueos locales permanecen sin cambios.

- 2026-11-02: por decisión del usuario, solo el creador autenticado puede editar o desactivar una cancha. V027 agrega `creator_uuid` nullable sin asignar dueños a canchas antiguas, que siguen protegidas; el servidor ignora un creador enviado por el cliente. Pruebas dirigidas con dos cuentas, ausencia de sesión y cancha antigua: 3 aprobadas; suite backend completa con H2: 718 pruebas, cero fallos y omisiones. El cambio no está desplegado ni verificado en PostgreSQL; faltan reglas de confianza y una forma autorizada de resolver canchas antiguas.

- 2026-11-02: verificada V027 en PostgreSQL 18.1 local y temporal. Flyway 10.10.0 aplicó V001–V026, se insertó una cancha sintética y se aplicó V027: el registro conservó sus datos y `creator_uuid` nulo. La prueba de autorización con dos cuentas, propietario falsificado y cancha antigua pasó contra PostgreSQL. Flyway advirtió que su soporte probado llega a PostgreSQL 16. Instancia y datos temporales retirados; no se verificó producción ni se asignaron dueños reales.

- 2026-11-02: compilación empaquetada actual y perfil beta iniciados con PostgreSQL 18.1 temporal; Flyway llegó a V027 y el smoke local completó 420 solicitudes con cuatro trabajadores. Cuenta y base sintéticas eliminadas al detener los servicios. Docker Desktop no arrancó, por lo que no se repitieron las pruebas de contenedor PostgreSQL 16 ni de límites de memoria y CPU. Sin verificación de servicios remotos ni despliegue.


## Beta: autorización social (2 de noviembre de 2026)
- El commit `dd9d1a3` desplegado en Render devuelve acceso denegado cuando un tercero responde solicitudes de amistad o invitaciones de equipo/partido. Pruebas de servicio y contrato aprobadas; en la beta se comprobó 403 y estado pendiente con datos sintéticos anonimizados. El error al aceptar sin equipo se corrigió posteriormente: 409 MATCH_TEAM_REQUIRED, invitación pendiente y rechazo permitido; validado en la beta el 2 de noviembre.

- 2026-11-06: rama de integración local contrastada con beta y main; suite Maven completa: 730 pruebas, sin fallos ni omisiones. Integración y despliegue externo no verificados; V028 modifica ratings actuales superiores a 100 y requiere respaldo y revisión antes de ejecutarse en datos reales.

- 2026-11-06: PostgreSQL 18 temporal con datos sintéticos: el cierre anticipado devolvió 400; después de ajustar solo started_at a dos horas antes, un partido interno con 10 confirmados se cerró desde la interfaz en 0-0 y persistió 10 historiales de jugador y 10 de rating. Voto MVP confirmado desde navegador y API. Servicios detenidos; no se modificó ninguna base real. Flyway advierte que PostgreSQL 18 supera su versión probada.

- 2026-11-06: cierre de partido no interno con menos de dos equipos se rechaza en vez de omitir silenciosamente los ratings; la transacción revierte el cierre ante el error. Pruebas de servicio e integración verifican el rechazo y el cálculo con dos equipos. La suite Maven de la rama de integración completa pasó: 733 pruebas. No se repitió el recorrido con PostgreSQL ni navegador para este cambio.
