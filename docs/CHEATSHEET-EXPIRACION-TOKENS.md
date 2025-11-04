# 🎯 CHEATSHEET: Expiración vs Revocación de Tokens

## Pregunta Frecuente
**"¿Cómo se actualiza cuando un usuario ya se agotó el tiempo de su token?"**

**Respuesta Corta:** NO se actualiza. El token se valida en cada petición y si `exp < ahora` → 401 Unauthorized.

---

## 🔄 Dos Formas de Invalidar un Token

### 1️⃣ Expiración Natural (Por Tiempo)
```
✅ Automática - NO requiere acción del servidor
✅ Validación local - NO consulta base de datos
✅ Rápida - Compara fechas dentro del token
```

**Cuándo ocurre:** Después de 24 horas desde la creación
**Dónde se valida:** `JwtUtils.validateToken()` → verifica `exp < now()`
**Qué pasa:** Lanza `TokenExpiredException` → 401 Unauthorized
**Se guarda en BD:** ❌ NO (no es necesario)

**Flujo:**
```
Token creado con exp=2025-11-05 10:00
          ↓
     24 horas pasan
          ↓
Cliente intenta usar token (2025-11-05 11:00)
          ↓
JwtUtils.validateToken() → exp < now() → EXPIRADO
          ↓
TokenExpiredException → 401 Unauthorized
          ↓
Cliente recibe 401 → Limpia localStorage → Login
```

### 2️⃣ Revocación Manual (Logout/Seguridad)
```
⚠️ Manual - Usuario hace logout o admin revoca
⚠️ Consulta BD - Verifica tabla revoked_tokens
⚠️ Inmediata - Antes de expiración natural
```

**Cuándo ocurre:** Usuario hace `POST /api/auth/logout`
**Dónde se valida:** `JwtRevocationFilter` → consulta `revoked_tokens`
**Qué pasa:** Responde inmediatamente `401 TOKEN_REVOKED`
**Se guarda en BD:** ✅ SÍ (tabla `revoked_tokens`)

**Flujo:**
```
Usuario hace logout (hora 2 de 24)
          ↓
Backend guarda JTI en revoked_tokens
          ↓
Cliente intenta usar mismo token
          ↓
JwtRevocationFilter → Consulta BD → JTI encontrado
          ↓
401 TOKEN_REVOKED (sin llegar a validar exp)
```

---

## 📊 Tabla Comparativa Rápida

| Característica | Expiración Natural | Revocación Manual |
|----------------|-------------------|-------------------|
| **Trigger** | Tiempo transcurrido (24h) | Usuario/Admin hace logout |
| **Validación** | Fecha `exp` en token | Consulta tabla `revoked_tokens` |
| **Consulta BD** | ❌ NO | ✅ SÍ |
| **Velocidad** | ⚡ Muy rápida | 🐢 Requiere BD query |
| **Filtro** | JwtAuthenticationFilter | JwtRevocationFilter |
| **Almacenamiento** | Ninguno | BD hasta que expire |
| **Código error** | `TOKEN_EXPIRED` | `TOKEN_REVOKED` |

---

## 🔍 Orden de Validación en Cada Request

```
1. JwtRevocationFilter
   ├─ Extrae JTI del token
   ├─ Consulta: SELECT jti FROM revoked_tokens WHERE jti = ?
   ├─ Si encontrado → 401 TOKEN_REVOKED (STOP)
   └─ Si NO encontrado → Continúa

2. JwtAuthenticationFilter
   ├─ Valida firma HMAC256
   ├─ Valida exp > now() ← Aquí se detecta expiración natural
   ├─ Si exp < now() → TokenExpiredException → 401 TOKEN_EXPIRED (STOP)
   ├─ Si válido → Establece Authentication
   └─ Continúa al controlador
```

---

## 💡 Código Clave

### Creación del Token (Login)
```java
// JwtUtils.createToken() - línea 52
.withExpiresAt(new Date(System.currentTimeMillis() + 86400000)) // +24h
```

