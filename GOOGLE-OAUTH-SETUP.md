# Configuración de Google OAuth2

## Guía de Configuración

### 1. Crear Proyecto en Google Cloud Console

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un nuevo proyecto o selecciona uno existente
3. Habilita la API de Google+ (Google People API)

### 2. Configurar OAuth Consent Screen

1. Ve a "APIs & Services" > "OAuth consent screen"
2. Selecciona "External" (o "Internal" si es para tu organización)
3. Completa la información requerida:
   - App name: "Sistema Atleta"
   - User support email: tu email
   - Developer contact: tu email
4. Agrega los scopes necesarios:
   - `.../auth/userinfo.email`
   - `.../auth/userinfo.profile`
5. Guarda y continúa

### 3. Crear Credenciales OAuth 2.0

1. Ve a "APIs & Services" > "Credentials"
2. Click en "Create Credentials" > "OAuth client ID"
3. Selecciona "Web application"
4. Configura:
   - Name: "Atleta Web Client"
   - Authorized JavaScript origins:
     - `http://localhost:8080` (desarrollo)
     - `https://tu-dominio.com` (producción)
   - Authorized redirect URIs:
     - `http://localhost:8080/login/oauth2/code/google` (desarrollo)
     - `https://tu-dominio.com/login/oauth2/code/google` (producción)
5. Click en "Create"
6. Copia el **Client ID** y **Client Secret**

### 4. Configurar Variables de Entorno

#### Desarrollo (application-dev.yaml)

Opción 1: Variables de entorno
```bash
export GOOGLE_CLIENT_ID="tu-client-id.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="tu-client-secret"
```

Opción 2: Archivo .env (no commitear)
```
GOOGLE_CLIENT_ID=tu-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=tu-client-secret
```

Opción 3: Directamente en application-dev.yaml (solo para desarrollo local)
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: "tu-client-id.apps.googleusercontent.com"
            client-secret: "tu-client-secret"
```

#### Producción

Configurar en el servidor:
```bash
export GOOGLE_CLIENT_ID="tu-client-id-prod.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="tu-client-secret-prod"
```

---

## Flujo de Autenticación

### Opción 1: Frontend con Google Sign-In Button (Recomendado)

El frontend usa el SDK de Google para obtener el ID Token y lo envía al backend.

#### 1. Frontend - Configurar Google Sign-In

```html
<!-- Agregar el script de Google -->
<script src="https://accounts.google.com/gsi/client" async defer></script>

<!-- Botón de Google Sign-In -->
<div id="g_id_onload"
     data-client_id="TU_CLIENT_ID.apps.googleusercontent.com"
     data-callback="handleCredentialResponse">
</div>
<div class="g_id_signin" data-type="standard"></div>
```

#### 2. Frontend - Manejar la Respuesta

```javascript
function handleCredentialResponse(response) {
    // response.credential contiene el ID Token de Google
    const idToken = response.credential;
    
    // Enviar al backend
    fetch('http://localhost:8080/api/v1/athletes/auth/google', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            idToken: idToken
        })
    })
    .then(response => response.json())
    .then(data => {
        console.log('Autenticación exitosa:', data);
        // Guardar el accessToken
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('user', JSON.stringify({
            uuid: data.atletaUuid,
            email: data.email,
            nombre: data.nombre,
            authProvider: data.authProvider
        }));
        
        // Redirigir al dashboard
        window.location.href = '/dashboard';
    })
    .catch(error => {
        console.error('Error en autenticación:', error);
        alert('Error al autenticar con Google');
    });
}
```

### Opción 2: Redirect Flow (Alternativa)

Si prefieres que Spring Security maneje todo el flujo:

1. Usuario hace click en "Login con Google"
2. Redirige a: `http://localhost:8080/oauth2/authorization/google`
3. Google autentica al usuario
4. Redirige de vuelta a: `http://localhost:8080/login/oauth2/code/google`
5. Spring Security procesa la respuesta

---

## Endpoints de Autenticación

### 1. Registro Local (Email/Password)

```http
POST /api/v1/athletes/register
Content-Type: application/json

{
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "password": "MiPassword123"
}
```

**Respuesta (201):**
```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "email": "juan@example.com",
  "nombre": "Juan Pérez",
  "createdAt": "2024-12-20T10:30:00"
}
```

### 2. Login Local (Email/Password)

```http
POST /api/v1/athletes/login
Content-Type: application/json

{
  "email": "juan@example.com",
  "password": "MiPassword123"
}
```

