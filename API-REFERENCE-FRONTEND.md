# API Reference para Frontend - Sistema Atleta

## 🌐 Información General

### Base URL
```
Desarrollo: http://localhost:8080/api/v1
Producción: https://tu-dominio.com/api/v1
```

### Autenticación
La mayoría de endpoints requieren un token de acceso en el header:
```
Authorization: Bearer <accessToken>
```

### Content-Type
Todos los requests POST/PUT deben incluir:
```
Content-Type: application/json
```

---

## 📑 Índice de Endpoints

### Autenticación
1. [POST /athletes/register](#1-post-athletesregister) - Registro local
2. [POST /athletes/login](#2-post-athleteslogin) - Login local
3. [POST /athletes/auth/google](#3-post-athletesauthgoogle) - Login con Google

### Perfiles de Jugador
4. [POST /player-profiles](#4-post-player-profiles) - Crear perfil
5. [GET /player-profiles/{uuid}](#5-get-player-profilesuuid) - Obtener perfil
6. [PUT /player-profiles/{uuid}](#6-put-player-profilesuuid) - Actualizar alias

### Posiciones
7. [GET /positions](#7-get-positions) - Listar posiciones
8. [POST /player-profiles/positions](#8-post-player-profilespositions) - Agregar posición

### Equipos
9. [POST /teams](#9-post-teams) - Crear equipo
10. [GET /teams/{id}](#10-get-teamsid) - Obtener equipo

### Partidos
11. [POST /matches](#11-post-matches) - Crear partido
12. [GET /matches](#12-get-matches) - Listar partidos
13. [POST /matches/join](#13-post-matchesjoin) - Unirse a partido
14. [POST /matches/events](#14-post-matchesevents) - Registrar evento
15. [PUT /matches/{id}/status](#15-put-matchesidstatus) - Cambiar estado

### Calificaciones
16. [GET /ratings/player/{uuid}](#16-get-ratingsplayeruuid) - Calificaciones por rol
17. [GET /ratings/player/{uuid}/overall](#17-get-ratingsplayeruuidoverall) - OVR completo
18. [GET /ratings/player/{uuid}/history](#18-get-ratingsplayeruuidhistory) - Historial

---


## 🔐 Autenticación

### 1. POST /athletes/register

Registra un nuevo usuario con email y contraseña.

**URL Completa:**
```
POST http://localhost:8080/api/v1/athletes/register
```

**Headers:**
```javascript
{
  "Content-Type": "application/json"
}
```

**Body:**
```javascript
{
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "password": "MiPassword123"
}
```

**Ejemplo con Fetch:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/athletes/register', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    nombre: 'Juan Pérez',
    email: 'juan@example.com',
    password: 'MiPassword123'
  })
});

const data = await response.json();
console.log(data.atletaUuid); // Guardar este UUID
```

**Ejemplo con Axios:**
```javascript
const { data } = await axios.post('http://localhost:8080/api/v1/athletes/register', {
  nombre: 'Juan Pérez',
  email: 'juan@example.com',
  password: 'MiPassword123'
});

console.log(data.atletaUuid);
```

**Respuesta Exitosa (201):**
```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "email": "juan@example.com",
  "nombre": "Juan Pérez",
  "createdAt": "2024-12-20T10:30:00"
}
```

**Errores:**
- `400 Bad Request` - Datos inválidos
- `409 Conflict` - Email ya existe

---

### 2. POST /athletes/login

Autentica un usuario con email y contraseña.

**URL Completa:**
```
POST http://localhost:8080/api/v1/athletes/login
```

**Headers:**
```javascript
{
  "Content-Type": "application/json"
}
```

**Body:**
```javascript
{
  "email": "juan@example.com",
  "password": "MiPassword123"
}
```

**Ejemplo con Fetch:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/athletes/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    email: 'juan@example.com',
    password: 'MiPassword123'
  })
});

if (response.ok) {
  const data = await response.json();
  // Guardar datos del usuario
  localStorage.setItem('user', JSON.stringify(data));
  localStorage.setItem('atletaUuid', data.atletaUuid);
} else {
  console.error('Credenciales incorrectas');
}
```

**Respuesta Exitosa (200):**
```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "email": "juan@example.com",
  "nombre": "Juan Pérez",
  "createdAt": "2024-12-20T10:30:00"
}
```

**Errores:**
- `401 Unauthorized` - Credenciales incorrectas

---

### 3. POST /athletes/auth/google

Autentica o registra un usuario con Google OAuth2.

**URL Completa:**
```
POST http://localhost:8080/api/v1/athletes/auth/google
```

**Headers:**
```javascript
{
  "Content-Type": "application/json"
}
```

**Body:**
```javascript
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE4MmU0M..."
}
```

**Ejemplo Completo con Google Sign-In:**
```html
<!-- 1. Agregar SDK de Google -->
<script src="https://accounts.google.com/gsi/client" async defer></script>

<!-- 2. Botón de Google -->
<div id="g_id_onload"
     data-client_id="TU_CLIENT_ID.apps.googleusercontent.com"
     data-callback="handleGoogleSignIn">
</div>
<div class="g_id_signin" data-type="standard"></div>

<script>
// 3. Manejar respuesta de Google
async function handleGoogleSignIn(response) {
  try {
    const res = await fetch('http://localhost:8080/api/v1/athletes/auth/google', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        idToken: response.credential
      })
    });
    
    const data = await res.json();
    
    // Guardar token y datos
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('user', JSON.stringify({
      uuid: data.atletaUuid,
      email: data.email,
      nombre: data.nombre,
      authProvider: data.authProvider
    }));
    
    // Redirigir
    window.location.href = '/dashboard.html';
    
  } catch (error) {
    console.error('Error:', error);
    alert('Error al autenticar con Google');
  }
}
</script>
```

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

**Errores:**
- `400 Bad Request` - Token inválido
- `401 Unauthorized` - Email no verificado

---


## 👤 Perfiles de Jugador

### 4. POST /player-profiles

Crea un perfil de jugador para un atleta.

**URL Completa:**
```
POST http://localhost:8080/api/v1/player-profiles
```

**Headers:**
```javascript
{
  "Content-Type": "application/json",
  "Authorization": "Bearer <accessToken>"
}
```

**Body:**
```javascript
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "alias": "JuanGol"
}
```

**Ejemplo con Fetch:**
```javascript
const accessToken = localStorage.getItem('accessToken');
const atletaUuid = localStorage.getItem('atletaUuid');

const response = await fetch('http://localhost:8080/api/v1/player-profiles', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    atletaUuid: atletaUuid,
    alias: 'JuanGol'
  })
});

const profile = await response.json();
console.log('Perfil creado:', profile);
```

**Respuesta Exitosa (201):**
```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "alias": "JuanGol",
  "trustScore": 100,
  "createdAt": "2024-12-20T10:31:00"
}
```

**Errores:**
- `404 Not Found` - Atleta no existe
- `409 Conflict` - Atleta ya tiene perfil o alias ya existe

---

### 5. GET /player-profiles/{uuid}

Obtiene el perfil de un jugador por su UUID.

**URL Completa:**
```
GET http://localhost:8080/api/v1/player-profiles/{atletaUuid}
```

**Headers:**
```javascript
{
  "Authorization": "Bearer <accessToken>"
}
```

**Ejemplo con Fetch:**
```javascript
const atletaUuid = '550e8400-e29b-41d4-a716-446655440000';
const accessToken = localStorage.getItem('accessToken');

const response = await fetch(
  `http://localhost:8080/api/v1/player-profiles/${atletaUuid}`,
  {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  }
);

const profile = await response.json();
console.log('Perfil:', profile);
```

**Respuesta Exitosa (200):**
```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "alias": "JuanGol",
  "trustScore": 100,
  "createdAt": "2024-12-20T10:31:00"
}
```

---

### 6. PUT /player-profiles/{uuid}

Actualiza el alias de un jugador.

**URL Completa:**
```
PUT http://localhost:8080/api/v1/player-profiles/{atletaUuid}
```

**Headers:**
```javascript
{
  "Content-Type": "application/json",
  "Authorization": "Bearer <accessToken>"
}
```

**Body:**
```javascript
{
  "alias": "NuevoAlias"
}
```

**Ejemplo con Fetch:**
```javascript
const atletaUuid = '550e8400-e29b-41d4-a716-446655440000';
const accessToken = localStorage.getItem('accessToken');

const response = await fetch(
  `http://localhost:8080/api/v1/player-profiles/${atletaUuid}`,
  {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
      alias: 'NuevoAlias'
    })
  }
);

const updated = await response.json();
```

---


## ⚽ Posiciones

### 7. GET /positions

Obtiene todas las posiciones disponibles.

**URL Completa:**
```
GET http://localhost:8080/api/v1/positions
```

**Headers:**
```javascript
{
  "Authorization": "Bearer <accessToken>"
}
```

**Ejemplo con Fetch:**
```javascript
const accessToken = localStorage.getItem('accessToken');

const response = await fetch('http://localhost:8080/api/v1/positions', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

const positions = await response.json();
console.log('Posiciones disponibles:', positions);
```

**Respuesta Exitosa (200):**
```json
[
  { "id": 1, "nombre": "Portero" },
  { "id": 2, "nombre": "Defensa" },
  { "id": 3, "nombre": "Carrilero" },
  { "id": 4, "nombre": "Mediocampista" },
  { "id": 5, "nombre": "Delantero" },
  { "id": 6, "nombre": "DT" }
]
```

---

### 8. POST /player-profiles/positions

Agrega una posición a un jugador.

**URL Completa:**
```
POST http://localhost:8080/api/v1/player-profiles/positions
```

**Headers:**
```javascript
{
  "Content-Type": "application/json",
  "Authorization": "Bearer <accessToken>"
}
```

**Body:**
```javascript
{
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "positionId": 5,
  "prioridad": 1
}
```

**Prioridades:**
- `1` = PRINCIPAL (multiplicador 1.0)
- `2` = SECUNDARIA (multiplicador 0.7)
- `3` = TERCIARIA (multiplicador 0.4)

**Ejemplo con Fetch:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/player-profiles/positions', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    playerUuid: atletaUuid,
    positionId: 5, // Delantero
    prioridad: 1   // Principal
  })
});
```

---


## 🏆 Equipos

### 9. POST /teams

Crea un nuevo equipo.

**URL Completa:**
```
POST http://localhost:8080/api/v1/teams
```

**Headers:**
```javascript
{
  "Content-Type": "application/json",
  "Authorization": "Bearer <accessToken>"
}
```

**Body:**
```javascript
{
  "creadorUuid": "550e8400-e29b-41d4-a716-446655440000",
  "nombre": "Los Tigres",
  "logo": "https://example.com/logo.png",
  "anioFundacion": 2024
}
```

**Ejemplo con Fetch:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/teams', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    creadorUuid: atletaUuid,
    nombre: 'Los Tigres',
    logo: 'https://example.com/logo.png',
    anioFundacion: 2024
  })
});

const team = await response.json();
console.log('Equipo creado:', team);
```

**Respuesta Exitosa (201):**
```json
{
  "id": 1,
  "nombre": "Los Tigres",
  "logo": "https://example.com/logo.png",
  "anioFundacion": 2024,
  "creador": {
    "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Juan Pérez"
  }
}
```

---

### 10. GET /teams/{id}

Obtiene información de un equipo.

**URL Completa:**
```
GET http://localhost:8080/api/v1/teams/{teamId}
```

**Headers:**
```javascript
{
  "Authorization": "Bearer <accessToken>"
}
```

**Ejemplo con Fetch:**
```javascript
const teamId = 1;

const response = await fetch(
  `http://localhost:8080/api/v1/teams/${teamId}`,
  {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  }
);

const team = await response.json();
```

---


## 🎮 Partidos

### 11. POST /matches

Crea un nuevo partido.

**URL Completa:**
```
POST http://localhost:8080/api/v1/matches
```

**Headers:**
```javascript
{
  "Content-Type": "application/json",
  "Authorization": "Bearer <accessToken>"
}
```

**Body:**
```javascript
{
  "creadorUuid": "550e8400-e29b-41d4-a716-446655440000",
  "modalidad": "CINCO_VS_CINCO",
  "fechaHoraProgramada": "2024-12-25T18:00:00",
  "latitud": -34.603722,
  "longitud": -58.381592,
  "cuota": 500.0
}
```

**Modalidades disponibles:**
- `CINCO_VS_CINCO` (5v5)
- `SEIS_VS_SEIS` (6v6)
- `SIETE_VS_SIETE` (7v7)

**Ejemplo con Fetch:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/matches', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    creadorUuid: atletaUuid,
    modalidad: 'CINCO_VS_CINCO',
    fechaHoraProgramada: '2024-12-25T18:00:00',
    latitud: -34.603722,
    longitud: -58.381592,
    cuota: 500.0
  })
});

const match = await response.json();
console.log('Partido creado con ID:', match.id);
```

**Respuesta Exitosa (201):**
```json
{
  "id": 1,
  "modalidad": "CINCO_VS_CINCO",
  "fechaHoraProgramada": "2024-12-25T18:00:00",
  "estado": "CREADO",
  "cuota": 500.0,
  "creador": {
    "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Juan Pérez"
  }
}
```

---

### 12. GET /matches

Obtiene lista de partidos.

**URL Completa:**
```
GET http://localhost:8080/api/v1/matches
```

**Headers:**
```javascript
{
  "Authorization": "Bearer <accessToken>"
}
```

**Ejemplo con Fetch:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/matches', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

const matches = await response.json();
console.log('Partidos:', matches);
```

**Otros endpoints útiles:**
- `GET /matches/upcoming` - Próximos partidos
- `GET /matches/by-player/{uuid}` - Partidos de un jugador
- `GET /matches/{id}` - Detalle de un partido

---

### 13. POST /matches/join

Unirse a un partido.

**URL Completa:**
```
POST http://localhost:8080/api/v1/matches/join
```

**Headers:**
```javascript
{
  "Content-Type": "application/json",
  "Authorization": "Bearer <accessToken>"
}
```

**Body:**
```javascript
{
  "matchId": 1,
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "teamId": 1,
  "positionId": 5,
  "role": "JUGADOR"
}
```

**Roles disponibles:**
- `JUGADOR` - Jugador normal
- `CAPITAN` - Capitán del equipo
- `DT` - Director técnico

**Ejemplo con Fetch:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/matches/join', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    matchId: 1,
    playerUuid: atletaUuid,
    teamId: 1,
    positionId: 5, // Delantero
    role: 'JUGADOR'
  })
});

if (response.ok) {
  console.log('Te uniste al partido exitosamente');
}
```

---

### 14. POST /matches/events

Registra un evento en el partido (gol o asistencia).

**URL Completa:**
```
POST http://localhost:8080/api/v1/matches/events
```

**Headers:**
```javascript
{
  "Content-Type": "application/json",
  "Authorization": "Bearer <accessToken>"
}
```

**Body:**
```javascript
{
  "matchId": 1,
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "teamId": 1,
  "eventType": "GOL",
  "assistPlayerUuid": "uuid-del-asistente",
  "registeredByUuid": "uuid-quien-registra"
}
```

**Tipos de evento:**
- `GOL` - Gol anotado
- `ASISTENCIA` - Asistencia

**Ejemplo con Fetch:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/matches/events', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    matchId: 1,
    playerUuid: goleadorUuid,
    teamId: 1,
    eventType: 'GOL',
    assistPlayerUuid: asistenteUuid,
    registeredByUuid: atletaUuid
  })
});
```

---

### 15. PUT /matches/{id}/status

Cambia el estado de un partido.

**URL Completa:**
```
PUT http://localhost:8080/api/v1/matches/{matchId}/status?status={nuevoEstado}
```

**Headers:**
```javascript
{
  "Authorization": "Bearer <accessToken>"
}
```

**Estados disponibles:**
- `CREADO` - Partido creado
- `INICIADO` - Partido en curso
- `FINALIZADO` - Partido terminado (actualiza calificaciones automáticamente)
- `INVALIDO` - Partido cancelado

**Ejemplo con Fetch:**
```javascript
const matchId = 1;

// Finalizar partido
const response = await fetch(
  `http://localhost:8080/api/v1/matches/${matchId}/status?status=FINALIZADO`,
  {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  }
);

if (response.ok) {
  console.log('Partido finalizado. Calificaciones actualizadas automáticamente.');
}
```

---


## 📊 Calificaciones

### 16. GET /ratings/player/{uuid}

Obtiene todas las calificaciones de un jugador por rol.

**URL Completa:**
```
GET http://localhost:8080/api/v1/ratings/player/{playerProfileId}
```

**Headers:**
```javascript
{
  "Authorization": "Bearer <accessToken>"
}
```

**Ejemplo con Fetch:**
```javascript
const playerUuid = '550e8400-e29b-41d4-a716-446655440000';

const response = await fetch(
  `http://localhost:8080/api/v1/ratings/player/${playerUuid}`,
  {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  }
);

const ratings = await response.json();
console.log('Calificaciones por rol:', ratings);
```

**Respuesta Exitosa (200):**
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
  },
  {
    "id": 2,
    "playerProfileId": "550e8400-e29b-41d4-a716-446655440000",
    "alias": "JuanGol",
    "roleType": "MEDIOCAMPO",
    "priorityLevel": "SECUNDARIA",
    "currentRating": 82.00,
    "matchesPlayed": 30,
    "lastUpdated": "2024-12-20T15:30:00"
  }
]
```

---

### 17. GET /ratings/player/{uuid}/overall

Obtiene el OVR (Overall Rating) completo del jugador.

**URL Completa:**
```
GET http://localhost:8080/api/v1/ratings/player/{playerProfileId}/overall
```

**Headers:**
```javascript
{
  "Authorization": "Bearer <accessToken>"
}
```

**Ejemplo con Fetch:**
```javascript
const playerUuid = '550e8400-e29b-41d4-a716-446655440000';

const response = await fetch(
  `http://localhost:8080/api/v1/ratings/player/${playerUuid}/overall`,
  {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  }
);

const overall = await response.json();

// Mostrar OVR
console.log(`OVR: ${overall.hybridOVR}`);
console.log(`Clasificación: ${overall.classification}`);
console.log(`Mejor rol: ${overall.bestRole} (${overall.bestRoleRating})`);

// Crear hexágono de estadísticas
displayHexagon(overall.roleBreakdown);
```

**Respuesta Exitosa (200):**
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

**Clasificaciones:**
- `LEYENDA` (95-100)
- `ÉLITE` (85-94)
- `EXPERTO` (75-84)
- `AVANZADO` (65-74)
- `INTERMEDIO` (55-64)
- `PRINCIPIANTE` (50-54)
- `NOVATO` (0-49)

---

### 18. GET /ratings/player/{uuid}/history

Obtiene el historial completo de calificaciones.

**URL Completa:**
```
GET http://localhost:8080/api/v1/ratings/player/{playerProfileId}/history
```

**Headers:**
```javascript
{
  "Authorization": "Bearer <accessToken>"
}
```

**Ejemplo con Fetch:**
```javascript
const playerUuid = '550e8400-e29b-41d4-a716-446655440000';

const response = await fetch(
  `http://localhost:8080/api/v1/ratings/player/${playerUuid}/history`,
  {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  }
);

const history = await response.json();

// Crear gráfico de evolución
createEvolutionChart(history);
```

**Respuesta Exitosa (200):**
```json
[
  {
    "id": 1,
    "playerProfileId": "550e8400-e29b-41d4-a716-446655440000",
    "roleType": "ATAQUE",
    "previousRating": 90.00,
    "newRating": 92.00,
    "ratingDelta": 2.00,
    "matchId": 15,
    "matchResult": "GANADO",
    "goalsScored": 2,
    "assistsMade": 1,
    "wasMvp": true,
    "recordedAt": "2024-12-20T15:30:00"
  }
]
```

---


## 🔧 Utilidades y Helpers

### Función Helper: API Client

```javascript
// api-client.js
class ApiClient {
  constructor(baseURL = 'http://localhost:8080/api/v1') {
    this.baseURL = baseURL;
  }

  getHeaders(includeAuth = true) {
    const headers = {
      'Content-Type': 'application/json'
    };

    if (includeAuth) {
      const token = localStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
    }

    return headers;
  }

  async request(endpoint, options = {}) {
    const url = `${this.baseURL}${endpoint}`;
    const config = {
      ...options,
      headers: {
        ...this.getHeaders(options.auth !== false),
        ...options.headers
      }
    };

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  }

  // Métodos de autenticación
  async register(nombre, email, password) {
    return this.request('/athletes/register', {
      method: 'POST',
      auth: false,
      body: JSON.stringify({ nombre, email, password })
    });
  }

  async login(email, password) {
    return this.request('/athletes/login', {
      method: 'POST',
      auth: false,
      body: JSON.stringify({ email, password })
    });
  }

  async loginWithGoogle(idToken) {
    return this.request('/athletes/auth/google', {
      method: 'POST',
      auth: false,
      body: JSON.stringify({ idToken })
    });
  }

  // Métodos de perfil
  async createProfile(atletaUuid, alias) {
    return this.request('/player-profiles', {
      method: 'POST',
      body: JSON.stringify({ atletaUuid, alias })
    });
  }

  async getProfile(uuid) {
    return this.request(`/player-profiles/${uuid}`);
  }

  // Métodos de partidos
  async createMatch(matchData) {
    return this.request('/matches', {
      method: 'POST',
      body: JSON.stringify(matchData)
    });
  }

  async getMatches() {
    return this.request('/matches');
  }

  async joinMatch(matchData) {
    return this.request('/matches/join', {
      method: 'POST',
      body: JSON.stringify(matchData)
    });
  }

  // Métodos de calificaciones
  async getPlayerRatings(uuid) {
    return this.request(`/ratings/player/${uuid}`);
  }

  async getPlayerOVR(uuid) {
    return this.request(`/ratings/player/${uuid}/overall`);
  }

  async getPlayerHistory(uuid) {
    return this.request(`/ratings/player/${uuid}/history`);
  }
}

// Exportar instancia
export const api = new ApiClient();
```

### Uso del API Client

```javascript
import { api } from './api-client.js';

// Registro
try {
  const user = await api.register('Juan Pérez', 'juan@example.com', 'password123');
  console.log('Usuario registrado:', user);
} catch (error) {
  console.error('Error en registro:', error);
}

// Login
try {
  const user = await api.login('juan@example.com', 'password123');
  localStorage.setItem('atletaUuid', user.atletaUuid);
} catch (error) {
  console.error('Error en login:', error);
}

// Obtener OVR
try {
  const atletaUuid = localStorage.getItem('atletaUuid');
  const ovr = await api.getPlayerOVR(atletaUuid);
  console.log(`Tu OVR es: ${ovr.hybridOVR}`);
} catch (error) {
  console.error('Error obteniendo OVR:', error);
}
```

---

## 📱 Ejemplos de Integración

### React Example

```jsx
import React, { useState, useEffect } from 'react';
import { api } from './api-client';

function PlayerProfile({ playerUuid }) {
  const [ovr, setOvr] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchOVR() {
      try {
        const data = await api.getPlayerOVR(playerUuid);
        setOvr(data);
      } catch (error) {
        console.error('Error:', error);
      } finally {
        setLoading(false);
      }
    }

    fetchOVR();
  }, [playerUuid]);

  if (loading) return <div>Cargando...</div>;
  if (!ovr) return <div>No se pudo cargar el perfil</div>;

  return (
    <div className="player-card">
      <h2>{ovr.alias}</h2>
      <div className="ovr-badge">{ovr.hybridOVR}</div>
      <div className="classification">{ovr.classification}</div>
      <div className="best-role">
        Mejor rol: {ovr.bestRole} ({ovr.bestRoleRating})
      </div>
      <div className="stats">
        <p>Versatilidad: {(ovr.versatilityIndex * 100).toFixed(0)}%</p>
        <p>Consistencia: {ovr.consistencyScore.toFixed(1)}</p>
        <p>Partidos jugados: {ovr.totalMatchesPlayed}</p>
      </div>
    </div>
  );
}

