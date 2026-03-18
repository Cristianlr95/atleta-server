# Endpoints y Accesos - Proyecto Atleta

## Información General

### Configuración de Servidor
- **Puerto por defecto**: 8080
- **URL Base**: `http://localhost:8080` (desarrollo)
- **Documentación API**: `http://localhost:8080/swagger-ui.html` (solo en desarrollo)
- **OpenAPI Docs**: `http://localhost:8080/v3/api-docs`

### Autenticación
- **Tipo**: OAuth2 con JWT (JSON Web Tokens)
- **Esquema**: Bearer Token
- **Header**: `Authorization: Bearer <token>`

### Perfiles de Ejecución
1. **dev** (desarrollo): Puerto 8080, base de datos local
2. **staging**: Configuración intermedia
3. **prod** (producción): Variables de entorno, SSL habilitado
4. **test**: Para pruebas automatizadas

---

## Configuración de Base de Datos

### Desarrollo (dev)
```yaml
URL: jdbc:postgresql://localhost:5432/atleta_dev
Usuario: postgres
Password: 12345
Pool máximo: 10 conexiones
```

### Producción (prod)
```yaml
URL: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
Usuario: ${DB_USERNAME}
Password: ${DB_PASSWORD}
SSL: Habilitado (requerido)
Pool máximo: 50 conexiones
```

**Variables de entorno requeridas en producción:**
- `DB_HOST`: Host de la base de datos
- `DB_PORT`: Puerto (default: 5432)
- `DB_NAME`: Nombre de la base de datos
- `DB_USERNAME`: Usuario de la base de datos
- `DB_PASSWORD`: Contraseña de la base de datos
- `SSL_CERT_PATH`: Ruta del certificado SSL
- `SSL_KEY_PATH`: Ruta de la clave SSL
- `SSL_ROOT_CERT_PATH`: Ruta del certificado raíz SSL
- `SSL_PASSWORD`: Contraseña del certificado SSL
- `OAUTH2_ISSUER_URI`: URI del emisor OAuth2
- `SERVER_PORT`: Puerto del servidor (default: 8080)

---

## Endpoints de Monitoreo (Actuator)

### Acceso Público
- `GET /actuator/health` - Estado de salud de la aplicación

### Acceso Autenticado
- `GET /actuator/info` - Información de la aplicación
- `GET /actuator/metrics` - Métricas de la aplicación
- `GET /actuator/flyway` - Estado de migraciones de base de datos
- `GET /actuator/prometheus` - Métricas en formato Prometheus

---

## 1. Endpoints de Atletas
**Base Path**: `/api/v1/athletes`

### 1.1 Registro y Autenticación

#### Registrar Atleta
```http
POST /api/v1/athletes/register
Content-Type: application/json

{
  "nombre": "string",
  "email": "string",
  "password": "string"
}
```
**Respuestas:**
- `201 Created`: Atleta registrado exitosamente
- `400 Bad Request`: Datos inválidos
- `409 Conflict`: Email ya existe

#### Login
```http
POST /api/v1/athletes/login
Content-Type: application/json

{
  "email": "string",
  "password": "string"
}
```
**Respuestas:**
- `200 OK`: Autenticación exitosa
- `401 Unauthorized`: Credenciales inválidas

### 1.2 Consultas de Atletas

#### Obtener Atleta por UUID
```http
GET /api/v1/athletes/{atletaUuid}
Authorization: Bearer <token>
```

#### Obtener Atleta por Email
```http
GET /api/v1/athletes/by-email/{email}
Authorization: Bearer <token>
```

#### Buscar Atletas por Nombre
```http
GET /api/v1/athletes/search?nombre={texto}
Authorization: Bearer <token>
```

#### Verificar si Email Existe
```http
GET /api/v1/athletes/email-exists/{email}
Authorization: Bearer <token>
```

#### Obtener Atletas Registrados Después de una Fecha
```http
GET /api/v1/athletes/registered-after?fecha={yyyy-MM-ddTHH:mm:ss}
Authorization: Bearer <token>
```

#### Obtener Total de Atletas
```http
GET /api/v1/athletes/stats
Authorization: Bearer <token>
```

### 1.3 Actualización de Atletas

#### Actualizar Información del Atleta
```http
PUT /api/v1/athletes/{atletaUuid}
Authorization: Bearer <token>
Content-Type: application/json

{
  "nombre": "string"
}
```

