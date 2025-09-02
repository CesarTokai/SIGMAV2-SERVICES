# Sistema de Manejo de Respuestas JWT Mejorado

## 🎯 Mejoras Implementadas

### 1. **Respuestas JSON Estandarizadas**
Todas las respuestas ahora siguen un formato consistente:

```json
{
  "success": true/false,
  "message": "Mensaje descriptivo",
  "data": { ... },
  "error": {
    "code": "ERROR_CODE",
    "message": "Mensaje de error",
    "details": "Detalles adicionales",
    "expiredAt": "2025-09-01T10:30:00" // Solo para tokens expirados
  },
  "timestamp": "2025-09-01T10:30:00"
}
```

### 2. **Excepciones JWT Específicas**
- `TokenExpiredException` - Token expirado
- `TokenInvalidException` - Token inválido o modificado  
- `TokenMissingException` - Token faltante
- `TokenMalformedException` - Formato de token incorrecto
- `JwtException` - Excepción base para errores JWT

### 3. **Filtro JWT Mejorado**
El nuevo `JwtAuthenticationFilter` proporciona:
- Validación completa del header Authorization
- Respuestas JSON detalladas para cada tipo de error
- Manejo específico de tokens expirados con fecha de expiración
- Logging apropiado para debugging

## 🧪 Endpoints de Prueba

### Endpoint de Salud (Público)
```http
GET /api/sigmav2/auth/health
```

### Validación Manual de Token
```http
POST /api/sigmav2/auth/validate-token
Authorization: Bearer <tu-token>
```

### Endpoint Protegido de Prueba
```http
GET /api/sigmav2/auth/test-protected
Authorization: Bearer <tu-token>
```

## 📝 Ejemplos de Respuestas

### ✅ Token Válido
```json
{
  "success": true,
  "message": "Acceso autorizado correctamente",
  "data": {
    "message": "Acceso autorizado correctamente",
    "timestamp": "2025-09-01T10:30:00",
    "authenticated": true
  },
  "timestamp": "2025-09-01T10:30:00"
}
```

### ❌ Token Expirado
```json
{
  "success": false,
  "error": {
    "code": "TOKEN_EXPIRED",
    "message": "Token expirado",
    "details": "Su sesión ha expirado, por favor inicie sesión nuevamente",
    "expiredAt": "2025-09-01T09:30:00"
  },
  "timestamp": "2025-09-01T10:30:00"
}
```

### ❌ Token Faltante
```json
{
  "success": false,
  "error": {
    "code": "TOKEN_MISSING",
    "message": "Token de autenticación requerido",
    "details": "Debe proporcionar un token válido en el header Authorization"
  },
  "timestamp": "2025-09-01T10:30:00"
}
```

### ❌ Token Malformado
```json
{
  "success": false,
  "error": {
    "code": "TOKEN_MALFORMED",
    "message": "Formato de token inválido",
    "details": "El token debe estar en formato: Bearer <token>"
  },
  "timestamp": "2025-09-01T10:30:00"
}
```

### ❌ Token Inválido
```json
{
  "success": false,
  "error": {
    "code": "TOKEN_INVALID",
    "message": "Token inválido",
    "details": "El token proporcionado no es válido o ha sido modificado"
  },
  "timestamp": "2025-09-01T10:30:00"
}
```

## 🔧 Casos de Uso

### 1. **Sin Token**
```bash
curl -X GET http://localhost:8080/api/sigmav2/auth/test-protected
```

### 2. **Token Malformado** 
```bash
curl -X GET http://localhost:8080/api/sigmav2/auth/test-protected \
  -H "Authorization: token_sin_bearer"
```

### 3. **Token Inválido**
```bash
curl -X GET http://localhost:8080/api/sigmav2/auth/test-protected \
  -H "Authorization: Bearer token_invalido"
```

### 4. **Token Válido**
```bash
curl -X GET http://localhost:8080/api/sigmav2/auth/test-protected \
  -H "Authorization: Bearer <token_valido>"
```

## 🎨 Frontend - Manejo de Respuestas

### JavaScript/TypeScript
```typescript
const handleApiResponse = (response: any) => {
  if (response.success) {
    // Éxito
    console.log(response.message);
    return response.data;
  } else {
    // Error
    const error = response.error;
    
    switch(error.code) {
      case 'TOKEN_EXPIRED':
        // Redirigir a login
        localStorage.removeItem('token');
        window.location.href = '/login';
        break;
        
      case 'TOKEN_MISSING':
      case 'TOKEN_INVALID':
      case 'TOKEN_MALFORMED':
        // Token inválido, limpiar storage
        localStorage.removeItem('token');
        window.location.href = '/login';
        break;
        
      default:
        // Mostrar error genérico
        showErrorMessage(error.message);
    }
  }
};
```

## 🚀 Beneficios

1. **Respuestas Consistentes**: Formato uniforme en toda la API
2. **Mejor UX**: Mensajes claros y códigos de error específicos  
3. **Debugging Facilitado**: Logs detallados y códigos de error únicos
4. **Manejo de Tokens Robusto**: Validación completa y respuestas apropiadas
5. **Compatibilidad Frontend**: Estructura JSON fácil de manejar

## 🔒 Códigos de Estado HTTP

- `200 OK` - Operación exitosa
- `400 Bad Request` - Error en la solicitud  
- `401 Unauthorized` - Error de autenticación/token
- `403 Forbidden` - Sin permisos suficientes
- `404 Not Found` - Recurso no encontrado
- `409 Conflict` - Conflicto (ej: usuario ya existe)
- `500 Internal Server Error` - Error interno del servidor
