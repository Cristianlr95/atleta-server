# Sistema Atleta - Plataforma de Gestión Deportiva

Sistema completo de gestión de atletas, equipos, partidos y calificaciones con autenticación dual (Local y Google OAuth2).

## 🎯 Características Principales

### Autenticación
- ✅ Registro y login con Email/Password
- ✅ Autenticación con Google OAuth2
- ✅ Vinculación automática de cuentas
- ✅ Tokens JWT para sesiones

### Gestión de Jugadores
- ✅ Perfiles de jugador con alias único
- ✅ Sistema de Trust Score (0-100)
- ✅ Múltiples posiciones por jugador
- ✅ Historial completo de participación

### Sistema de Calificaciones
- ✅ Calificaciones por rol (6 roles diferentes)
- ✅ Cálculo automático basado en rendimiento
- ✅ Hexágono de estadísticas estilo Pokémon
- ✅ OVR (Overall Rating) estilo FIFA
- ✅ Historial completo de calificaciones

### Gestión de Partidos
- ✅ Creación de partidos (5v5, 6v6, 7v7)
- ✅ Sistema de confirmación de jugadores
- ✅ Registro de eventos (goles, asistencias)
- ✅ Actualización automática de calificaciones

## 🚀 Inicio Rápido

### Requisitos Previos

- Java 21+
- PostgreSQL 15+
- Maven 3.8+
- Cuenta de Google Cloud (para OAuth2)

### 1. Clonar y Configurar

```bash
# Clonar repositorio
git clone <repository-url>
cd server-atleta

# Crear base de datos
createdb atleta_dev
```

### 2. Configurar Variables de Entorno

```bash
# Copiar archivo de ejemplo
cp .env.example .env

# Editar .env con tus credenciales
nano .env
```

Variables requeridas:
```env
# Base de datos
DB_HOST=localhost
DB_PORT=5432
DB_NAME=atleta_dev
DB_USERNAME=postgres
DB_PASSWORD=tu_password

# Google OAuth2 (opcional pero recomendado)
GOOGLE_CLIENT_ID=tu-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=tu-client-secret
```

### 3. Ejecutar la Aplicación

```bash
# Compilar y ejecutar
mvn clean install
mvn spring-boot:run

# O con el wrapper
./mvnw spring-boot:run
```

La aplicación estará disponible en: `http://localhost:8080`

### 4. Verificar Instalación

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI (solo desarrollo)
open http://localhost:8080/swagger-ui.html
```

## 📚 Documentación

### Para Desarrolladores Frontend
- **[api/](api/)** ⭐ **NUEVO** - Documentación dividida por secciones
  - [00-getting-started.md](api/00-getting-started.md) - Inicio rápido
  - [01-autenticacion.md](api/01-autenticacion.md) - Registro y login
  - [02-perfiles-jugadores.md](api/02-perfiles-jugadores.md) - Perfiles y posiciones
  - [03-equipos.md](api/03-equipos.md) - Gestión de equipos
  - [04-partidos.md](api/04-partidos.md) - Crear y gestionar partidos
  - [05-calificaciones.md](api/05-calificaciones.md) - Ratings y OVR
  - [06-utilidades.md](api/06-utilidades.md) - Helpers y ejemplos
- **[API-REFERENCE-FRONTEND.md](API-REFERENCE-FRONTEND.md)** - Referencia completa en un solo archivo
- **[GUIA-API-FRONTEND.md](GUIA-API-FRONTEND.md)** - Guía rápida

### Configuración OAuth2
- **[GOOGLE-OAUTH-SETUP.md](GOOGLE-OAUTH-SETUP.md)** - Configuración de Google Cloud Console

### Sistema de Calificaciones
- **[docs/analisis-sistema-calificaciones-y-flujos.md](docs/analisis-sistema-calificaciones-y-flujos.md)** - Fórmulas y flujos
- **[docs/sistema-hexagono-estadisticas.md](docs/sistema-hexagono-estadisticas.md)** - Hexágono estilo Pokémon
- **[docs/calificacion-general-jugador.md](docs/calificacion-general-jugador.md)** - Sistema OVR
- **[docs/implementacion-ovr-completa.md](docs/implementacion-ovr-completa.md)** - Implementación técnica

### Documentación Técnica
- **[docs/endpoints-y-accesos.md](docs/endpoints-y-accesos.md)** - Todos los endpoints disponibles
- **[docs/ci-cd-configuration.md](docs/ci-cd-configuration.md)** - Configuración CI/CD
- **[docs/database-security-guide.md](docs/database-security-guide.md)** - Seguridad de base de datos
- **[docs/database-migration-guide.md](docs/database-migration-guide.md)** - Guía de migraciones

## 🔐 Autenticación

### Opción 1: Google OAuth2 (Recomendado)

```javascript
// Frontend - Botón de Google Sign-In
<script src="https://accounts.google.com/gsi/client" async defer></script>