export default PlayerProfile;
```

### Vue Example

```vue
<template>
  <div class="player-profile">
    <div v-if="loading">Cargando...</div>
    <div v-else-if="ovr" class="profile-card">
      <h2>{{ ovr.alias }}</h2>
      <div class="ovr">{{ ovr.hybridOVR }}</div>
      <div class="classification">{{ ovr.classification }}</div>
      <div class="hexagon">
        <canvas ref="hexagonCanvas"></canvas>
      </div>
    </div>
  </div>
</template>

<script>
import { api } from './api-client';

export default {
  props: ['playerUuid'],
  data() {
    return {
      ovr: null,
      loading: true
    };
  },
  async mounted() {
    try {
      this.ovr = await api.getPlayerOVR(this.playerUuid);
      this.drawHexagon();
    } catch (error) {
      console.error('Error:', error);
    } finally {
      this.loading = false;
    }
  },
  methods: {
    drawHexagon() {
      // Implementar dibujo del hexágono con roleBreakdown
      const canvas = this.$refs.hexagonCanvas;
      const ctx = canvas.getContext('2d');
      // ... código de dibujo
    }
  }
};
</script>
```

### Angular Example

```typescript
import { Component, OnInit } from '@angular/core';
import { ApiService } from './api.service';

@Component({
  selector: 'app-player-profile',
  template: `
    <div class="player-profile" *ngIf="ovr">
      <h2>{{ ovr.alias }}</h2>
      <div class="ovr-badge">{{ ovr.hybridOVR }}</div>
      <div class="classification">{{ ovr.classification }}</div>
      <div class="stats">
        <p>Mejor rol: {{ ovr.bestRole }} ({{ ovr.bestRoleRating }})</p>
        <p>Partidos: {{ ovr.totalMatchesPlayed }}</p>
      </div>
    </div>
  `
})
export class PlayerProfileComponent implements OnInit {
  ovr: any;

