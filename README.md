# Atleta Server

## Descripcion
Backend REST para Atleta, una plataforma de gestion deportiva para futbol amateur. El sistema administra atletas, perfiles de jugador, posiciones, equipos, partidos, invitaciones, ratings, OVR, leaderboard, trust score y eventos asociados a la competencia.

## Repositorios relacionados
- Frontend cliente: [Cristianlr95/atleta-app](https://github.com/Cristianlr95/atleta-app)

## Problema que resuelve
Los grupos de futbol amateur suelen organizarse por chats, planillas y acuerdos manuales. Eso dificulta medir rendimiento, confirmar asistencia, balancear equipos, mantener historial y dar trazabilidad a partidos. Atleta Server centraliza la logica de negocio en una API con persistencia, seguridad, ratings y contratos documentados para frontend.

## Funcionalidades principales
- Registro y login local con JWT.
- Login/registro con Google OAuth desde token de identidad.
- Perfil de jugador con alias, posiciones, trust score y ratings.
- Gestion de equipos, miembros e invitaciones.
- Creacion y administracion de partidos 5v5, 6v6 y 7v7.
- Confirmacion de jugadores, asignacion de equipos, eventos, cierre de partido y MVP.
- Sistema de ratings por rol, OVR, historial y leaderboard.
- Catalogo de posiciones y canchas.
- Notificaciones e invitaciones sociales.
- Migraciones Flyway, OpenAPI, Actuator y metricas.

## Stack tecnico
- Java 21
- Spring Boot 3.3
- Spring Web, Spring Security, OAuth2 Client/Resource Server
- Spring Data JPA y Bean Validation
- PostgreSQL 16
- Flyway 10
- OpenAPI/Swagger con `springdoc-openapi`
- Micrometer, Prometheus y Actuator
- Maven Wrapper
- JUnit 5, Spring Security Test, Testcontainers, H2, jqwik y JaCoCo
- Docker y Docker Compose

## Arquitectura / Estructura
El backend es un monolito modular por capas. La estructura efectiva separa contratos HTTP, servicios de negocio, repositorios, entidades, DTOs, configuracion y validaciones.

```text
atleta-server/
  src/main/java/com/atleta/demo/
    controller/        # endpoints REST
    service/           # reglas de negocio y orquestacion
    repository/        # acceso a datos JPA
    entity/            # modelo persistente
    dto/
      request/         # contratos de entrada
      response/        # contratos de salida
    enums/             # estados y tipos de dominio
    config/            # seguridad, OpenAPI, metricas
    validation/        # validadores de configuracion
    exception/         # manejo centralizado de errores
  src/main/resources/
    db/migration/      # migraciones Flyway
    db/test-data/      # datos para testing
    application*.yaml
  api/                 # documentacion de API por dominio
  docs/                # documentacion tecnica y funcional
  scripts/             # backup y restore de base de datos
```

## Instalacion y ejecucion local
Requisitos:

- Java 21
- Docker Desktop o PostgreSQL local
- Maven Wrapper incluido

### Opcion recomendada con Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

Si no existe `.env.example` en tu copia, crea `.env` con al menos:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=atleta_dev
DB_USERNAME=postgres
DB_PASSWORD=postgres
SPRING_PROFILES_ACTIVE=dev
```

La API queda disponible en `http://localhost:8080`.

### Ejecucion local contra PostgreSQL

```bash
# Windows PowerShell
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="atleta_dev"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
.\mvnw.cmd spring-boot:run
```

```bash
# Linux/macOS
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=atleta_dev
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
./mvnw spring-boot:run
```

Comandos utiles:

```bash
./mvnw test
./mvnw verify
```

En Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

## Endpoints de referencia
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/actuator/health`
- Documentacion por dominio: [`api/`](api/)
- Documentacion tecnica: [`docs/`](docs/)

## Estado del proyecto
Proyecto en estado funcional avanzado. La API ya cubre los dominios principales de negocio, autenticacion, ratings y trazabilidad de partidos. El foco actual de evolucion esta en hardening productivo, autorizacion fina y automatizaciones operativas.

## Funcionalidades implementadas
- Autenticacion local y Google OAuth.
- Perfil de jugador, posiciones, equipos y trust score.
- Partidos, invitaciones, confirmaciones, eventos, cierre y MVP.
- Ratings, OVR, historial y leaderboard.
- Catalogos de posiciones y canchas.
- Notificaciones, amistades e invitaciones sociales.
- Observabilidad basica con Actuator/Micrometer.

## Funcionalidades en desarrollo o parciales
- Auto-start y auto-invalidacion de partidos dependen de lecturas; no hay scheduler dedicado.
- Confirmacion dual de eventos existe en API, pero el registro actual cierra eventos inmediatamente.
- Algunas rutas requieren autorizacion de dominio mas estricta.
- Trust score tiene logica distribuida y trazabilidad incompleta para `matchId`.
- Gestion de equipos no cubre edicion completa, reactivacion ni roles avanzados.

## Proximas mejoras
- Separar servicios grandes como `MatchService` y `RatingService`.
- Introducir policies de autorizacion basadas en el usuario autenticado.
- Versionar formulas de rating/XP.
- Agregar jobs programados para expiracion, inicio automatico y recordatorios.
- Ampliar pruebas de integracion para partidos, ratings y seguridad.
- Revisar CORS, secretos, HTTPS y exposicion de Swagger/Actuator por ambiente.

## Valor profesional del proyecto
Este backend demuestra capacidad para modelar un dominio complejo, construir APIs REST con Spring Boot, disenar persistencia relacional versionada, aplicar seguridad JWT/OAuth, documentar contratos para frontend, instrumentar observabilidad y sostener reglas de negocio no triviales como ratings, MVP, trust score y balance de equipos.

## Que conviene revisar primero
- Seguridad y autenticacion JWT/OAuth.
- Dominio de partidos: creacion, confirmaciones, cierre y MVP.
- Sistema de ratings, OVR y leaderboard.
- Migraciones Flyway, OpenAPI y documentacion tecnica del repositorio.
