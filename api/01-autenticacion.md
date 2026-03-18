# Autenticación - API Sistema Atleta

## 🔐 Endpoints de Autenticación

### 1. Registro Local (Email/Password)

**Endpoint:** `POST /athletes/register`

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

**Validaciones:**
- `nombre`: Max 100 caracteres, no vacío
- `email`: Formato válido, único en el sistema
- `password`: Min 8, Max 100 caracteres

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

const user = await response.json();
localStorage.setItem('atletaUuid', user.atletaUuid);
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

**Errores:**
- `400` - Datos inválidos
- `409` - Email ya existe

---

### 2. Login Local

**Endpoint:** `POST /athletes/login`

**URL Completa:**
```
POST http://localhost:8080/api/v1/athletes/login
```

**Body:**
```javascript
{
  "email": "juan@example.com",
  "password": "MiPassword123"
}
```

**Ejemplo:**
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
  const user = await response.json();
  localStorage.setItem('atletaUuid', user.atletaUuid);
  localStorage.setItem('user', JSON.stringify(user));
  window.location.href = '/dashboard';
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

**Errores:**
- `401` - Credenciales incorrectas

---

### 3. Login con Google OAuth2

**Endpoint:** `POST /athletes/auth/google`

**URL Completa:**
```
POST http://localhost:8080/api/v1/athletes/auth/google
```

**Body:**
```javascript
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE4MmU0M..."
}
```

**Implementación Completa:**

```html
<!-- 1. SDK de Google -->
<script src="https://accounts.google.com/gsi/client" async defer></script>

<!-- 2. Botón -->
<div id="g_id_onload"
     data-client_id="TU_CLIENT_ID.apps.googleusercontent.com"
     data-callback="handleGoogleSignIn">
</div>
<div class="g_id_signin" data-type="standard"></div>

<script>
async function handleGoogleSignIn(response) {
  try {
    const res = await fetch('http://localhost:8080/api/v1/athletes/auth/google', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken: response.credential })
    });
    
    const data = await res.json();
    
    // Guardar token y datos
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('atletaUuid', data.atletaUuid);
    localStorage.setItem('user', JSON.stringify(data));
    
    // Redirigir
    window.location.href = '/dashboard';
  } catch (error) {
    console.error('Error:', error);
  }
}
</script>
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

**Comportamiento:**
- Usuario nuevo → Se crea automáticamente
- Usuario existente con Google → Se autentica
- Usuario local con mismo email → Se vincula con Google

**Errores:**
- `400` - Token inválido
- `401` - Email no verificado

---

## 🔑 Gestión de Tokens

### Guardar Token

```javascript
// Después del login exitoso
localStorage.setItem('accessToken', data.accessToken);
localStorage.setItem('atletaUuid', data.atletaUuid);
```

### Usar Token en Requests

```javascript
const response = await fetch(url, {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
    'Content-Type': 'application/json'
  }
});
```

### Verificar si está Autenticado

```javascript
function isAuthenticated() {
  return localStorage.getItem('accessToken') !== null;
}

// Proteger rutas
if (!isAuthenticated()) {
  window.location.href = '/login';
}
```

### Cerrar Sesión

```javascript
function logout() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('atletaUuid');
  localStorage.removeItem('user');
  window.location.href = '/login';
}
```

---

## 📱 Ejemplos por Framework

### React

```jsx
import { useState } from 'react';

function LoginForm() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    try {
      const response = await fetch('http://localhost:8080/api/v1/athletes/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });

      if (response.ok) {
        const user = await response.json();
        localStorage.setItem('atletaUuid', user.atletaUuid);
        window.location.href = '/dashboard';
      } else {
        alert('Credenciales incorrectas');
      }
    } catch (error) {
      console.error('Error:', error);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input 
        type="email" 
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="Email"
        required
      />
      <input 
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Password"
        required
      />
      <button type="submit">Login</button>
    </form>
  );
}
```

### Vue

```vue
<template>
  <form @submit.prevent="handleLogin">
    <input v-model="email" type="email" placeholder="Email" required>
    <input v-model="password" type="password" placeholder="Password" required>
    <button type="submit">Login</button>
  </form>
</template>

<script>
export default {
  data() {
    return {
      email: '',
      password: ''
    };
  },
  methods: {
    async handleLogin() {
      try {
        const response = await fetch('http://localhost:8080/api/v1/athletes/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            email: this.email,
            password: this.password
          })
        });

        if (response.ok) {
          const user = await response.json();
          localStorage.setItem('atletaUuid', user.atletaUuid);
          this.$router.push('/dashboard');
        }
      } catch (error) {
        console.error('Error:', error);
      }
    }
  }
};
</script>
```

---

## 🔒 Seguridad

### Mejores Prácticas

1. **HTTPS en Producción:** Siempre usar HTTPS
2. **No exponer tokens:** No loguear tokens en consola
3. **Expiración:** Implementar renovación de tokens
4. **Validación:** Validar datos antes de enviar
5. **Sanitización:** Limpiar inputs del usuario

### Validación de Email

```javascript
function isValidEmail(email) {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
}
```

### Validación de Password

```javascript
function isValidPassword(password) {
  return password.length >= 8 && password.length <= 100;
}
```

---

## 🔗 Próximos Pasos

- **Crear perfil:** [02-perfiles-jugadores.md](02-perfiles-jugadores.md)
- **Configurar Google OAuth:** [../GOOGLE-OAUTH-SETUP.md](../GOOGLE-OAUTH-SETUP.md)

**Volver al índice:** [README.md](README.md)