#### Cambiar Contraseña
```http
PUT /api/v1/athletes/{atletaUuid}/password
Authorization: Bearer <token>
Content-Type: application/json

{
  "currentPassword": "string",
  "newPassword": "string"
}
```

---

## 2. Endpoints de Perfiles de Jugador
**Base Path**: `/api/v1/player-profiles`

### 2.1 Gestión de Perfiles

#### Crear Perfil de Jugador
```http
POST /api/v1/player-profiles
Authorization: Bearer <token>
Content-Type: application/json

{
  "atletaUuid": "uuid",
  "alias": "string"
}
```
**Respuestas:**
- `201 Created`: Perfil creado (trust score inicial: 100)
- `404 Not Found`: Atleta no encontrado
- `409 Conflict`: Atleta ya tiene perfil o alias ya existe

#### Obtener Perfil por UUID del Atleta
```http
GET /api/v1/player-profiles/{atletaUuid}
Authorization: Bearer <token>
```

#### Obtener Perfil por Alias
```http
GET /api/v1/player-profiles/by-alias/{alias}
Authorization: Bearer <token>
```

#### Actualizar Alias
```http
PUT /api/v1/player-profiles/{atletaUuid}
Authorization: Bearer <token>
Content-Type: application/json

{
  "alias": "string"
}
```

#### Buscar Jugadores por Nombre
```http
GET /api/v1/player-profiles/search?nombre={texto}
Authorization: Bearer <token>
```

### 2.2 Gestión de Posiciones

#### Agregar Posición a Jugador
```http
POST /api/v1/player-profiles/positions
Authorization: Bearer <token>
Content-Type: application/json

{
  "playerUuid": "uuid",
  "positionId": "long",
  "prioridad": "integer (1, 2, o 3)"
}
```

#### Obtener Posiciones de Jugador
```http
GET /api/v1/player-profiles/{atletaUuid}/positions
Authorization: Bearer <token>
```

#### Remover Posición de Jugador
```http
DELETE /api/v1/player-profiles/{atletaUuid}/positions/{positionId}
Authorization: Bearer <token>
```

#### Agregar Experiencia a Posición
```http
PUT /api/v1/player-profiles/{atletaUuid}/positions/{positionId}/experience?xp={cantidad}
Authorization: Bearer <token>
```

### 2.3 Trust Score

#### Actualizar Trust Score
```http
PUT /api/v1/player-profiles/trust-score
Authorization: Bearer <token>
Content-Type: application/json

{
  "playerUuid": "uuid",
  "cambio": "integer",
  "razon": "string"
}
```

#### Obtener Historial de Trust Score
```http
GET /api/v1/player-profiles/{atletaUuid}/trust-history
Authorization: Bearer <token>
```

#### Buscar Jugadores por Rango de Trust Score
```http
GET /api/v1/player-profiles/by-trust-score?minScore={min}&maxScore={max}
Authorization: Bearer <token>
```

---

## 3. Endpoints de Posiciones
**Base Path**: `/api/v1/positions`

### 3.1 Catálogo de Posiciones

#### Obtener Todas las Posiciones
```http
GET /api/v1/positions
Authorization: Bearer <token>
```
**Posiciones disponibles:**
- Portero
- Defensa
- Carrilero
- Mediocampista
- Delantero
- DT (Director Técnico)

#### Obtener Posición por ID
```http
GET /api/v1/positions/{id}
Authorization: Bearer <token>
```

#### Buscar Posiciones por Nombre
```http
GET /api/v1/positions/search?nombre={texto}
Authorization: Bearer <token>
```

---

## 4. Endpoints de Equipos
**Base Path**: `/api/v1/teams`

### 4.1 Gestión de Equipos

#### Crear Equipo
```http
POST /api/v1/teams
Authorization: Bearer <token>
Content-Type: application/json

{
  "creadorUuid": "uuid",
  "nombre": "string",
  "logo": "string (URL)",
  "anioFundacion": "integer"
}
```
**Respuestas:**
- `201 Created`: Equipo creado con estadísticas en cero
- `404 Not Found`: Creador no encontrado
- `409 Conflict`: Nombre de equipo ya existe

---

## 5. Endpoints de Partidos
**Base Path**: `/api/v1/matches`

### 5.1 Gestión de Partidos

#### Crear Partido
```http
POST /api/v1/matches
Authorization: Bearer <token>
Content-Type: application/json

{
  "creadorUuid": "uuid",
  "modalidad": "CINCO_VS_CINCO | SEIS_VS_SEIS | SIETE_VS_SIETE",
  "fechaHora": "yyyy-MM-ddTHH:mm:ss",
  "latitud": "double",
  "longitud": "double",
  "cuota": "double"
}
```

