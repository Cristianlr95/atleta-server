# Equipos - API Sistema Atleta

## 🏆 Gestión de Equipos

### 1. Crear Equipo

**Endpoint:** `POST /teams` 🔒

**URL:** `http://localhost:8080/api/v1/teams`

**Body:**
```javascript
{
  "creadorUuid": "550e8400-e29b-41d4-a716-446655440000",
  "nombre": "Los Tigres",
  "logo": "https://example.com/logo.png",
  "anioFundacion": 2024
}
```

**Ejemplo:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/teams', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  },
  body: JSON.stringify({
    creadorUuid: localStorage.getItem('atletaUuid'),
    nombre: 'Los Tigres',
    logo: 'https://example.com/logo.png',
    anioFundacion: 2024
  })
});

const team = await response.json();
console.log('Equipo creado con ID:', team.id);
```

**Respuesta (201):**
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

### 2. Obtener Equipo

**Endpoint:** `GET /teams/{id}` 🔒

**URL:** `http://localhost:8080/api/v1/teams/{teamId}`

**Ejemplo:**
```javascript
const teamId = 1;
const response = await fetch(
  `http://localhost:8080/api/v1/teams/${teamId}`,
  {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    }
  }
);

const team = await response.json();
```

---

### 3. Listar Todos los Equipos

**Endpoint:** `GET /teams` 🔒

**URL:** `http://localhost:8080/api/v1/teams`

**Ejemplo:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/teams', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  }
});

const teams = await response.json();
```

---

### 4. Actualizar Equipo

**Endpoint:** `PUT /teams/{id}` 🔒

**URL:** `http://localhost:8080/api/v1/teams/{teamId}`

**Body:**
```javascript
{
  "nombre": "Los Tigres FC",
  "logo": "https://example.com/new-logo.png"
}
```

---

### 5. Unirse a Equipo

**Endpoint:** `POST /teams/join` 🔒

**URL:** `http://localhost:8080/api/v1/teams/join`

**Body:**
```javascript
{
  "teamId": 1,
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Ejemplo:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/teams/join', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  },
  body: JSON.stringify({
    teamId: 1,
    playerUuid: localStorage.getItem('atletaUuid')
  })
});
```

---

### 6. Obtener Miembros del Equipo

**Endpoint:** `GET /teams/{id}/members` 🔒

**URL:** `http://localhost:8080/api/v1/teams/{teamId}/members`

**Ejemplo:**
```javascript
const teamId = 1;
const response = await fetch(
  `http://localhost:8080/api/v1/teams/${teamId}/members`,
  {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    }
  }
);

const members = await response.json();
```

**Respuesta (200):**
```json
[
  {
    "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
    "alias": "JuanGol",
    "joinedAt": "2024-12-20T10:00:00"
  }
]
```

---

### 7. Obtener Estadísticas del Equipo

**Endpoint:** `GET /teams/{id}/stats` 🔒

**URL:** `http://localhost:8080/api/v1/teams/{teamId}/stats`

**Respuesta (200):**
```json
{
  "teamId": 1,
  "matchesPlayed": 15,
  "matchesWon": 10,
  "matchesDrawn": 3,
  "matchesLost": 2,
  "goalsScored": 45,
  "goalsConceded": 20
}
```

---

## 📱 Ejemplo Completo

```javascript
async function createAndJoinTeam() {
  const atletaUuid = localStorage.getItem('atletaUuid');
  const accessToken = localStorage.getItem('accessToken');
  
  try {
    // 1. Crear equipo
    const teamResponse = await fetch('http://localhost:8080/api/v1/teams', {
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
    
    const team = await teamResponse.json();
    console.log('✅ Equipo creado:', team.id);
    
    // 2. Obtener estadísticas
    const statsResponse = await fetch(
      `http://localhost:8080/api/v1/teams/${team.id}/stats`,
      {
        headers: { 'Authorization': `Bearer ${accessToken}` }
      }
    );
    
    const stats = await statsResponse.json();
    console.log('Estadísticas:', stats);
    
  } catch (error) {
    console.error('Error:', error);
  }
}
```

---

**Volver al índice:** [README.md](README.md)
