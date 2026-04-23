# Arquitectura del Backend Atleta

## Stack detectado

- Java 21
- Spring Boot 3.3.2
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security
- Spring OAuth2 Resource Server y OAuth2 Client
- Spring Actuator
- PostgreSQL 16 compatible
- Flyway 10.10.0
- springdoc OpenAPI 2.5.0
- Micrometer / Prometheus
- JUnit 5, Spring Security Test, Testcontainers, jqwik, H2

## Patron arquitectonico detectado

Monolito modular por capas. No hay hexagonal real en el codigo ejecutable, aunque hay documentacion conceptual antigua. La estructura efectiva es:

- `controller`: contratos HTTP
- `service`: reglas de negocio y orquestacion
- `repository`: consultas JPA
- `entity`: modelo persistente
- `dto`: contratos request/response
- `config` y `validation`: plataforma

## Estructura por capas

### Capa HTTP

- `AthleteController`
- `PlayerProfileController`
- `TeamController`
- `MatchController`
- `RatingController`
- `SocialController`
- `FieldLocationController`
- `PositionController`

### Capa de negocio

- Servicios orientados a dominio: `AthleteService`, `PlayerProfileService`, `TeamService`, `MatchService`, `SocialService`, `RatingService`
- Servicios de apoyo: `JwtService`, `GoogleAuthService`, `MatchMvpService`, `MatchLiveEventService`, `XPService`
- Servicios con deuda/duplicidad: `TrustScoreService`, `DataInitializationService`

### Capa de persistencia

- Repositorios Spring Data con queries derivadas y JPQL/custom SQL.
- Uso de entities JPA enriquecidas con relaciones, validaciones y `@Version`.

## Dominio principal

### Nucleo de identidad

- `Athlete`: identidad global, email unico, password hash, auth provider, Google ID, genero.
- `PlayerProfile`: extension 1:1 de `Athlete`, alias, trust score, posiciones y ratings.

### Nucleo deportivo

- `Team`, `TeamMember`, `TeamStats`
- `Match`, `MatchTeam`, `MatchPlayer`, `MatchEvent`, `MatchMvpVote`
- `Position`, `PlayerPosition`
- `PlayerHistory`

### Nucleo social

- `Friendship`
- `TeamInvite`
- `MatchInvite`
- `AppNotification`

### Nucleo analitico

- `PlayerRating`
- `RatingHistory`
- `XPService` y DTOs de `service/xp`

## Estrategia de persistencia

- Base principal: PostgreSQL con Flyway.
- `spring.jpa.hibernate.ddl-auto=validate` en todos los perfiles.
- Migraciones versionadas `V001` a `V019`.
- `PlayerHistory` se usa como fuente de verdad historica.
- `PlayerRating` y `RatingHistory` separan snapshot actual vs detalle de cambios.
- Entidades principales usan `@Version` para optimistic locking.

## Autenticacion y seguridad

- Registro local con BCrypt.
- Google OAuth via `tokeninfo`.
- JWT HS256 propio con `issuer`, `expiration` y secret configurable.
- Security filter chain stateless.
- CORS restringido a localhost en el codigo actual.
- Actuator/Swagger abiertos en `dev` y `test`.

## Convenciones detectadas

- Prefijo API: `/api/v1/...`
- Uso mixto de `UUID` para usuario y `Long` para agregados.
- DTOs separados en `dto/request` y `dto/response`.
- Naming del dominio en espanol, enums en espanol/ingles mixto.
- Servicios grandes concentran mapping entity -> DTO dentro del mismo servicio.

## Puntos debiles de arquitectura

- `MatchService` es un God Service: estado, cupos, eventos, score final, XP, historial, validaciones y refresh automatico.
- `RatingService` mezcla inicializacion, calculo, historial, estadisticas y leaderboard.
- Falta capa de autorizacion de dominio; seguridad queda mayormente en rutas, no en casos de uso.
- Trust score esta repartido en dos servicios distintos.
- La automatizacion temporal depende de lecturas, no de scheduler ni job dedicado.
- Hay incoherencias entre docs heredadas, migraciones antiguas y el codigo vigente.

## Mejoras sugeridas

1. Separar `MatchService` en submodulos: convocatoria, arbitraje/eventos, cierre, validacion automatica.
2. Introducir autorizacion basada en principal JWT y policies de dominio.
3. Consolidar trust score en una sola API interna.
4. Extraer mappers DTO dedicados para bajar acoplamiento.
5. Introducir jobs programados para expiracion y auto-start.
6. Versionar formulas de rating/XP para evitar cambios silenciosos en el dominio.