#### Obtener Partido por ID
```http
GET /api/v1/matches/{matchId}
Authorization: Bearer <token>
```

#### Obtener Todos los Partidos
```http
GET /api/v1/matches
Authorization: Bearer <token>
```

#### Obtener Próximos Partidos
```http
GET /api/v1/matches/upcoming
Authorization: Bearer <token>
```

#### Obtener Partidos por Jugador
```http
GET /api/v1/matches/by-player/{playerUuid}
Authorization: Bearer <token>
```

#### Obtener Partidos por Equipo
```http
GET /api/v1/matches/by-team/{teamId}
Authorization: Bearer <token>
```

### 5.2 Gestión de Estado del Partido

#### Cambiar Estado del Partido
```http
PUT /api/v1/matches/{matchId}/status?status={CREADO|INICIADO|FINALIZADO|INVALIDO}
Authorization: Bearer <token>
```

### 5.3 Gestión de Equipos en Partido

#### Agregar Equipo al Partido
```http
POST /api/v1/matches/{matchId}/teams/{teamId}?esLocal={true|false}
Authorization: Bearer <token>
```

### 5.4 Participación de Jugadores

#### Unirse a un Partido
```http
POST /api/v1/matches/join
Authorization: Bearer <token>
Content-Type: application/json

{
  "matchId": "long",
  "playerUuid": "uuid",
  "teamId": "long",
  "positionId": "long",
  "role": "JUGADOR | CAPITAN | DT"
}
```

#### Confirmar Participación
```http
PUT /api/v1/matches/{matchId}/players/{playerUuid}/confirm
Authorization: Bearer <token>
```

### 5.5 Eventos del Partido

#### Registrar Evento (Gol/Asistencia)
```http
POST /api/v1/matches/events
Authorization: Bearer <token>
Content-Type: application/json

{
  "matchId": "long",
  "playerUuid": "uuid",
  "teamId": "long",
  "eventType": "GOL | ASISTENCIA",
  "assistPlayerUuid": "uuid (opcional)",
  "registeredByUuid": "uuid"
}
```

#### Confirmar Evento
```http
PUT /api/v1/matches/events/{eventId}/confirm?confirmingPlayerUuid={uuid}&isLocalTeam={true|false}
Authorization: Bearer <token>
```

#### Obtener Eventos del Partido
```http
GET /api/v1/matches/{matchId}/events
Authorization: Bearer <token>
```

---

## 6. Endpoints de Calificaciones
**Base Path**: `/api/v1/ratings`

### 6.1 Actualización de Calificaciones

#### Actualizar Calificaciones Manualmente
```http
POST /api/v1/ratings/update
Authorization: Bearer <token>
Content-Type: application/json

{
  "matchId": "long",
  "performances": [
    {
      "playerUuid": "uuid",
      "goalsScored": "integer",
      "assistsMade": "integer",
      "goalsConceded": "integer",
      "wasMvp": "boolean",
      "matchResult": "WIN | DRAW | LOSS"
    }
  ]
}
```

#### Actualizar Calificaciones en Modo Arquero Rotativo
```http
POST /api/v1/ratings/update-rotative-goalkeeper
Authorization: Bearer <token>
Content-Type: application/json

{
  "matchId": "long",
  "matchResult": "WIN | DRAW | LOSS"
}
```

### 6.2 Consulta de Calificaciones

#### Obtener Calificaciones de Jugador
```http
GET /api/v1/ratings/player/{playerProfileId}
Authorization: Bearer <token>
```

#### Obtener Calificaciones por Rol
```http
GET /api/v1/ratings/player/{playerProfileId}/role/{roleType}
Authorization: Bearer <token>
```
**Roles disponibles:** `GOALKEEPER`, `DEFENDER`, `MIDFIELDER`, `FORWARD`, `COACH`

#### Obtener Calificaciones por Prioridad
```http
GET /api/v1/ratings/player/{playerProfileId}/priority/{priorityLevel}
Authorization: Bearer <token>
```
**Prioridades:** `PRIORITY_1`, `PRIORITY_2`, `PRIORITY_3`

### 6.3 Historial de Calificaciones

#### Obtener Historial Completo
```http
GET /api/v1/ratings/player/{playerProfileId}/history
Authorization: Bearer <token>
```

