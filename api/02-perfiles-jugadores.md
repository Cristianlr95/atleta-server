# Perfiles y Jugadores - API Sistema Atleta

## 👤 Gestión de Perfiles

### 1. Crear Perfil de Jugador

**Endpoint:** `POST /player-profiles` 🔒

**URL:** `http://localhost:8080/api/v1/player-profiles`

**Body:**
```javascript
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "alias": "JuanGol"
}
```

**Ejemplo:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/player-profiles', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  },
  body: JSON.stringify({
    atletaUuid: localStorage.getItem('atletaUuid'),
    alias: 'JuanGol'
  })
});

const profile = await response.json();
```

**Respuesta (201):**
```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "alias": "JuanGol",
  "trustScore": 100,
  "createdAt": "2024-12-20T10:31:00"
}
```

---

### 2. Obtener Perfil

**Endpoint:** `GET /player-profiles/{uuid}` 🔒

**URL:** `http://localhost:8080/api/v1/player-profiles/{atletaUuid}`

**Ejemplo:**
```javascript
const uuid = localStorage.getItem('atletaUuid');
const response = await fetch(
  `http://localhost:8080/api/v1/player-profiles/${uuid}`,
  {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    }
  }
);

const profile = await response.json();
```

---

### 3. Actualizar Alias

**Endpoint:** `PUT /player-profiles/{uuid}` 🔒

**Body:**
```javascript
{
  "alias": "NuevoAlias"
}
```

---

## ⚽ Gestión de Posiciones

### 4. Listar Posiciones Disponibles

**Endpoint:** `GET /positions` 🔒

**URL:** `http://localhost:8080/api/v1/positions`

**Ejemplo:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/positions', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  }
});

const positions = await response.json();
```

**Respuesta (200):**
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

### 5. Agregar Posición a Jugador

**Endpoint:** `POST /player-profiles/positions` 🔒

**URL:** `http://localhost:8080/api/v1/player-profiles/positions`

**Body:**
```javascript
{
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "positionId": 5,
  "prioridad": 1
}
```

**Prioridades:**
- `1` = PRINCIPAL (multiplicador 1.0, base 70)
- `2` = SECUNDARIA (multiplicador 0.7, base 60)
- `3` = TERCIARIA (multiplicador 0.4, base 50)

**Ejemplo:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/player-profiles/positions', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  },
  body: JSON.stringify({
    playerUuid: localStorage.getItem('atletaUuid'),
    positionId: 5, // Delantero
    prioridad: 1   // Principal
  })
});
```

---

### 6. Obtener Posiciones del Jugador

**Endpoint:** `GET /player-profiles/{uuid}/positions` 🔒

**URL:** `http://localhost:8080/api/v1/player-profiles/{atletaUuid}/positions`

**Ejemplo:**
```javascript
const uuid = localStorage.getItem('atletaUuid');
const response = await fetch(
  `http://localhost:8080/api/v1/player-profiles/${uuid}/positions`,
  {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    }
  }
);

const positions = await response.json();
```

**Respuesta (200):**
```json
[
  {
    "id": 1,
    "positionId": 5,
    "positionName": "Delantero",
    "prioridad": 1,
    "experiencia": 0
  }
]
```

---

### 7. Remover Posición

**Endpoint:** `DELETE /player-profiles/{uuid}/positions/{positionId}` 🔒

**URL:** `http://localhost:8080/api/v1/player-profiles/{atletaUuid}/positions/{positionId}`

**Ejemplo:**
```javascript
const uuid = localStorage.getItem('atletaUuid');
const positionId = 5;

const response = await fetch(
  `http://localhost:8080/api/v1/player-profiles/${uuid}/positions/${positionId}`,
  {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    }
  }
);
```

---

## 🎯 Trust Score

### 8. Actualizar Trust Score

**Endpoint:** `PUT /player-profiles/trust-score` 🔒

**Body:**
```javascript
{
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "cambio": -10,
  "razon": "No asistió al partido"
}
```

**Ejemplo:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/player-profiles/trust-score', {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  },
  body: JSON.stringify({
    playerUuid: localStorage.getItem('atletaUuid'),
    cambio: -10,
    razon: 'No asistió al partido'
  })
});
```

---

### 9. Obtener Historial de Trust Score

**Endpoint:** `GET /player-profiles/{uuid}/trust-history` 🔒

**URL:** `http://localhost:8080/api/v1/player-profiles/{atletaUuid}/trust-history`

---

## 📱 Ejemplo Completo: Crear Perfil con Posiciones

```javascript
async function setupPlayerProfile() {
  const atletaUuid = localStorage.getItem('atletaUuid');
  const accessToken = localStorage.getItem('accessToken');
  
  try {
    // 1. Crear perfil
    const profileResponse = await fetch('http://localhost:8080/api/v1/player-profiles', {
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
    
    const profile = await profileResponse.json();
    console.log('Perfil creado:', profile);
    
    // 2. Agregar posición principal
    const positionResponse = await fetch('http://localhost:8080/api/v1/player-profiles/positions', {
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
    
    console.log('Posición agregada');
    
    // 3. Agregar posición secundaria
    await fetch('http://localhost:8080/api/v1/player-profiles/positions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      },
      body: JSON.stringify({
        playerUuid: atletaUuid,
        positionId: 4, // Mediocampista
        prioridad: 2   // Secundaria
      })
    });
    
    console.log('✅ Perfil configurado completamente');
    
  } catch (error) {
    console.error('Error:', error);
  }
}
```

---

**Volver al índice:** [README.md](README.md)
