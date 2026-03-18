# Guía API para Frontend - Sistema Atleta

## 📋 Resumen de Endpoints Principales

### Base URL
```
http://localhost:8080/api/v1
```

---

## 1. Registro y Autenticación de Usuario

### 1.1 Registrar Atleta con Email/Password (Local)

**Endpoint:** `POST /athletes/register`

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "password": "MiPassword123"
}
```

**Campos:**

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `nombre` | String | ✅ | Max 100 caracteres, no vacío | Nombre completo |
| `email` | String | ✅ | Formato email válido, único | Email para login |
| `password` | String | ✅ | Min 8, Max 100 caracteres | Contraseña (se hashea automáticamente) |

**Respuesta Exitosa (201):**
```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "email": "juan@example.com",
  "nombre": "Juan Pérez",
  "createdAt": "2024-12-20T10:30:00"
}
```

**Errores Posibles:**
- `400`: Datos inválidos (validación fallida)
- `409`: Email ya registrado

---

### 1.2 Autenticación con Google OAuth2 (Nuevo)

**Endpoint:** `POST /athletes/auth/google`

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE4MmU0M..."
}
```

**Campos:**

| Campo | Tipo | Obligatorio | Descripción |
|-------|------|-------------|-------------|
| `idToken` | String | ✅ | Token de ID de Google obtenido del SDK |

**Respuesta Exitosa (200):**
```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "email": "juan@gmail.com",
  "nombre": "Juan Pérez",
  "authProvider": "GOOGLE",
  "accessToken": "NTUwZTg0MDAtZTI5Yi00MWQ0LWE3MTYtNDQ2NjU1NDQwMDAw...",
  "authenticatedAt": "2024-12-20T10:30:00"
}
```

**Comportamiento:**
- Si el usuario NO existe: Se crea automáticamente con los datos de Google
- Si el usuario existe con Google: Se autentica y retorna token
- Si el usuario existe con cuenta local (mismo email): Se vincula con Google

**Errores Posibles:**
- `400`: Token de Google inválido o expirado
- `401`: Email de Google no verificado

**Ejemplo de Implementación Frontend:**

```javascript
// 1. Configurar Google Sign-In Button
<script src="https://accounts.google.com/gsi/client" async defer></script>

<div id="g_id_onload"
     data-client_id="TU_CLIENT_ID.apps.googleusercontent.com"
     data-callback="handleGoogleSignIn">
</div>
<div class="g_id_signin" data-type="standard"></div>

// 2. Manejar la respuesta
function handleGoogleSignIn(response) {
    const idToken = response.credential;
    
    fetch('http://localhost:8080/api/v1/athletes/auth/google', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken })
    })
    .then(res => res.json())
    .then(data => {
        // Guardar token y datos del usuario
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('user', JSON.stringify(data));
        window.location.href = '/dashboard';
    })
    .catch(error => console.error('Error:', error));
}
```

---

### 1.3 Crear Perfil de Jugador (Opcional)

**Endpoint:** `POST /player-profiles`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>  (si aplica)
```

**Body (JSON):**
```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "alias": "JuanGol"
}
```

**Campos:**

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `atletaUuid` | UUID | ✅ | UUID válido | UUID del atleta registrado |
| `alias` | String | ❌ | Max 50 caracteres, único | Apodo del jugador |

**Respuesta Exitosa (201):**
```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "alias": "JuanGol",
  "trustScore": 100,
  "createdAt": "2024-12-20T10:31:00"
}
```

**Errores Posibles:**
- `404`: Atleta no encontrado
- `409`: Atleta ya tiene perfil o alias ya existe

---

## 2. Login (Autenticación Local)

**Endpoint:** `POST /athletes/login`

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "email": "juan@example.com",
  "password": "MiPassword123"
}
```

**Campos:**

| Campo | Tipo | Obligatorio | Descripción |
|-------|------|-------------|-------------|
| `email` | String | ✅ | Email del usuario |
| `password` | String | ✅ | Contraseña |

**Respuesta Exitosa (200):**
```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "email": "juan@example.com",
  "nombre": "Juan Pérez",
  "createdAt": "2024-12-20T10:30:00"
}
```

**Errores Posibles:**
- `401`: Credenciales incorrectas

---

## 3. Crear Partido

**Endpoint:** `POST /matches`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Body (JSON):**
```json
{
  "creadorUuid": "550e8400-e29b-41d4-a716-446655440000",
  "modalidad": "CINCO_VS_CINCO",
  "fechaHoraProgramada": "2024-12-25T18:00:00",
  "latitud": -34.603722,
  "longitud": -58.381592,
  "cuota": 500.0
}
```

**Campos:**

| Campo | Tipo | Obligatorio | Valores Posibles | Descripción |
|-------|------|-------------|------------------|-------------|
| `creadorUuid` | UUID | ✅ | UUID válido | UUID del jugador creador |
| `modalidad` | String | ✅ | `CINCO_VS_CINCO`, `SEIS_VS_SEIS`, `SIETE_VS_SIETE` | Modalidad del partido |
| `fechaHoraProgramada` | DateTime | ✅ | ISO 8601: `yyyy-MM-ddTHH:mm:ss` | Fecha y hora del partido |
| `latitud` | Decimal | ❌ | -90 a 90 | Latitud de ubicación |
| `longitud` | Decimal | ❌ | -180 a 180 | Longitud de ubicación |
| `cuota` | Decimal | ❌ | >= 0 | Cuota económica |

