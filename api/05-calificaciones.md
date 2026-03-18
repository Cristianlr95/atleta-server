# Calificaciones - API Sistema Atleta

## 📊 Sistema de Calificaciones

### 1. Obtener Calificaciones por Rol

**Endpoint:** `GET /ratings/player/{uuid}` 🔒

**URL:** `http://localhost:8080/api/v1/ratings/player/{playerProfileId}`

**Ejemplo:**
```javascript
const uuid = localStorage.getItem('atletaUuid');
const response = await fetch(
  `http://localhost:8080/api/v1/ratings/player/${uuid}`,
  {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    }
  }
);

const ratings = await response.json();
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

### 2. Obtener OVR (Overall Rating)

**Endpoint:** `GET /ratings/player/{uuid}/overall` 🔒

**URL:** `http://localhost:8080/api/v1/ratings/player/{playerProfileId}/overall`

**Ejemplo:**
```javascript
const uuid = localStorage.getItem('atletaUuid');
const response = await fetch(
  `http://localhost:8080/api/v1/ratings/player/${uuid}/overall`,
  {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    }
  }
);

const ovr = await response.json();

// Mostrar OVR
console.log(`OVR: ${ovr.hybridOVR}`);
console.log(`Clasificación: ${ovr.classification}`);
console.log(`Mejor rol: ${ovr.bestRole}`);
```

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

**Clasificaciones:**
- `LEYENDA` (95-100)
- `ÉLITE` (85-94)
- `EXPERTO` (75-84)
- `AVANZADO` (65-74)
- `INTERMEDIO` (55-64)
- `PRINCIPIANTE` (50-54)
- `NOVATO` (0-49)

---

### 3. Obtener Historial

**Endpoint:** `GET /ratings/player/{uuid}/history` 🔒

**URL:** `http://localhost:8080/api/v1/ratings/player/{playerProfileId}/history`

**Ejemplo:**
```javascript
const uuid = localStorage.getItem('atletaUuid');
const response = await fetch(
  `http://localhost:8080/api/v1/ratings/player/${uuid}/history`,
  {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    }
  }
);

const history = await response.json();
```

**Respuesta (200):**
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

### 4. Historial por Rol

**Endpoint:** `GET /ratings/player/{uuid}/history/role/{roleType}` 🔒

**Roles:** `ATAQUE`, `MEDIOCAMPO`, `CARRILERO`, `DEFENSA`, `ARQUERO`, `DT`

---

### 5. Estadísticas Generales

**Endpoint:** `GET /ratings/player/{uuid}/statistics` 🔒

**URL:** `http://localhost:8080/api/v1/ratings/player/{playerProfileId}/statistics`

---

## 📱 Ejemplos de Uso

### Mostrar Tarjeta de Jugador

```javascript
async function displayPlayerCard(playerUuid) {
  const accessToken = localStorage.getItem('accessToken');
  
  // Obtener OVR
  const response = await fetch(
    `http://localhost:8080/api/v1/ratings/player/${playerUuid}/overall`,
    {
      headers: { 'Authorization': `Bearer ${accessToken}` }
    }
  );
  
  const ovr = await response.json();
  
  // Crear tarjeta HTML
  const card = `
    <div class="player-card">
      <h2>${ovr.alias}</h2>
      <div class="ovr-badge">${ovr.hybridOVR}</div>
      <div class="classification">${ovr.classification}</div>
      <div class="best-role">
        Mejor rol: ${ovr.bestRole} (${ovr.bestRoleRating})
      </div>
      <div class="stats">
        <p>Versatilidad: ${(ovr.versatilityIndex * 100).toFixed(0)}%</p>
        <p>Consistencia: ${ovr.consistencyScore.toFixed(1)}</p>
        <p>Partidos: ${ovr.totalMatchesPlayed}</p>
      </div>
    </div>
  `;
  
  document.getElementById('player-container').innerHTML = card;
}
```

### Crear Hexágono de Estadísticas

```javascript
function createHexagon(roleBreakdown) {
  const roles = ['ATAQUE', 'MEDIOCAMPO', 'CARRILERO', 'DEFENSA', 'ARQUERO', 'DT'];
  const values = roles.map(role => roleBreakdown[role] || 0);
  
  // Usar Chart.js o similar
  const ctx = document.getElementById('hexagonChart').getContext('2d');
  new Chart(ctx, {
    type: 'radar',
    data: {
      labels: roles,
      datasets: [{
        label: 'Calificaciones',
        data: values,
        backgroundColor: 'rgba(54, 162, 235, 0.2)',
        borderColor: 'rgba(54, 162, 235, 1)',
        borderWidth: 2
      }]
    },
    options: {
      scales: {
        r: {
          min: 0,
          max: 100,
          ticks: { stepSize: 20 }
        }
      }
    }
  });
}
```

### Gráfico de Evolución

```javascript
async function displayEvolution(playerUuid) {
  const accessToken = localStorage.getItem('accessToken');
  
  // Obtener historial
  const response = await fetch(
    `http://localhost:8080/api/v1/ratings/player/${playerUuid}/history`,
    {
      headers: { 'Authorization': `Bearer ${accessToken}` }
    }
  );
  
  const history = await response.json();
  
  // Preparar datos para gráfico
  const dates = history.map(h => new Date(h.recordedAt).toLocaleDateString());
  const ratings = history.map(h => h.newRating);
  
  // Crear gráfico de línea
  const ctx = document.getElementById('evolutionChart').getContext('2d');
  new Chart(ctx, {
    type: 'line',
    data: {
      labels: dates,
      datasets: [{
        label: 'Evolución de Rating',
        data: ratings,
        borderColor: 'rgb(75, 192, 192)',
        tension: 0.1
      }]
    }
  });
}
```

---

## 🎯 Casos de Uso

### Ranking de Jugadores

```javascript
async function getRanking() {
  // Obtener OVR de múltiples jugadores
  const playerUuids = ['uuid1', 'uuid2', 'uuid3'];
  
  const players = await Promise.all(
    playerUuids.map(async uuid => {
      const response = await fetch(
        `http://localhost:8080/api/v1/ratings/player/${uuid}/overall`,
        {
          headers: { 'Authorization': `Bearer ${accessToken}` }
        }
      );
      return response.json();
    })
  );
  
  // Ordenar por OVR
  players.sort((a, b) => b.hybridOVR - a.hybridOVR);
  
  return players;
}
```

### Comparar Jugadores

```javascript
async function comparePlayer(uuid1, uuid2) {
  const [player1, player2] = await Promise.all([
    fetch(`http://localhost:8080/api/v1/ratings/player/${uuid1}/overall`, {
      headers: { 'Authorization': `Bearer ${accessToken}` }
    }).then(r => r.json()),
    fetch(`http://localhost:8080/api/v1/ratings/player/${uuid2}/overall`, {
      headers: { 'Authorization': `Bearer ${accessToken}` }
    }).then(r => r.json())
  ]);
  
  console.log(`${player1.alias}: ${player1.hybridOVR}`);
  console.log(`${player2.alias}: ${player2.hybridOVR}`);
  console.log(`Diferencia: ${Math.abs(player1.hybridOVR - player2.hybridOVR)}`);
}
```

---

**Volver al índice:** [README.md](README.md)
