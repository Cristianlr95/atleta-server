# Utilidades y Helpers - API Sistema Atleta

## 🔧 API Client Completo

```javascript
// api-client.js
class ApiClient {
  constructor(baseURL = 'http://localhost:8080/api/v1') {
    this.baseURL = baseURL;
  }

  getHeaders(includeAuth = true) {
    const headers = { 'Content-Type': 'application/json' };
    if (includeAuth) {
      const token = localStorage.getItem('accessToken');
      if (token) headers['Authorization'] = `Bearer ${token}`;
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

    const response = await fetch(url, config);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return response.json();
  }

  // Autenticación
  register(nombre, email, password) {
    return this.request('/athletes/register', {
      method: 'POST',
      auth: false,
      body: JSON.stringify({ nombre, email, password })
    });
  }

  login(email, password) {
    return this.request('/athletes/login', {
      method: 'POST',
      auth: false,
      body: JSON.stringify({ email, password })
    });
  }

  loginWithGoogle(idToken) {
    return this.request('/athletes/auth/google', {
      method: 'POST',
      auth: false,
      body: JSON.stringify({ idToken })
    });
  }

  // Perfiles
  createProfile(atletaUuid, alias) {
    return this.request('/player-profiles', {
      method: 'POST',
      body: JSON.stringify({ atletaUuid, alias })
    });
  }

  getProfile(uuid) {
    return this.request(`/player-profiles/${uuid}`);
  }

  // Partidos
  createMatch(matchData) {
    return this.request('/matches', {
      method: 'POST',
      body: JSON.stringify(matchData)
    });
  }

  joinMatch(matchData) {
    return this.request('/matches/join', {
      method: 'POST',
      body: JSON.stringify(matchData)
    });
  }

  // Calificaciones
  getPlayerOVR(uuid) {
    return this.request(`/ratings/player/${uuid}/overall`);
  }

  getPlayerHistory(uuid) {
    return this.request(`/ratings/player/${uuid}/history`);
  }
}

export const api = new ApiClient();
```

---

## 🚨 Manejo de Errores

```javascript
// error-handler.js
export function handleApiError(error) {
  const status = error.message.match(/\d+/)?.[0];
  
  const messages = {
    '400': 'Datos inválidos. Verifica la información.',
    '401': 'Sesión expirada. Inicia sesión nuevamente.',
    '404': 'Recurso no encontrado.',
    '409': 'El recurso ya existe.',
    '500': 'Error del servidor. Intenta más tarde.'
  };
  
  const message = messages[status] || 'Error de conexión.';
  
  if (status === '401') {
    localStorage.clear();
    window.location.href = '/login';
  }
  
  return message;
}

// Uso
try {
  await api.register(nombre, email, password);
} catch (error) {
  const message = handleApiError(error);
  alert(message);
}
```

---

## 📝 Validaciones

```javascript
// validators.js
export const validators = {
  email(email) {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
  },
  
  password(password) {
    return password.length >= 8 && password.length <= 100;
  },
  
  nombre(nombre) {
    return nombre.length > 0 && nombre.length <= 100;
  },
  
  alias(alias) {
    return alias.length > 0 && alias.length <= 50;
  },
  
  uuid(uuid) {
    const regex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    return regex.test(uuid);
  }
};
```

---

## 🔄 Interceptores

```javascript
// interceptors.js
export function setupInterceptors() {
  const originalFetch = window.fetch;
  
  window.fetch = async (...args) => {
    // Request interceptor
    console.log('Request:', args[0]);
    
    try {
      const response = await originalFetch(...args);
      
      // Response interceptor
      if (!response.ok) {
        console.error('Response error:', response.status);
      }
      
      return response;
    } catch (error) {
      console.error('Network error:', error);
      throw error;
    }
  };
}
```

---

## 💾 Storage Helper

```javascript
// storage.js
export const storage = {
  set(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
  },
  
  get(key) {
    const item = localStorage.getItem(key);
    return item ? JSON.parse(item) : null;
  },
  
  remove(key) {
    localStorage.removeItem(key);
  },
  
  clear() {
    localStorage.clear();
  },
  
  // Específicos
  setUser(user) {
    this.set('user', user);
    this.set('atletaUuid', user.atletaUuid);
    if (user.accessToken) {
      this.set('accessToken', user.accessToken);
    }
  },
  
  getUser() {
    return this.get('user');
  },
  
  isAuthenticated() {
    return this.get('accessToken') !== null;
  },
  
  logout() {
    this.clear();
    window.location.href = '/login';
  }
};
```

