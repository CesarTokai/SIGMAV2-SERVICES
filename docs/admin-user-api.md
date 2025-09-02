# API de Administración de Usuarios - SIGMAV2

## Endpoints de Administración

**Base URL**: `/api/sigmav2/admin/users`

**Autenticación**: Requiere rol `ADMIN`

---

## 📋 **CRUD Básico**

### 1. **Listar Usuarios**
```http
GET /api/sigmav2/admin/users
```

**Parámetros de consulta:**
- `page` (int): Número de página (default: 0)
- `size` (int): Elementos por página (default: 20)
- `sortBy` (string): Campo para ordenar (default: "createdAt")
- `sortDir` (string): Dirección del ordenamiento - "asc"|"desc" (default: "desc")
- `email` (string): Filtrar por email
- `role` (string): Filtrar por rol - "USER"|"ADMIN"
- `verified` (boolean): Filtrar por estado de verificación
- `status` (boolean): Filtrar por estado activo

**Respuesta:**
```json
{
  "users": [
    {
      "id": 1,
      "email": "usuario@ejemplo.com",
      "role": "USER",
      "status": true,
      "verified": true,
      "attempts": 0,
      "lastTryAt": null,
      "createdAt": "2025-09-02T10:00:00",
      "updatedAt": "2025-09-02T10:00:00",
      "verificationCode": null,
      "totalVerificationCodes": 2,
      "lastVerificationCodeSent": "2025-09-02T09:00:00",
      "accountLocked": false
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 20,
  "hasNext": true,
  "hasPrevious": false,
  "totalVerifiedUsers": 45,
  "totalUnverifiedUsers": 5,
  "totalActiveUsers": 48,
  "totalInactiveUsers": 2
}
```

### 2. **Crear Usuario**
```http
POST /api/sigmav2/admin/users
```

**Body:**
```json
{
  "email": "nuevo@ejemplo.com",
  "password": "password123",
  "role": "USER",
  "preVerified": false,
  "sendWelcomeEmail": true
}
```

### 3. **Obtener Usuario por ID**
```http
GET /api/sigmav2/admin/users/{userId}
```

### 4. **Actualizar Usuario**
```http
PUT /api/sigmav2/admin/users/{userId}
```

**Body:**
```json
{
  "email": "nuevo-email@ejemplo.com",
  "role": "ADMIN",
  "status": true,
  "verified": true,
  "resetAttempts": true,
  "adminNotes": "Usuario promovido a administrador"
}
```

### 5. **Eliminar Usuario**
```http
DELETE /api/sigmav2/admin/users/{userId}
```

---

## ⚡ **Acciones Especiales**

### 6. **Forzar Verificación**
```http
POST /api/sigmav2/admin/users/{userId}/force-verify
```

Marca un usuario como verificado sin necesidad de código.

### 7. **Resetear Intentos**
```http
POST /api/sigmav2/admin/users/{userId}/reset-attempts
```

Resetea el contador de intentos fallidos de verificación.

### 8. **Cambiar Estado (Activar/Desactivar)**
```http
POST /api/sigmav2/admin/users/{userId}/toggle-status
```

Alterna entre activo/inactivo la cuenta del usuario.

### 9. **Reenviar Código de Verificación**
```http
POST /api/sigmav2/admin/users/{userId}/resend-verification
```

Reenvía código de verificación sin restricciones de rate limiting.

---

## 📊 **Estadísticas y Reportes**

### 10. **Estadísticas Generales**
```http
GET /api/sigmav2/admin/users/statistics
```

**Respuesta:**
```json
{
  "success": true,
  "data": {
    "totalUsers": 100,
    "verifiedUsers": 85,
    "unverifiedUsers": 15,
    "activeUsers": 95,
    "inactiveUsers": 5,
    "oldUnverifiedUsers": 8,
    "verificationRate": 85.0
  }
}
```

### 11. **Limpieza de Usuarios No Verificados**
```http
DELETE /api/sigmav2/admin/users/cleanup-unverified?daysOld=30
```

Elimina usuarios no verificados más antiguos que X días.

---

## 🔍 **Ejemplos de Uso**

### Buscar usuarios no verificados:
```bash
GET /api/sigmav2/admin/users?verified=false&page=0&size=10
```

### Buscar administradores activos:
```bash
GET /api/sigmav2/admin/users?role=ADMIN&status=true
```

### Usuarios creados recientemente:
```bash
GET /api/sigmav2/admin/users?sortBy=createdAt&sortDir=desc&size=5
```

### Buscar por email específico:
```bash
GET /api/sigmav2/admin/users?email=usuario@ejemplo.com
```

---

## 🛡️ **Seguridad**

### Autorización
- Todos los endpoints requieren autenticación
- Solo usuarios con rol `ADMIN` pueden acceder
- Se registran todas las acciones administrativas en logs

### Validaciones
- Emails deben tener formato válido
- Roles solo pueden ser "USER" o "ADMIN"
- Contraseñas mínimo 8 caracteres al crear usuarios

### Auditoría
- Todas las operaciones se registran con:
  - ID del administrador que ejecuta la acción
  - Timestamp de la operación
  - Usuario afectado
  - Tipo de operación

---

## 📱 **Casos de Uso Típicos**

### 1. **Gestión Diaria**
```bash
# Ver usuarios pendientes de verificación
GET /admin/users?verified=false

# Verificar usuario manualmente
POST /admin/users/123/force-verify

# Revisar estadísticas
GET /admin/users/statistics
```

### 2. **Mantenimiento**
```bash
# Limpiar usuarios antiguos no verificados
DELETE /admin/users/cleanup-unverified?daysOld=7

# Resetear intentos de usuarios bloqueados
POST /admin/users/456/reset-attempts
```

### 3. **Soporte al Usuario**
```bash
# Buscar usuario por email
GET /admin/users?email=usuario@problema.com

# Reenviar código manualmente
POST /admin/users/789/resend-verification

# Activar cuenta desactivada
POST /admin/users/789/toggle-status
```

---

## 📋 **Respuestas de Error**

### 401 - No autorizado
```json
{
  "success": false,
  "message": "Token de autenticación requerido"
}
```

### 403 - Acceso denegado
```json
{
  "success": false,
  "message": "Requiere privilegios de administrador"
}
```

### 404 - Usuario no encontrado
```json
{
  "success": false,
  "message": "Usuario no encontrado con ID: 123"
}
```

### 400 - Datos inválidos
```json
{
  "success": false,
  "message": "El formato del email no es válido"
}
```

---

## 🚀 **Panel de Administración Frontend**

Para implementar un panel de administración, estos endpoints proporcionan:

1. **Dashboard**: Estadísticas generales
2. **Tabla de usuarios**: Lista paginada con filtros
3. **Acciones masivas**: Selección múltiple para operaciones
4. **Detalles de usuario**: Vista completa con historial
5. **Herramientas de mantenimiento**: Limpieza y reportes

### Tecnologías sugeridas:
- **React/Vue/Angular** para el frontend
- **Tanstack Table** para tabla con filtros avanzados
- **Chart.js** para gráficos de estadísticas
- **Axios** para llamadas a la API
