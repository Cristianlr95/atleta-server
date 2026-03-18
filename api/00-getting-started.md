# Getting Started - API Sistema Atleta

## 🌐 Información General

### Base URL

```
Desarrollo: http://localhost:8080/api/v1
Producción: https://tu-dominio.com/api/v1
```

### Autenticación

La mayoría de endpoints requieren un token de acceso en el header:

```javascript
Authorization: Bearer <accessToken>
```

### Content-Type

Todos los requests POST/PUT deben incluir:

```javascript
Content-Type: application/json
```

---

## 🚀 Configuración Inicial

### 1. Constantes de Configuración

```javascript
// config.js
export const API_CONFIG = {
  BASE_URL: process.env.API_URL || 'http://localhost:8080/api/v1',
  TIMEOUT: 30000, // 30 segundos
  HEADERS: {
    'Content-Type': 'application/json'
  }
};
```

### 2. Función Helper para Headers

```javascript
// utils/headers.js
export function getHeaders(includeAuth = true) {
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
```

### 3. Función Helper para Requests

```javascript
// utils/api.js
import { API_CONFIG } from '../config';
import { getHeaders } from './headers';

export async function apiRequest(endpoint, options = {}) {
  const url = `${API_CONFIG.BASE_URL}${endpoint}`;
  
  const config = {
    ...options,
    headers: {
      ...getHeaders(options.auth !== false),
      ...options.headers
    }
  };

  try {
    const response = await fetch(url, config);
    
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || `HTTP ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('API Error:', error);
    throw error;
  }
}
```

---

## 📦 Instalación de Dependencias

### Opción 1: Vanilla JavaScript (Fetch API)

No requiere instalación adicional. Fetch API está incluido en navegadores modernos.

```javascript
// Listo para usar
const response = await fetch(url, options);
```

### Opción 2: Axios

```bash
npm install axios
```

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Interceptor para agregar token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
```

### Opción 3: React Query

```bash
npm install @tanstack/react-query
```

```javascript
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutos
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      {/* Tu app */}
    </QueryClientProvider>
  );
}
```

---

## 🔧 Configuración por Framework

### React

```javascript
// src/services/api.js
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1';

export const api = {
  async get(endpoint) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      }
    });
    return response.json();
  },
  
  async post(endpoint, data) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      },
      body: JSON.stringify(data)
    });
    return response.json();
  }
};
```

### Vue

```javascript
// src/plugins/api.js
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1'
});

api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default {
  install: (app) => {
    app.config.globalProperties.$api = api;
  }
};
```

### Angular

```typescript
// src/app/services/api.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = environment.apiUrl || 'http://localhost:8080/api/v1';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('accessToken');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  get<T>(endpoint: string) {
    return this.http.get<T>(`${this.baseUrl}${endpoint}`, {
      headers: this.getHeaders()
    });
  }

  post<T>(endpoint: string, data: any) {
    return this.http.post<T>(`${this.baseUrl}${endpoint}`, data, {
      headers: this.getHeaders()
    });
  }
}
```

---

## 🧪 Primer Test

### Test de Conexión

```javascript
async function testConnection() {
  try {
    const response = await fetch('http://localhost:8080/actuator/health');
    const data = await response.json();
    
    if (data.status === 'UP') {
      console.log('✅ Conexión exitosa con el backend');
      return true;
    }
  } catch (error) {
    console.error('❌ Error de conexión:', error);
    return false;
  }
}

// Ejecutar al cargar la app
testConnection();
```

### Test de Registro

```javascript
async function testRegister() {
  try {
    const response = await fetch('http://localhost:8080/api/v1/athletes/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        nombre: 'Test User',
        email: `test${Date.now()}@example.com`,
        password: 'Test123456'
      })
    });

    const data = await response.json();
    console.log('✅ Registro exitoso:', data);
    return data;
  } catch (error) {
    console.error('❌ Error en registro:', error);
  }
}
```

---

## 📝 Variables de Entorno

### React (.env)

```env
REACT_APP_API_URL=http://localhost:8080/api/v1
REACT_APP_GOOGLE_CLIENT_ID=tu-client-id.apps.googleusercontent.com
```

### Vue (.env)

```env
VITE_API_URL=http://localhost:8080/api/v1
VITE_GOOGLE_CLIENT_ID=tu-client-id.apps.googleusercontent.com
```

### Angular (environment.ts)

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  googleClientId: 'tu-client-id.apps.googleusercontent.com'
};
```

---

## 🚨 Manejo de Errores Global

```javascript
// utils/errorHandler.js
export function handleApiError(error) {
  if (error.message.includes('401')) {
    // Token expirado o inválido
    localStorage.removeItem('accessToken');
    window.location.href = '/login';
    return 'Sesión expirada. Por favor inicia sesión nuevamente.';
  }
  
  if (error.message.includes('400')) {
    return 'Datos inválidos. Verifica la información.';
  }
  
  if (error.message.includes('404')) {
    return 'Recurso no encontrado.';
  }
  
  if (error.message.includes('409')) {
    return 'El recurso ya existe.';
  }
  
  if (error.message.includes('500')) {
    return 'Error del servidor. Intenta más tarde.';
  }
  
  return 'Error de conexión. Verifica tu internet.';
}
```

---

## ✅ Checklist de Configuración

- [ ] Backend corriendo en http://localhost:8080
- [ ] Health check responde correctamente
- [ ] Variables de entorno configuradas
- [ ] Función de API request implementada
- [ ] Manejo de errores configurado
- [ ] Headers de autenticación funcionando
- [ ] Test de conexión exitoso

---

## 🔗 Próximos Pasos

1. **Autenticación:** Lee [01-autenticacion.md](01-autenticacion.md)
2. **Crear perfil:** Lee [02-perfiles-jugadores.md](02-perfiles-jugadores.md)
3. **Gestionar partidos:** Lee [04-partidos.md](04-partidos.md)

---

**Volver al índice:** [README.md](README.md)
