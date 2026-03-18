# Partidos - API Sistema Atleta

## 🎮 Gestión de Partidos

### 1. Crear Partido

**Endpoint:** `POST /matches` 🔒

**URL:** `http://localhost:8080/api/v1/matches`

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

**Modalidades:** `CINCO_VS_CINCO`, `SEIS_VS_SEIS`, `SIETE_VS_SIETE`

**Ejemplo:**
```javascript
const response = await fetch('http://localhost:8080/api/v1/matches', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  },
  body: JSON.stringify({
    creadorUuid: localStorage.getItem('atletaUuid'),
    modalidad: 'CINCO_VS_CINCO',
    fechaHoraProgramada: '2024-12-25T18:00:00',
    cuota: 500.0
  })
});

const match = await response.json();
```

---

### 2. Listar Partidos

**Endpoint:** `GET /matches` 🔒

**Otros endpoints útiles:**
- `GET /matches/upcoming` - Próximos partidos
- `GET /matches/by-player/{uuid}` - Partidos de un jugador
- `GET /matches/{id}` - Detalle de un partido

---

### 3. Unirse a Partido

**Endpoint:** `POST /matches/join` 🔒

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

**Roles:** `JUGADOR`, `CAPITAN`, `DT`

---

### 4. Registrar Evento

**Endpoint:** `POST /matches/events` 🔒

**Body:**
```javascript
{
  "matchId": 1,
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "teamId": 1,
  "eventType": "GOL",
  "assistPlayerUuid": "uuid-asistente",
  "registeredByUuid": "uuid-registrador"
}
```

**Tipos:** `GOL`, `ASISTENCIA`

---

### 5. Cambiar Estado

**Endpoint:** `PUT /matches/{id}/status?status={estado}` 🔒

**Estados:** `CREADO`, `INICIADO`, `FINALIZADO`, `INVALIDO`

**Ejemplo:**
```javascript
const matchId = 1;
const response = await fetch(
  `http://localhost:8080/api/v1/matches/${matchId}/status?status=FINALIZADO`,
  {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    }
  }
);

// Al finalizar, las calificaciones se actualizan automáticamente
```

---

## 📱 Flujo Completo

```javascript
async function createAndManageMatch() {
  const atletaUuid = localStorage.getItem('atletaUuid');
  const accessToken = localStorage.getItem('accessToken');
  
  // 1. Crear partido
  const matchRes = await fetch('http://localhost:8080/api/v1/matches', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
      creadorUuid: atletaUuid,
      modalidad: 'CINCO_VS_CINCO',
      fechaHoraProgramada: '2024-12-25T18:00:00',
      cuota: 500.0
    })
  });
  
  const match = await matchRes.json();
  console.log('Partido creado:', match.id);
  
  // 2. Unirse al partido
  await fetch('http://localhost:8080/api/v1/matches/join', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
      matchId: match.id,
      playerUuid: atletaUuid,
      teamId: 1,
      positionId: 5,
      role: 'JUGADOR'
    })
  });
  
  // 3. Iniciar partido
  await fetch(
    `http://localhost:8080/api/v1/matches/${match.id}/status?status=INICIADO`,
    {
      method: 'PUT',
      headers: { 'Authorization': `Bearer ${accessToken}` }
    }
  );
  
  // 4. Registrar gol
  await fetch('http://localhost:8080/api/v1/matches/events', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
      matchId: match.id,
      playerUuid: atletaUuid,
      teamId: 1,
      eventType: 'GOL',
      registeredByUuid: atletaUuid
    })
  });
  
  // 5. Finalizar partido (actualiza calificaciones)
  await fetch(
    `http://localhost:8080/api/v1/matches/${match.id}/status?status=FINALIZADO`,
    {
      method: 'PUT',
      headers: { 'Authorization': `Bearer ${accessToken}` }
    }
  );
  
  console.log('✅ Partido finalizado. Calificaciones actualizadas.');
}
```

---

**Volver al índice:** [README.md](README.md)