**Respuesta (200):**
```json
{
  "atletaUuid": "550e8400-e29b-41d4-a716-446655440000",
  "email": "juan@example.com",
  "nombre": "Juan Pérez",
  "createdAt": "2024-12-20T10:30:00"
}
```

### 3. Autenticación con Google (Nuevo)

```http
POST /api/v1/athletes/auth/google
Content-Type: application/json

{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE4MmU0M..."
}
```

**Respuesta (200):**
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

---

## Comportamiento del Sistema

### Caso 1: Usuario Nuevo con Google

1. Usuario hace login con Google por primera vez
2. Sistema valida el token con Google
3. Sistema crea un nuevo usuario con:
   - `authProvider`: "GOOGLE"
   - `googleId`: ID único de Google
   - `email`: Email de Google
   - `nombre`: Nombre de Google
   - `pictureUrl`: URL de foto de perfil
   - `passwordHash`: null (no necesita contraseña)
4. Retorna token de acceso

### Caso 2: Usuario Existente con Google

1. Usuario hace login con Google
2. Sistema encuentra el usuario por `googleId` o `email`
3. Actualiza información si cambió (nombre, email, foto)
4. Retorna token de acceso

### Caso 3: Usuario Local que Vincula con Google

1. Usuario tiene cuenta local (email/password)
2. Usuario hace login con Google usando el mismo email
3. Sistema vincula la cuenta:
   - Cambia `authProvider` de "LOCAL" a "GOOGLE"
   - Agrega `googleId`
   - Agrega `pictureUrl`
   - Mantiene `passwordHash` (puede seguir usando ambos métodos)
4. Retorna token de acceso

### Caso 4: Usuario con Ambos Métodos

Después de vincular, el usuario puede:
- Login con Google (usando el botón de Google)
- Login con email/password (usando el formulario local)

---

## Seguridad

### Validaciones del Token de Google

El sistema valida:
1. ✅ Token es válido (firma de Google)
2. ✅ Token no ha expirado
3. ✅ Token es para nuestra aplicación (audience)
4. ✅ Email está verificado por Google

### Protección de Endpoints

- ✅ `/api/v1/athletes/register` - Público
- ✅ `/api/v1/athletes/login` - Público
- ✅ `/api/v1/athletes/auth/google` - Público
- 🔒 Todos los demás endpoints - Requieren autenticación

---

## Testing

### Probar con cURL

```bash
# 1. Obtener un ID Token de Google (desde el navegador)
# Usar: https://developers.google.com/oauthplayground/

# 2. Autenticar con el backend
curl -X POST http://localhost:8080/api/v1/athletes/auth/google \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "TU_ID_TOKEN_DE_GOOGLE"
  }'
```

### Probar con Postman

1. Crear una request POST a `http://localhost:8080/api/v1/athletes/auth/google`
2. Headers: `Content-Type: application/json`
3. Body (raw JSON):
```json
{
  "idToken": "TU_ID_TOKEN_DE_GOOGLE"
}
```

---

## Troubleshooting

### Error: "Token de Google inválido"

- Verifica que el token no haya expirado (duran 1 hora)
- Verifica que el Client ID sea correcto
- Verifica que el token sea para tu aplicación

### Error: "Email de Google no está verificado"

- El usuario debe verificar su email en Google
- Solo emails verificados pueden autenticarse

### Error: "Token de Google no es para esta aplicación"

- Verifica que el `GOOGLE_CLIENT_ID` en el backend coincida con el usado en el frontend
- Verifica que el token fue generado con el Client ID correcto

---

## Migración de Base de Datos

La migración `V005__add_oauth_fields_to_athletes.sql` agrega:

- `auth_provider` VARCHAR(20) - "LOCAL" o "GOOGLE"
- `google_id` VARCHAR(255) - ID único de Google
- `picture_url` VARCHAR(500) - URL de foto de perfil
- `password_hash` ahora es nullable

Los usuarios existentes mantienen `auth_provider = 'LOCAL'`.

---

## Próximos Pasos

1. ✅ Configurar Google Cloud Console
2. ✅ Obtener Client ID y Client Secret
3. ✅ Configurar variables de entorno
4. ✅ Ejecutar migración de base de datos
5. ✅ Implementar botón de Google en el frontend
6. ✅ Probar flujo completo
7. 🔄 Implementar JWT real con firma (opcional, para producción)
8. 🔄 Agregar refresh tokens (opcional)
9. 🔄 Agregar más proveedores OAuth (Facebook, Apple, etc.)

---

## Recursos

- [Google OAuth2 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Google Sign-In for Web](https://developers.google.com/identity/gsi/web)
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