#### Obtener Historial por Rol
```http
GET /api/v1/ratings/player/{playerProfileId}/history/role/{roleType}
Authorization: Bearer <token>
```

#### Obtener Historial por Período
```http
GET /api/v1/ratings/player/{playerProfileId}/history/period?startDate={fecha}&endDate={fecha}
Authorization: Bearer <token>
```
**Formato de fecha:** `yyyy-MM-ddTHH:mm:ss`

### 6.4 Estadísticas de Rendimiento

#### Obtener Estadísticas Generales
```http
GET /api/v1/ratings/player/{playerProfileId}/statistics
Authorization: Bearer <token>
```

#### Obtener Estadísticas por Rol
```http
GET /api/v1/ratings/player/{playerProfileId}/statistics/role/{roleType}
Authorization: Bearer <token>
```

---

## Códigos de Respuesta HTTP

### Exitosos
- `200 OK`: Solicitud exitosa
- `201 Created`: Recurso creado exitosamente
- `204 No Content`: Operación exitosa sin contenido de respuesta

### Errores del Cliente
- `400 Bad Request`: Datos de entrada inválidos
- `401 Unauthorized`: No autenticado o token inválido
- `403 Forbidden`: No autorizado para realizar la operación
- `404 Not Found`: Recurso no encontrado
- `409 Conflict`: Conflicto con el estado actual (ej: email duplicado)

### Errores del Servidor
- `500 Internal Server Error`: Error interno del servidor

---

## Seguridad

### Endpoints Públicos (sin autenticación)
- `POST /api/v1/athletes/register`
- `POST /api/v1/athletes/login`
- `GET /actuator/health`
- `GET /swagger-ui/**` (solo en desarrollo)
- `GET /v3/api-docs/**` (solo en desarrollo)

### Endpoints Protegidos
Todos los demás endpoints requieren:
```http
Authorization: Bearer <JWT_TOKEN>
```

### Configuración de Sesiones
- **Política**: STATELESS (sin sesiones)
- **CSRF**: Deshabilitado
- **CORS**: Deshabilitado

---

## Migraciones de Base de Datos (Flyway)

### Ubicación de Scripts
- **Migraciones**: `src/main/resources/db/migration/`
- **Datos de prueba**: `src/main/resources/db/test-data/`

### Scripts Disponibles
1. `V001__create_initial_schema.sql` - Esquema inicial
2. `V002__add_basic_indexes.sql` - Índices de rendimiento
3. `V003__create_player_ratings_table.sql` - Tabla de calificaciones
4. `V004__create_rating_history_table.sql` - Historial de calificaciones
5. `V900__insert_test_athletes.sql` - Datos de prueba de atletas
6. `V901__insert_test_teams.sql` - Datos de prueba de equipos

### Endpoint de Estado de Flyway
```http
GET /actuator/flyway
Authorization: Bearer <token>
```

---

## Ejemplos de Uso

### 1. Flujo Completo de Registro y Creación de Perfil

```bash
# 1. Registrar atleta
curl -X POST http://localhost:8080/api/v1/athletes/register \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Juan Pérez",
    "email": "juan@example.com",
    "password": "password123"
  }'

# 2. Login
curl -X POST http://localhost:8080/api/v1/athletes/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "password123"
  }'

# 3. Crear perfil de jugador (con token obtenido)
curl -X POST http://localhost:8080/api/v1/player-profiles \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "atletaUuid": "<uuid-del-atleta>",
    "alias": "JuanGol"
  }'
```

### 2. Crear y Unirse a un Partido

```bash
# 1. Crear partido
curl -X POST http://localhost:8080/api/v1/matches \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "creadorUuid": "<uuid>",
    "modalidad": "CINCO_VS_CINCO",
    "fechaHora": "2024-12-25T18:00:00",
    "latitud": -34.603722,
    "longitud": -58.381592,
    "cuota": 500.0
  }'

# 2. Unirse al partido
curl -X POST http://localhost:8080/api/v1/matches/join \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "matchId": 1,
    "playerUuid": "<uuid>",
    "teamId": 1,
    "positionId": 3,
    "role": "JUGADOR"
  }'
```

---

## Notas Adicionales

### Formato de Fechas
Todas las fechas usan el formato ISO 8601: `yyyy-MM-ddTHH:mm:ss`
Ejemplo: `2024-12-25T18:30:00`