**Respuesta Exitosa (201):**
```json
{
  "id": 1,
  "modalidad": "CINCO_VS_CINCO",
  "fechaHoraProgramada": "2024-12-25T18:00:00",
  "estado": "CREADO",
  "cuota": 500.0,
  "creador": { ... }
}
```

---

## 4. Unirse a un Partido

**Endpoint:** `POST /matches/join`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Body (JSON):**
```json
{
  "matchId": 1,
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "teamId": 1,
  "positionId": 3,
  "role": "JUGADOR"
}
```

**Campos:**

| Campo | Tipo | Obligatorio | Valores Posibles | Descripción |
|-------|------|-------------|------------------|-------------|
| `matchId` | Long | ✅ | ID válido | ID del partido |
| `playerUuid` | UUID | ✅ | UUID válido | UUID del jugador |
| `teamId` | Long | ✅ | ID válido | ID del equipo |
| `positionId` | Long | ✅ | 1-6 | ID de la posición |
| `role` | String | ✅ | `JUGADOR`, `CAPITAN`, `DT` | Rol en el partido |

**Posiciones Disponibles:**
- 1: Portero
- 2: Defensa
- 3: Carrilero
- 4: Mediocampista
- 5: Delantero
- 6: DT

---

## 5. Registrar Evento en Partido

**Endpoint:** `POST /matches/events`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Body (JSON):**
```json
{
  "matchId": 1,
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "teamId": 1,
  "eventType": "GOL",
  "assistPlayerUuid": "uuid-asistente",
  "registeredByUuid": "uuid-registrador"
}
```

**Campos:**

| Campo | Tipo | Obligatorio | Valores Posibles | Descripción |
|-------|------|-------------|------------------|-------------|
| `matchId` | Long | ✅ | ID válido | ID del partido |
| `playerUuid` | UUID | ✅ | UUID válido | Jugador que hace el evento |
| `teamId` | Long | ✅ | ID válido | Equipo del jugador |
| `eventType` | String | ✅ | `GOL`, `ASISTENCIA` | Tipo de evento |
| `assistPlayerUuid` | UUID | ❌ | UUID válido | Jugador que asiste (solo para goles) |
| `registeredByUuid` | UUID | ✅ | UUID válido | Quien registra el evento |

---

## 6. Actualizar Calificaciones (Manual)

**Endpoint:** `POST /ratings/update`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Body (JSON):**
```json
{
  "matchId": 1,
  "performances": [
    {
      "playerProfileId": "550e8400-e29b-41d4-a716-446655440000",
      "roleType": "ATAQUE",
      "priorityLevel": "PRINCIPAL",
      "goalsScored": 2,
      "assistsMade": 1,
      "goalsConceded": null,
      "wasMvp": true,
      "matchResult": "GANADO"
    }
  ]
}
```

**Campos de Performance:**

| Campo | Tipo | Obligatorio | Valores Posibles | Descripción |
|-------|------|-------------|------------------|-------------|
| `playerProfileId` | UUID | ✅ | UUID válido | UUID del jugador |
| `roleType` | String | ✅ | `ATAQUE`, `MEDIOCAMPO`, `CARRILERO`, `DEFENSA`, `ARQUERO`, `DT` | Rol jugado |
| `priorityLevel` | String | ✅ | `PRINCIPAL`, `SECUNDARIA`, `TERCIARIA` | Nivel de prioridad |
| `goalsScored` | Integer | ✅ | >= 0 | Goles anotados |
| `assistsMade` | Integer | ✅ | >= 0 | Asistencias realizadas |
| `goalsConceded` | Integer | ❌ | >= 0 | Goles recibidos (solo DEFENSA/ARQUERO) |
| `wasMvp` | Boolean | ✅ | `true`, `false` | Si fue MVP (solo uno por partido) |
| `matchResult` | String | ✅ | `GANADO`, `EMPATE`, `PERDIDO` | Resultado del partido |

---

## 7. Obtener Calificación General (OVR)

**Endpoint:** `GET /ratings/player/{playerProfileId}/overall`

**Headers:**
```
Authorization: Bearer <token>
```

**Parámetros:**
- `playerProfileId` (path): UUID del jugador