  constructor(private apiService: ApiService) {}

  async ngOnInit() {
    const playerUuid = localStorage.getItem('atletaUuid');
    this.ovr = await this.apiService.getPlayerOVR(playerUuid);
  }
}
```

---

## 🚨 Manejo de Errores

### Códigos de Error Comunes

```javascript
async function handleApiCall(apiFunction) {
  try {
    return await apiFunction();
  } catch (error) {
    if (error.message.includes('400')) {
      alert('Datos inválidos. Verifica la información ingresada.');
    } else if (error.message.includes('401')) {
      alert('No autorizado. Por favor inicia sesión nuevamente.');
      // Redirigir al login
      window.location.href = '/login.html';
    } else if (error.message.includes('404')) {
      alert('Recurso no encontrado.');
    } else if (error.message.includes('409')) {
      alert('Conflicto. El recurso ya existe.');
    } else if (error.message.includes('500')) {
      alert('Error del servidor. Intenta más tarde.');
    } else {
      alert('Error de conexión. Verifica tu internet.');
    }
    throw error;
  }
}

// Uso
await handleApiCall(() => api.register(nombre, email, password));
```

---

## 📝 Notas Importantes

1. **Tokens de Acceso:** Guardar en `localStorage` o `sessionStorage`
2. **CORS:** El backend debe configurar CORS para tu dominio
3. **HTTPS:** Usar HTTPS en producción
4. **Timeouts:** Implementar timeouts para requests largos
5. **Retry Logic:** Implementar reintentos para errores de red
6. **Rate Limiting:** Respetar límites de requests por minuto
7. **Validación:** Validar datos antes de enviar al backend
8. **Fechas:** Usar formato ISO 8601 (yyyy-MM-ddTHH:mm:ss)
9. **UUIDs:** Todos los IDs de atletas/jugadores son UUID v4
10. **Tokens Google:** Expiran en 1 hora, renovar si es necesario

---

## 🔗 Enlaces Útiles

- **Swagger UI (desarrollo):** http://localhost:8080/swagger-ui.html
- **OpenAPI Docs:** http://localhost:8080/v3/api-docs
- **Health Check:** http://localhost:8080/actuator/health
- **Documentación completa:** Ver `docs/endpoints-y-accesos.md`
- **Configuración OAuth2:** Ver `GOOGLE-OAUTH-SETUP.md`

---

**Última actualización:** 2024-12-20

Para más información, consulta la documentación completa en el repositorio.