### UUIDs
Los identificadores de atletas y jugadores usan UUID v4
Ejemplo: `550e8400-e29b-41d4-a716-446655440000`

### Logs
- **Desarrollo**: Nivel DEBUG, logs en consola
- **Producción**: Nivel ERROR, logs en archivo (`logs/application.log`)
- **Tamaño máximo de archivo**: 500MB
- **Retención**: 60 días

### Métricas y Monitoreo
- **Prometheus**: Habilitado en todos los perfiles
- **Health checks**: Incluye estado de base de datos
- **Percentiles HTTP**: 50%, 95%, 99%


---

## 7. Endpoint de Calificación General (OVR)

### Obtener OVR Completo del Jugador

```http
GET /api/v1/ratings/player/{playerProfileId}/overall
Authorization: Bearer <token>
```

**Descripción:** Calcula y retorna la calificación general (OVR) del jugador usando la fórmula híbrida, junto con métricas adicionales.

**Parámetros:**
- `playerProfileId` (path): UUID del perfil del jugador

**Respuesta Exitosa (200 OK):**
```json
{
  "playerProfileId": "550e8400-e29b-41d4-a716-446655440000",
  "alias": "El Fenómeno",
  "hybridOVR": 83.80,
  "weightedOVR": 81.70,
  "simpleOVR": 71.00,
  "classification": "EXPERTO",
  "versatilityIndex": 0.67,
  "consistencyScore": 73.20,
  "bestRole": "ATAQUE",
  "bestRoleRating": 92.00,
  "totalRatings": 6,
  "totalMatchesPlayed": 113,
  "roleBreakdown": {
    "ATAQUE": 92.00,
    "MEDIOCAMPO": 82.00,
    "CARRILERO": 72.00,
    "DEFENSA": 65.00,
    "DT": 60.00,
    "ARQUERO": 55.00
  }
}
```

**Campos de la Respuesta:**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `playerProfileId` | UUID | Identificador del jugador |
| `alias` | String | Alias del jugador |
| `hybridOVR` | Decimal | OVR calculado con fórmula híbrida (recomendado) |
| `weightedOVR` | Decimal | OVR ponderado por prioridades |
| `simpleOVR` | Decimal | OVR simple (promedio de todos los roles) |
| `classification` | String | Clasificación del jugador (LEYENDA, ÉLITE, EXPERTO, etc.) |
| `versatilityIndex` | Decimal | Índice de versatilidad (0.0 - 1.0) |
| `consistencyScore` | Decimal | Score de consistencia (0 - 100) |
| `bestRole` | String | Mejor rol del jugador |
| `bestRoleRating` | Decimal | Calificación del mejor rol |
| `totalRatings` | Integer | Número total de roles con calificación |
| `totalMatchesPlayed` | Integer | Total de partidos jugados en todos los roles |
| `roleBreakdown` | Object | Desglose de calificaciones por rol |

**Clasificaciones Posibles:**
- `LEYENDA`: 95-100
- `ÉLITE`: 85-94
- `EXPERTO`: 75-84
- `AVANZADO`: 65-74
- `INTERMEDIO`: 55-64
- `PRINCIPIANTE`: 50-54
- `NOVATO`: 0-49

**Códigos de Respuesta:**
- `200 OK`: OVR calculado exitosamente
- `400 Bad Request`: ID de jugador inválido
- `404 Not Found`: Jugador no encontrado o sin calificaciones

**Ejemplo de Uso:**

```bash
curl -X GET "http://localhost:8080/api/v1/ratings/player/550e8400-e29b-41d4-a716-446655440000/overall" \
  -H "Authorization: Bearer <token>"
```

### Fórmula del OVR Híbrido

```
OVR = (Mejor_Rol × 0.4) + (Top_3_Promedio × 0.4) + (Todos_Promedio × 0.2)

Donde:
- Mejor_Rol: Calificación más alta del jugador (40%)
- Top_3_Promedio: Promedio de las 3 mejores calificaciones (40%)
- Todos_Promedio: Promedio de todas las calificaciones (20%)
```

### Casos de Uso

1. **Ranking de Jugadores**: Ordenar jugadores por OVR para crear clasificaciones
2. **Balanceo de Equipos**: Usar OVR para crear equipos equilibrados
3. **Matchmaking**: Emparejar jugadores con OVR similar
4. **Perfil de Jugador**: Mostrar tarjeta estilo FIFA con OVR destacado
5. **Recomendación de Posición**: Comparar OVR con calificación por rol
