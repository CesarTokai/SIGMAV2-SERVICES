# Guía de Integración Frontend - Sistema de Logout y Revocación

## Para Desarrolladores Frontend

Esta guía explica cómo implementar el logout correctamente en tu aplicación frontend para que funcione con el nuevo sistema de revocación de tokens.

---

## API Endpoint

```
POST /api/auth/logout
```

**Headers:**
- `Authorization: Bearer {token}` ← Token JWT actual del usuario
- `Content-Type: application/json`

**Response Success (200):**
```json
{
  "success": true,
  "message": "Sesión cerrada exitosamente",
  "data": null,
  "timestamp": "2025-11-04T10:30:00"
}
```

**Response Token Revocado (401):**
```json
{
  "success": false,
  "error": {
    "code": "TOKEN_REVOKED",
    "message": "El token ha sido revocado",
    "details": "Este token ya no es válido. Por favor, inicie sesión nuevamente."
  },
  "timestamp": "2025-11-04T10:30:00"
}
```

---

## Implementación en JavaScript/TypeScript

### Ejemplo con Fetch API

```javascript
// logout.js
async function logout() {
  try {
    // Obtener token del almacenamiento local
    const token = localStorage.getItem('authToken');

    if (!token) {
      console.warn('No hay token para cerrar sesión');
      redirectToLogin();
      return;
    }

    // Llamar al endpoint de logout
    const response = await fetch('http://localhost:8080/api/auth/logout', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();

    if (response.ok && data.success) {
      console.log('Logout exitoso:', data.message);

      // Limpiar almacenamiento local
      localStorage.removeItem('authToken');
      localStorage.removeItem('userData');

      // Redirigir a login
      redirectToLogin();
    } else {
      console.error('Error en logout:', data.error);

      // Aún si falla, limpiamos el token y redirigimos
      localStorage.removeItem('authToken');
      redirectToLogin();
    }
  } catch (error) {
    console.error('Error de red en logout:', error);

    // En caso de error, limpiamos de todos modos
    localStorage.removeItem('authToken');
    redirectToLogin();
  }
}

function redirectToLogin() {
  window.location.href = '/login.html';
}

// Exponer función globalmente si es necesario
window.logout = logout;
```

### Ejemplo con Axios

```typescript
// auth.service.ts
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

export async function logout(): Promise<void> {
  try {
    const token = localStorage.getItem('authToken');

    if (!token) {
      throw new Error('No hay sesión activa');
    }

    const response = await axios.post(
      `${API_BASE_URL}/api/auth/logout`,
      {},
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    );

    console.log('Logout exitoso:', response.data.message);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      console.warn('Token ya estaba revocado o expirado');
    } else {
      console.error('Error en logout:', error);
    }
  } finally {
    // SIEMPRE limpiar el almacenamiento local
    localStorage.removeItem('authToken');
    localStorage.removeItem('userData');

    // Redirigir a login
    window.location.href = '/login';
  }
}
```

### Ejemplo con React + Context API

```tsx
// AuthContext.tsx
import React, { createContext, useContext, useState } from 'react';
import axios from 'axios';

interface AuthContextType {
  token: string | null;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(
    localStorage.getItem('authToken')
  );

  const logout = async () => {
    if (!token) {
      console.warn('No hay token para cerrar sesión');
      return;
    }

    try {
      await axios.post(
        'http://localhost:8080/api/auth/logout',
        {},
        {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        }
      );

      console.log('Logout exitoso');
    } catch (error) {
      console.error('Error al hacer logout:', error);
    } finally {
      // Siempre limpiar estado y almacenamiento
      setToken(null);
      localStorage.removeItem('authToken');
      localStorage.removeItem('userData');
    }
  };

  return (
    <AuthContext.Provider value={{
      token,
      logout,
      isAuthenticated: !!token
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return context;
}

// Componente de uso
function LogoutButton() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <button onClick={handleLogout}>
      Cerrar Sesión
    </button>
  );
}
```

---

## Manejo de Errores Comunes

### 1. Token Revocado (401)

Cuando el servidor responde con `TOKEN_REVOKED`, significa que el token ya fue invalidado (por ejemplo, se hizo logout desde otro dispositivo).

**Acción**: Limpiar almacenamiento y redirigir a login

```javascript
if (error.response?.data?.error?.code === 'TOKEN_REVOKED') {
  console.log('Sesión ya cerrada desde otro dispositivo');
  localStorage.clear();
  window.location.href = '/login';
}
```

### 2. Token Expirado (401)

```javascript
if (error.response?.data?.error?.code === 'TOKEN_EXPIRED') {
  console.log('Tu sesión ha expirado');
  localStorage.clear();
  window.location.href = '/login?reason=expired';
}
```

### 3. Error de Red

```javascript
if (error.message === 'Network Error') {
  console.log('No se pudo conectar al servidor');
  // Aún así limpiar token local
  localStorage.removeItem('authToken');
  window.location.href = '/login?reason=network';
}
```

---

## Interceptor Global de Axios

Para manejar automáticamente tokens revocados/expirados en TODAS las peticiones:

```javascript
// axiosConfig.js
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080'
});

// Interceptor de request: agregar token automáticamente
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor de response: manejar errores de autenticación
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const errorCode = error.response.data?.error?.code;

      if (errorCode === 'TOKEN_REVOKED' || errorCode === 'TOKEN_EXPIRED') {
        console.warn('Sesión inválida, redirigiendo a login');

        // Limpiar almacenamiento
        localStorage.clear();

        // Redirigir a login
        window.location.href = '/login?reason=session_expired';
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

**Uso:**
```javascript
import apiClient from './axiosConfig';