**Respuesta (200):**
```json
{
  "playerProfileId": "550e8400-e29b-41d4-a716-446655440000",
  "alias": "JuanGol",
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

---

## 8. Obtener Calificaciones por Rol

**Endpoint:** `GET /ratings/player/{playerProfileId}`

**Headers:**
```
Authorization: Bearer <token>
```

**Respuesta (200):**
```json
[
  {
    "id": 1,
    "playerProfileId": "550e8400-e29b-41d4-a716-446655440000",
    "alias": "JuanGol",
    "roleType": "ATAQUE",
    "priorityLevel": "PRINCIPAL",
    "currentRating": 92.00,
    "matchesPlayed": 45,
    "lastUpdated": "2024-12-20T15:30:00"
  }
]
```

---

## Valores de Enumeraciones

### Proveedores de Autenticación
```
LOCAL    (Registro con email/password)
GOOGLE   (Registro con Google OAuth2)
```

### Modalidades de Partido
```
CINCO_VS_CINCO
SEIS_VS_SEIS
SIETE_VS_SIETE
```

### Estados de Partido
```
CREADO
INICIADO
FINALIZADO
INVALIDO
```

### Roles de Jugador
```
ATAQUE
MEDIOCAMPO
CARRILERO
DEFENSA
ARQUERO
DT
```

### Niveles de Prioridad
```
PRINCIPAL    (multiplicador 1.0, base 70)
SECUNDARIA   (multiplicador 0.7, base 60)
TERCIARIA    (multiplicador 0.4, base 50)
```

### Resultados de Partido
```
GANADO   (+2.0 puntos)
EMPATE   (+0.5 puntos)
PERDIDO  (-1.5 puntos)
```

### Roles en Partido
```
JUGADOR
CAPITAN
DT
```

### Tipos de Evento
```
GOL
ASISTENCIA
```

### Clasificaciones OVR
```
LEYENDA       (95-100)
ÉLITE         (85-94)
EXPERTO       (75-84)
AVANZADO      (65-74)
INTERMEDIO    (55-64)
PRINCIPIANTE  (50-54)
NOVATO        (0-49)
```

---

## Códigos de Respuesta HTTP

| Código | Significado | Cuándo ocurre |
|--------|-------------|---------------|
| `200` | OK | Operación exitosa (GET, PUT) |
| `201` | Created | Recurso creado exitosamente (POST) |
| `204` | No Content | Operación exitosa sin contenido (DELETE) |
| `400` | Bad Request | Datos inválidos o validación fallida |
| `401` | Unauthorized | No autenticado o credenciales incorrectas |
| `404` | Not Found | Recurso no encontrado |
| `409` | Conflict | Conflicto (email/alias duplicado) |
| `500` | Server Error | Error interno del servidor |

---

## Formato de Fechas

Todas las fechas usan formato ISO 8601:
```
yyyy-MM-ddTHH:mm:ss
```

**Ejemplos:**
```
2024-12-25T18:00:00
2024-01-15T14:30:00
```

---

## Resumen de Flujo Típico

### Opción 1: Registro/Login con Google (Recomendado)

```
1. Usuario hace click en "Continuar con Google"
   → Frontend muestra botón de Google Sign-In
   
2. Usuario autentica con Google
   → Google retorna ID Token al frontend
   
3. Frontend envía token al backend
   POST /athletes/auth/google
   Body: { "idToken": "..." }
   
4. Backend valida token y crea/autentica usuario
   → Retorna accessToken y datos del usuario
   
5. Frontend guarda token y redirige
   → localStorage.setItem('accessToken', data.accessToken)
   → window.location.href = '/dashboard'
```

### Opción 2: Registro/Login Local (Email/Password)

```
1. Registro
   POST /athletes/register
   → Guardar atletaUuid

2. Crear Perfil (opcional)
   POST /player-profiles
   → Usar atletaUuid del paso 1

3. Login
   POST /athletes/login
   → Obtener datos del usuario

4. Crear Partido
   POST /matches
   → Guardar matchId

5. Unirse a Partido
   POST /matches/join
   → Usar matchId y playerUuid

6. Registrar Eventos
   POST /matches/events
   → Durante el partido

7. Finalizar Partido
   PUT /matches/{matchId}/status?status=FINALIZADO
   → Calificaciones se actualizan automáticamente

8. Ver OVR
   GET /ratings/player/{playerProfileId}/overall
   → Ver estadísticas del jugador
```

---

## Notas Importantes

1. **Autenticación Dual**: El sistema soporta dos métodos:
   - Google OAuth2 (recomendado para mejor UX)
   - Email/Password local (tradicional)

2. **Vinculación de Cuentas**: Si un usuario se registra localmente y luego usa Google con el mismo email, las cuentas se vinculan automáticamente

3. **UUIDs**: Todos los IDs de atletas/jugadores son UUID v4

4. **Autenticación**: La mayoría de endpoints requieren token Bearer (excepto registro y login)

5. **Calificaciones**: Se actualizan automáticamente al finalizar partido

6. **Trust Score**: Inicia en 100 al crear perfil

7. **Email**: Debe ser único en todo el sistema

8. **Alias**: Debe ser único si se proporciona

9. **Contraseñas**: Se hashean automáticamente con BCrypt (solo para usuarios locales)

10. **Fechas**: Siempre en formato ISO 8601

11. **Google OAuth**: Requiere configurar Client ID en Google Cloud Console (ver GOOGLE-OAUTH-SETUP.md)

12. **Token de Google**: Expira en 1 hora, el frontend debe obtener uno nuevo si es necesario