---

## 🎨 Formatters

```javascript
// formatters.js
export const formatters = {
  date(dateString) {
    return new Date(dateString).toLocaleDateString('es-ES');
  },
  
  datetime(dateString) {
    return new Date(dateString).toLocaleString('es-ES');
  },
  
  ovr(value) {
    return parseFloat(value).toFixed(2);
  },
  
  percentage(value) {
    return `${(value * 100).toFixed(0)}%`;
  },
  
  classification(ovr) {
    if (ovr >= 95) return 'LEYENDA';
    if (ovr >= 85) return 'ÉLITE';
    if (ovr >= 75) return 'EXPERTO';
    if (ovr >= 65) return 'AVANZADO';
    if (ovr >= 55) return 'INTERMEDIO';
    if (ovr >= 50) return 'PRINCIPIANTE';
    return 'NOVATO';
  }
};
```

---

## 🔔 Notificaciones

```javascript
// notifications.js
export const notify = {
  success(message) {
    // Implementar con tu librería favorita
    console.log('✅', message);
  },
  
  error(message) {
    console.error('❌', message);
  },
  
  info(message) {
    console.info('ℹ️', message);
  },
  
  warning(message) {
    console.warn('⚠️', message);
  }
};
```

---

## 🧪 Testing Helpers

```javascript
// test-helpers.js
export async function testConnection() {
  try {
    const response = await fetch('http://localhost:8080/actuator/health');
    const data = await response.json();
    return data.status === 'UP';
  } catch {
    return false;
  }
}

export async function createTestUser() {
  const timestamp = Date.now();
  return api.register(
    'Test User',
    `test${timestamp}@example.com`,
    'Test123456'
  );
}
```

---

## 📊 Constants

```javascript
// constants.js
export const API_BASE_URL = 'http://localhost:8080/api/v1';

export const MODALIDADES = {
  CINCO_VS_CINCO: '5v5',
  SEIS_VS_SEIS: '6v6',
  SIETE_VS_SIETE: '7v7'
};

export const ROLES = {
  ATAQUE: 'Delantero',
  MEDIOCAMPO: 'Mediocampista',
  CARRILERO: 'Lateral',
  DEFENSA: 'Defensor',
  ARQUERO: 'Portero',
  DT: 'Director Técnico'
};

export const ESTADOS_PARTIDO = {
  CREADO: 'Creado',
  INICIADO: 'En curso',
  FINALIZADO: 'Finalizado',
  INVALIDO: 'Cancelado'
};

export const CLASIFICACIONES = {
  LEYENDA: { min: 95, color: '#FFD700' },
  ÉLITE: { min: 85, color: '#FF6B6B' },
  EXPERTO: { min: 75, color: '#4ECDC4' },
  AVANZADO: { min: 65, color: '#95E1D3' },
  INTERMEDIO: { min: 55, color: '#A8E6CF' },
  PRINCIPIANTE: { min: 50, color: '#DCEDC1' },
  NOVATO: { min: 0, color: '#C7CEEA' }
};
```

---

## 🎯 Ejemplo de Uso Completo

```javascript
import { api } from './api-client.js';
import { storage } from './storage.js';
import { handleApiError } from './error-handler.js';
import { validators } from './validators.js';
import { notify } from './notifications.js';

async function completeFlow() {
  try {
    // 1. Validar datos
    if (!validators.email('juan@example.com')) {
      throw new Error('Email inválido');
    }
    
    // 2. Registrar
    const user = await api.register('Juan', 'juan@example.com', 'Pass123');
    storage.setUser(user);
    notify.success('Registro exitoso');
    
    // 3. Crear perfil
    const profile = await api.createProfile(user.atletaUuid, 'JuanGol');
    notify.success('Perfil creado');
    
    // 4. Obtener OVR
    const ovr = await api.getPlayerOVR(user.atletaUuid);
    console.log(`Tu OVR es: ${ovr.hybridOVR}`);
    
  } catch (error) {
    const message = handleApiError(error);
    notify.error(message);
  }
}
```

---

**Volver al índice:** [README.md](README.md)