// Todas las peticiones usan el interceptor automáticamente
async function getUserProfile() {
  const response = await apiClient.get('/api/sigmav2/users/profile');
  return response.data;
}
```

---

## Best Practices

### ✅ DO (Hacer)

1. **Siempre llamar al endpoint de logout** antes de limpiar el token del cliente
   ```javascript
   await api.post('/api/auth/logout');
   localStorage.removeItem('authToken');
   ```

2. **Usar try-finally** para asegurar limpieza incluso si el logout falla
   ```javascript
   try {
     await logout();
   } finally {
     localStorage.clear();
     redirectToLogin();
   }
   ```

3. **Manejar el caso de token ya revocado** sin mostrar error al usuario
   ```javascript
   if (error.code === 'TOKEN_REVOKED') {
     // Es esperado, solo limpiar y continuar
     localStorage.clear();
   }
   ```

4. **Implementar interceptor global** para manejar 401 en toda la app

5. **Mostrar mensaje amigable** al usuario
   ```javascript
   showToast('Sesión cerrada exitosamente', 'success');
   ```

### ❌ DON'T (No Hacer)

1. **No solo limpiar localStorage sin llamar al logout**
   ```javascript
   // ❌ MAL: Token sigue válido en el servidor
   localStorage.clear();
   ```

2. **No ignorar errores del logout**
   ```javascript
   // ❌ MAL: Podría dejar el token activo
   logout().catch(() => {}); // Silenciar errores sin limpiar
   ```

3. **No hacer logout en cada recarga de página**
   ```javascript
   // ❌ MAL: Cierra sesión innecesariamente
   window.onbeforeunload = () => logout();
   ```

4. **No dejar el token en memoria después de logout**
   ```javascript
   // ❌ MAL: Token aún accesible
   await logout();
   // Olvidó: localStorage.removeItem('authToken');
   ```

---

## Testing Manual

### Caso 1: Logout Normal
1. Login en la aplicación
2. Hacer acciones normales (verificar que funcionan)
3. Click en botón "Cerrar Sesión"
4. Verificar redirección a login
5. Intentar acceder a ruta protegida → debe redirigir a login
6. Login nuevamente → debe funcionar

### Caso 2: Logout desde Múltiples Dispositivos
1. Login en navegador A
2. Login en navegador B (mismo usuario)
3. Logout desde navegador A
4. Intentar hacer acción en navegador B → debe fallar con 401
5. Verificar que navegador B redirija a login automáticamente

### Caso 3: Token Expirado Naturalmente
1. Login en la aplicación
2. Esperar 24 horas (o modificar expiración a 1 min para prueba)
3. Intentar hacer acción → debe fallar con TOKEN_EXPIRED
4. Verificar redirección automática a login

---

## Ejemplo de Página Completa (HTML + JS)

```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Dashboard - SIGMAV2</title>
</head>
<body>
  <div id="app">
    <header>
      <h1>Dashboard</h1>
      <button id="logoutBtn">Cerrar Sesión</button>
    </header>
    <main id="content">
      <!-- Contenido de la app -->
    </main>
  </div>

  <script>
    const API_BASE = 'http://localhost:8080';

    // Verificar autenticación al cargar
    window.addEventListener('DOMContentLoaded', () => {
      const token = localStorage.getItem('authToken');

      if (!token) {
        window.location.href = '/login.html';
        return;
      }

      // Configurar botón de logout
      document.getElementById('logoutBtn').addEventListener('click', handleLogout);

      // Cargar datos iniciales
      loadDashboardData();
    });

    async function handleLogout() {
      const token = localStorage.getItem('authToken');

      if (!token) {
        window.location.href = '/login.html';
        return;
      }

      try {
        const response = await fetch(`${API_BASE}/api/auth/logout`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });

        const data = await response.json();

        if (data.success) {
          console.log('✓ Logout exitoso');
        } else {
          console.warn('Advertencia en logout:', data.error);
        }
      } catch (error) {
        console.error('Error en logout:', error);
      } finally {
        // SIEMPRE limpiar y redirigir
        localStorage.removeItem('authToken');
        localStorage.removeItem('userData');
        window.location.href = '/login.html';
      }
    }

    async function loadDashboardData() {
      const token = localStorage.getItem('authToken');

      try {
        const response = await fetch(`${API_BASE}/api/sigmav2/users/profile`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (!response.ok) {
          if (response.status === 401) {
            console.warn('Token inválido o expirado');
            localStorage.clear();
            window.location.href = '/login.html';
          }
          throw new Error('Error al cargar datos');
        }

        const data = await response.json();
        // Renderizar datos...
      } catch (error) {
        console.error('Error cargando dashboard:', error);
      }
    }
  </script>
</body>
</html>
```

---

## Resumen

✅ **Llamar siempre** `POST /api/auth/logout` con el token en el header
✅ **Limpiar localStorage** después del logout (en el `finally`)
✅ **Redirigir a login** inmediatamente
✅ **Manejar errores 401** con interceptor global
✅ **Probar en múltiples dispositivos** para verificar revocación

Con esto tu frontend estará completamente integrado con el nuevo sistema de revocación de tokens del backend.

