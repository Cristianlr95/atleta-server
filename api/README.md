# API Reference - Sistema Atleta

## 📚 Documentación por Secciones

Esta carpeta contiene la documentación completa de la API dividida por funcionalidad para facilitar su consulta.

### 🌐 Información General
- **[00-getting-started.md](00-getting-started.md)** - Inicio rápido y configuración

### 🔐 Autenticación
- **[01-autenticacion.md](01-autenticacion.md)** - Registro, login y OAuth2

### 👤 Perfiles y Jugadores
- **[02-perfiles-jugadores.md](02-perfiles-jugadores.md)** - Gestión de perfiles y posiciones

### 🏆 Equipos
- **[03-equipos.md](03-equipos.md)** - Creación y gestión de equipos

### 🎮 Partidos
- **[04-partidos.md](04-partidos.md)** - Crear, unirse y gestionar partidos

### 📊 Calificaciones
- **[05-calificaciones.md](05-calificaciones.md)** - Sistema de ratings y OVR

### 🔧 Utilidades
- **[06-utilidades.md](06-utilidades.md)** - Helpers, ejemplos y manejo de errores

---

## 🚀 Inicio Rápido

### 1. Configuración Básica

```javascript
const API_BASE_URL = 'http://localhost:8080/api/v1';

// Headers básicos
const headers = {
  'Content-Type': 'application/json'
};

// Headers con autenticación
const authHeaders = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
};
```

### 2. Primer Request

```javascript
// Registrar usuario
const response = await fetch(`${API_BASE_URL}/athletes/register`, {
  method: 'POST',
  headers: headers,
  body: JSON.stringify({
    nombre: 'Juan Pérez',
    email: 'juan@example.com',
    password: 'MiPassword123'
  })
});

const user = await response.json();
console.log('Usuario registrado:', user.atletaUuid);
```

### 3. Siguiente Paso

Lee la sección correspondiente a lo que necesitas:
- **Autenticación:** [01-autenticacion.md](01-autenticacion.md)
- **Crear perfil:** [02-perfiles-jugadores.md](02-perfiles-jugadores.md)
- **Crear partido:** [04-partidos.md](04-partidos.md)
- **Ver calificaciones:** [05-calificaciones.md](05-calificaciones.md)

---

## 📋 Índice Completo de Endpoints

### Autenticación (3)
- `POST /athletes/register` - Registro local
- `POST /athletes/login` - Login local
- `POST /athletes/auth/google` - Login con Google

### Perfiles (6)
- `POST /player-profiles` - Crear perfil
- `GET /player-profiles/{uuid}` - Obtener perfil
- `PUT /player-profiles/{uuid}` - Actualizar alias
- `GET /positions` - Listar posiciones
- `POST /player-profiles/positions` - Agregar posición
- `DELETE /player-profiles/{uuid}/positions/{id}` - Remover posición

### Equipos (4)
- `POST /teams` - Crear equipo
- `GET /teams/{id}` - Obtener equipo
- `GET /teams` - Listar equipos
- `PUT /teams/{id}` - Actualizar equipo

### Partidos (8)
- `POST /matches` - Crear partido
- `GET /matches` - Listar partidos
- `GET /matches/{id}` - Obtener partido
- `GET /matches/upcoming` - Próximos partidos
- `POST /matches/join` - Unirse a partido
- `POST /matches/events` - Registrar evento
- `PUT /matches/{id}/status` - Cambiar estado
- `GET /matches/by-player/{uuid}` - Partidos de jugador

### Calificaciones (5)
- `GET /ratings/player/{uuid}` - Calificaciones por rol
- `GET /ratings/player/{uuid}/overall` - OVR completo
- `GET /ratings/player/{uuid}/history` - Historial completo
- `GET /ratings/player/{uuid}/history/role/{role}` - Historial por rol
- `GET /ratings/player/{uuid}/statistics` - Estadísticas generales

---

## 🎯 Flujos Comunes

### Flujo 1: Registro y Primer Partido

```
1. Registro → 01-autenticacion.md
2. Crear perfil → 02-perfiles-jugadores.md
3. Agregar posición → 02-perfiles-jugadores.md
4. Crear partido → 04-partidos.md
5. Unirse al partido → 04-partidos.md
```

### Flujo 2: Ver Estadísticas

```
1. Login → 01-autenticacion.md
2. Obtener OVR → 05-calificaciones.md
3. Ver historial → 05-calificaciones.md
```

### Flujo 3: Gestionar Partido

```
1. Crear partido → 04-partidos.md
2. Jugadores se unen → 04-partidos.md
3. Iniciar partido → 04-partidos.md
4. Registrar eventos → 04-partidos.md
5. Finalizar partido → 04-partidos.md
```

---

## 📝 Convenciones

### URLs
Todas las URLs son relativas a: `http://localhost:8080/api/v1`

### Autenticación
Endpoints que requieren autenticación incluyen: 🔒

### Formato de Fechas
ISO 8601: `yyyy-MM-ddTHH:mm:ss`
Ejemplo: `2024-12-25T18:00:00`

### UUIDs
Todos los IDs de atletas/jugadores son UUID v4
Ejemplo: `550e8400-e29b-41d4-a716-446655440000`

---

## 🔗 Enlaces Útiles

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health Check:** http://localhost:8080/actuator/health
- **Documentación técnica:** [../docs/endpoints-y-accesos.md](../docs/endpoints-y-accesos.md)
- **Configuración OAuth2:** [../GOOGLE-OAUTH-SETUP.md](../GOOGLE-OAUTH-SETUP.md)

---

**Última actualización:** 2024-12-20