<div id="g_id_onload"
     data-client_id="TU_CLIENT_ID.apps.googleusercontent.com"
     data-callback="handleGoogleSignIn">
</div>
<div class="g_id_signin" data-type="standard"></div>

<script>
function handleGoogleSignIn(response) {
    fetch('http://localhost:8080/api/v1/athletes/auth/google', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken: response.credential })
    })
    .then(res => res.json())
    .then(data => {
        localStorage.setItem('accessToken', data.accessToken);
        window.location.href = '/dashboard';
    });
}
</script>
```

### Opción 2: Email/Password (Local)

```bash
# Registro
curl -X POST http://localhost:8080/api/v1/athletes/register \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Juan Pérez",
    "email": "juan@example.com",
    "password": "MiPassword123"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/athletes/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "MiPassword123"
  }'
```

## 📊 Endpoints Principales

### Autenticación
- `POST /api/v1/athletes/register` - Registro local
- `POST /api/v1/athletes/login` - Login local
- `POST /api/v1/athletes/auth/google` - Autenticación con Google

### Jugadores
- `POST /api/v1/player-profiles` - Crear perfil de jugador
- `GET /api/v1/player-profiles/{uuid}` - Obtener perfil
- `POST /api/v1/player-profiles/positions` - Agregar posición

### Partidos
- `POST /api/v1/matches` - Crear partido
- `POST /api/v1/matches/join` - Unirse a partido
- `POST /api/v1/matches/events` - Registrar evento
- `PUT /api/v1/matches/{id}/status` - Cambiar estado

### Calificaciones
- `GET /api/v1/ratings/player/{uuid}` - Calificaciones por rol
- `GET /api/v1/ratings/player/{uuid}/overall` - OVR completo
- `GET /api/v1/ratings/player/{uuid}/history` - Historial

Ver documentación completa en [GUIA-API-FRONTEND.md](GUIA-API-FRONTEND.md)

## 🛠️ Tecnologías

- **Backend:** Java 21, Spring Boot 3.3.2
- **Base de Datos:** PostgreSQL 15+
- **Seguridad:** Spring Security, OAuth2 Client
- **Migraciones:** Flyway
- **Documentación:** OpenAPI/Swagger
- **Testing:** JUnit 5, Testcontainers

## 📁 Estructura del Proyecto

```
server-atleta/
├── src/main/java/com/atleta/demo/
│   ├── config/              # Configuración (Security, OAuth2, etc.)
│   ├── controller/          # Controladores REST
│   ├── dto/                 # DTOs (request/response)
│   ├── entity/              # Entidades JPA
│   ├── repository/          # Repositorios Spring Data
│   ├── service/             # Lógica de negocio
│   └── validation/          # Validadores personalizados
├── src/main/resources/
│   ├── db/migration/        # Scripts Flyway
│   ├── application.yaml     # Configuración común
│   └── application-dev.yaml # Configuración desarrollo
├── docs/                    # Documentación técnica
├── scripts/                 # Scripts de utilidad
└── *.md                     # Documentación del proyecto
```

## 🧪 Testing

```bash
# Ejecutar todos los tests
mvn test

# Tests específicos
mvn test -Dtest=AthleteServiceTest

# Con cobertura
mvn clean test jacoco:report
```

## 🔧 Configuración Avanzada

### Perfiles de Ejecución

```bash
# Desarrollo (default)
mvn spring-boot:run

# Staging
mvn spring-boot:run -Dspring-boot.run.profiles=staging

# Producción
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Configuración de Google OAuth2