### Validación de Expiración
```java
// JwtUtils.validateToken() - línea 58-70
return JWT.require(algorithm)
    .withIssuer(userGenerator)
    .build()
    .verify(token); // ← Aquí verifica exp < now()
```

### Captura de Token Expirado
```java
// JwtUtils.validateToken() - línea 73-76
catch (com.auth0.jwt.exceptions.TokenExpiredException e) {
    throw new TokenExpiredException(e.getMessage(), expiredAt);
}
```

---

## 🎬 Ejemplos de Respuestas

### Token Expirado (Natural)
```json
{
  "success": false,
  "error": {
    "code": "TOKEN_EXPIRED",
    "message": "El token ha expirado",
    "details": "El token expiró naturalmente después de 24 horas",
    "expiredAt": "2025-11-05T10:00:00"
  }
}
```

### Token Revocado (Logout)
```json
{
  "success": false,
  "error": {
    "code": "TOKEN_REVOKED",
    "message": "El token ha sido revocado",
    "details": "Este token ya no es válido. Por favor, inicie sesión nuevamente."
  }
}
```

---

## ⏱️ Timeline Visual

```
T=0h    T=2h         T=24h              T=25h
│       │            │                  │
│ Login │ Logout     │ Expira           │ Intento
│       │ (manual)   │ (natural)        │
│       │            │                  │
├───────┼────────────┼──────────────────┼────────>
│       │            │                  │
│ Token │ Revocado   │ Expirado         │ Validación
│ OK    │ en BD      │ por fecha        │
│       │            │                  │
│ ✓     │ ✗ 401      │ ✗ 401            │ 1. ¿Revocado? → BD
│       │ REVOKED    │ EXPIRED          │ 2. ¿Expirado? → fecha
```

---

## 🔧 Cambiar Tiempo de Expiración

### Opción 1: Hardcoded
```java
// JwtUtils.java línea 52
.withExpiresAt(new Date(System.currentTimeMillis() + 3600000)) // 1 hora
```

### Opción 2: Configurable (Recomendado)
```properties
# application.properties
security.jwt.expiration-ms=3600000  # 1 hora
```

```java
// JwtUtils.java
@Value("${security.jwt.expiration-ms:86400000}")
private long expirationMs;

.withExpiresAt(new Date(System.currentTimeMillis() + expirationMs))
```

---

## ❓ FAQs

### ¿El token se "actualiza" cuando expira?
**NO.** Simplemente deja de ser válido y el cliente debe hacer login para obtener uno nuevo.

### ¿Se guarda el token expirado en BD?
**NO.** Solo se guardan tokens revocados manualmente (logout).

### ¿Qué pasa si hago logout a las 2 horas y luego expira a las 24h?
El token queda en `revoked_tokens` hasta su expiración natural (24h), luego se purga automáticamente.

### ¿Puedo tener diferentes tiempos de expiración por rol?
Sí, modificando `createToken()` para aceptar parámetro `expirationMs` según el rol.

### ¿El servidor "sabe" cuándo expira un token sin validarlo?
NO. El servidor solo sabe cuando intenta validar el token en una petición.

---

## 🎯 Respuesta Final a Tu Pregunta

**"¿Cómo se actualiza cuando un usuario ya se agotó el tiempo de su token?"**

✅ **NO se actualiza nada**
✅ **El token contiene su fecha de expiración desde el inicio**
✅ **En cada petición, el servidor valida: exp > now()?**
✅ **Si exp < now() → 401 Unauthorized**
✅ **Cliente recibe 401 → Limpia token → Hace login nuevamente**
✅ **Nuevo login = Nuevo token con nueva fecha de expiración**

**Es como un boleto de cine:** tiene fecha/hora impresa, no se "actualiza" cuando expira, simplemente ya no sirve.

---

**Sistema:** SIGMAV2
**Tiempo de expiración actual:** 24 horas (86400000 ms)
**Configurable en:** `JwtUtils.java` línea 52