1. Crear proyecto en [Google Cloud Console](https://console.cloud.google.com/)
2. Habilitar Google+ API
3. Configurar OAuth Consent Screen
4. Crear credenciales OAuth 2.0
5. Configurar Authorized JavaScript origins:
   - `http://localhost:8080` (desarrollo)
   - `https://tu-dominio.com` (producción)
6. Configurar Authorized redirect URIs:
   - `http://localhost:8080/login/oauth2/code/google`

Ver guía completa: [GOOGLE-OAUTH-SETUP.md](GOOGLE-OAUTH-SETUP.md)

## 📈 Monitoreo

### Actuator Endpoints

- `GET /actuator/health` - Estado de salud
- `GET /actuator/metrics` - Métricas de la aplicación
- `GET /actuator/prometheus` - Métricas Prometheus
- `GET /actuator/flyway` - Estado de migraciones

### Logs

```bash
# Ver logs en tiempo real
tail -f logs/application.log

# Nivel de logs (application-dev.yaml)
logging:
  level:
    com.atleta.demo: DEBUG
```

## 🚨 Troubleshooting

### Error: "Token de Google inválido"
- Verifica que el token no haya expirado (duran 1 hora)
- Verifica que el `GOOGLE_CLIENT_ID` sea correcto
- Verifica que el token sea para tu aplicación

### Error: "Connection refused" a PostgreSQL
```bash
# Verificar que PostgreSQL esté corriendo
pg_isready

# Verificar credenciales
psql -U postgres -d atleta_dev
```

### Error: Flyway migration failed
```bash
# Limpiar y reiniciar (solo desarrollo)
mvn flyway:clean
mvn flyway:migrate
```

## 🎮 Ejemplo Completo de Uso

### 1. Registrar Usuario con Google

```javascript
// Frontend obtiene ID Token de Google
const idToken = response.credential;

// Enviar al backend
const authResponse = await fetch('/api/v1/athletes/auth/google', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken })
});

const data = await authResponse.json();
// data.accessToken - Token para usar en requests
// data.atletaUuid - UUID del usuario
```

### 2. Crear Perfil de Jugador

```javascript
const profileResponse = await fetch('/api/v1/player-profiles', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
        atletaUuid: data.atletaUuid,
        alias: "JuanGol"
    })
});
```

### 3. Crear y Unirse a Partido

```javascript
// Crear partido
const matchResponse = await fetch('/api/v1/matches', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
        creadorUuid: data.atletaUuid,
        modalidad: "CINCO_VS_CINCO",
        fechaHoraProgramada: "2024-12-25T18:00:00",
        cuota: 500.0
    })
});

const match = await matchResponse.json();

// Unirse al partido
await fetch('/api/v1/matches/join', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
        matchId: match.id,
        playerUuid: data.atletaUuid,
        teamId: 1,
        positionId: 5, // Delantero
        role: "JUGADOR"
    })
});
```

### 4. Ver Calificaciones (OVR)

```javascript
const ratingsResponse = await fetch(
    `/api/v1/ratings/player/${data.atletaUuid}/overall`,
    {
        headers: { 'Authorization': `Bearer ${accessToken}` }
    }
);

const ratings = await ratingsResponse.json();
console.log(`OVR: ${ratings.hybridOVR}`);
console.log(`Clasificación: ${ratings.classification}`);
console.log(`Mejor rol: ${ratings.bestRole}`);
```

## 📝 Notas Importantes

1. **Autenticación Dual:** El sistema soporta Google OAuth2 y Email/Password
2. **Vinculación Automática:** Cuentas locales se vinculan con Google si usan el mismo email
3. **Calificaciones Automáticas:** Se actualizan al finalizar cada partido
4. **Trust Score:** Inicia en 100 y se ajusta según comportamiento
5. **Tokens JWT:** Actualmente usa Base64 simple (mejorar para producción)

## 🔒 Seguridad

- ✅ Contraseñas hasheadas con BCrypt
- ✅ Tokens de Google validados con API oficial
- ✅ Verificación de email en Google OAuth
- ✅ CORS y CSRF configurables
- ✅ Sesiones stateless (JWT)
- ⚠️ Implementar JWT con firma para producción
- ⚠️ Configurar HTTPS en producción

## 🤝 Contribuir

1. Fork el proyecto
2. Crea una rama (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la licencia MIT.

## 📧 Soporte

Para preguntas o problemas:
- Consulta la documentación en `/docs`
- Revisa los archivos `.md` en la raíz
- Abre un issue en el repositorio

---

**¡Listo para usar!** 🎉

Ver [GUIA-API-FRONTEND.md](GUIA-API-FRONTEND.md) para comenzar a integrar con tu frontend.
