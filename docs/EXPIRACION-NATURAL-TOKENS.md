# ⏰ Manejo de Expiración Natural de Tokens JWT

## 🎯 Tu Pregunta

**"¿Cómo se actualiza cuando un usuario ya se agotó el tiempo de su token?"**

---

## ✅ Respuesta Directa

### NO se actualiza nada en la base de datos

**Cuando un token expira naturalmente (después de 24 horas):**

1. ✅ **El token contiene su propia fecha de expiración** (`exp` claim)
2. ✅ **La validación se hace en tiempo real** al intentar usarlo
3. ✅ **NO se guarda en `revoked_tokens`** (no es necesario)
4. ✅ **El servidor solo verifica**: `¿exp < ahora?` → Token expirado

---

## 🔄 Flujo Completo de Expiración Natural

### Paso 1: Creación del Token (Login)
```java
// En JwtUtils.createToken()
.withExpiresAt(new Date(System.currentTimeMillis() + 86400000)) // +24 horas
```

El token contiene internamente:
```json
{
  "sub": "user@example.com",
  "jti": "uuid-123-abc",
  "iat": 1730736000,  // Fecha creación: 2025-11-04 10:00:00
  "exp": 1730822400,  // Fecha expiración: 2025-11-05 10:00:00 (+24h)
  "authorities": "ROLE_USER"
}
```

### Paso 2: Token Válido (dentro de 24 horas)
```
Cliente envía token → JwtRevocationFilter verifica revocación → NO revocado →
JwtAuthenticationFilter valida firma y fecha → exp > now() → ✓ VÁLIDO
```

### Paso 3: Token Expirado (después de 24 horas)
```
T=0h                           T=24h                          T=24h+1min
│                              │                              │
│ Token creado                 │ Token expira                 │ Cliente intenta usar
│ exp=2025-11-05 10:00         │ (en el servidor              │ (hace petición)
│                              │  NO pasa nada)               │
│                              │                              │
└──────────────────────────────┴──────────────────────────────┴───────────>
                                     ↑                              ↑
                            Fecha de expiración          Momento de validación
                            (solo es una fecha
                             dentro del token)

Cliente → Request → JwtRevocationFilter → JwtAuthenticationFilter →
JwtUtils.validateToken() → Verifica exp < now() →
¡EXPIRADO! → Lanza TokenExpiredException →
Filtro captura → 401 Unauthorized
```

---

## 📊 Diferencia Clave: Revocación vs Expiración

### 🔴 Revocación (Logout Manual)
```
Usuario hace logout → INSERT INTO revoked_tokens →
Siguiente petición → Consulta BD → Token encontrado en lista → 401
```
**Razón**: El usuario cerró sesión ANTES de que expire naturalmente

### 🟠 Expiración Natural (24 horas)
```
Tiempo pasa → Token llega a fecha exp →
Siguiente petición → Validación de fecha exp → exp < now() → 401
```
**Razón**: El token ya no es válido por tiempo transcurrido

---

## 💡 ¿Por Qué NO Guardar Tokens Expirados en BD?

### Ventajas de NO guardarlos:
✅ **Eficiencia**: No llenamos la BD con millones de tokens expirados
✅ **Sin consultas extra**: La validación de `exp` es instantánea (está en el token)
✅ **Auto-gestionado**: El token se invalida solo, sin intervención del servidor
✅ **Stateless**: Mantenemos la naturaleza stateless de JWT

### Comparación:

| Aspecto | Revocación en BD | Expiración Natural |
|---------|------------------|-------------------|
| **Almacenamiento** | Sí (tabla revoked_tokens) | No (fecha en el token) |
| **Consulta BD** | Sí (por cada request) | No (validación local) |
| **Uso** | Logout, seguridad, admin | Tiempo transcurrido |
| **Limpieza** | Purga programada | Automática (no hay nada que limpiar) |

---

## 🔍 Código Actual - Cómo Funciona

### 1. JwtRevocationFilter (Primer Filtro)
```java
// Verifica si el token fue REVOCADO MANUALMENTE
if (jti != null && revocationService.isRevoked(jti)) {
    // Token en lista negra → 401
    return;
}
// No revocado → Continúa
```

### 2. JwtAuthenticationFilter (Segundo Filtro)
```java
// Valida el token (incluye verificación de expiración)
DecodedJWT decodedJWT = jwtUtils.validateToken(token);
// Si llegamos aquí → token NO expirado y NO revocado
```

### 3. JwtUtils.validateToken()
```java
// Internamente, la librería auth0 verifica:
return JWT.require(algorithm)
    .withIssuer(userGenerator)
    .build()
    .verify(token); // ← Aquí se verifica exp < now()

// Si exp < now() → Lanza TokenExpiredException
```

---

## 🎬 Ejemplo Práctico Completo

### Escenario 1: Token Válido (3 horas después de login)
```
Login: 2025-11-04 10:00:00
Expira: 2025-11-05 10:00:00 (en 24h)
Ahora: 2025-11-04 13:00:00 (3h después)

Request → JwtRevocationFilter:
  ¿Revocado? → Consulta BD → NO

Request → JwtAuthenticationFilter:
  JwtUtils.validateToken() → Verifica exp:
  exp (2025-11-05 10:00) > now (2025-11-04 13:00) ✓
  Token VÁLIDO → Continúa

Response: 200 OK
```

### Escenario 2: Token Expirado (25 horas después de login)
```
Login: 2025-11-04 10:00:00
Expira: 2025-11-05 10:00:00
Ahora: 2025-11-05 11:00:00 (25h después, 1h pasado exp)

Request → JwtRevocationFilter:
  ¿Revocado? → Consulta BD → NO
  Continúa...

Request → JwtAuthenticationFilter:
  JwtUtils.validateToken() → Verifica exp:
  exp (2025-11-05 10:00) < now (2025-11-05 11:00) ✗
  ¡EXPIRADO!
  Lanza: TokenExpiredException

Filter captura excepción → sendErrorResponse():
  Status: 401 Unauthorized
  Body: {
    "success": false,
    "error": {
      "code": "TOKEN_EXPIRED",
      "message": "El token ha expirado",
      "expiredAt": "2025-11-05T10:00:00"
    }
  }
```

### Escenario 3: Token Revocado (logout a las 2 horas)
```
Login: 2025-11-04 10:00:00
Logout: 2025-11-04 12:00:00 ← INSERT INTO revoked_tokens
Expira: 2025-11-05 10:00:00 (aún falta 22h)
Ahora: 2025-11-04 13:00:00 (1h después del logout)

Request → JwtRevocationFilter:
  ¿Revocado? → Consulta BD → SÍ (está en tabla)
  RECHAZAR INMEDIATAMENTE
  Status: 401 Unauthorized
  Body: {
    "success": false,
    "error": {
      "code": "TOKEN_REVOKED",
      "message": "El token ha sido revocado"
    }
  }

❌ NO llega a JwtAuthenticationFilter
❌ NO se valida exp (ya fue rechazado)
```

---

## 🔄 Timeline Visual

```
T=0h          T=2h          T=24h         T=25h
│             │             │             │
│ Login       │ Logout      │ Expiración  │ Intento de uso
│ Crea token  │ manual      │ natural     │
│             │             │             │
├─────────────┼─────────────┼─────────────┼────────>
│             │             │             │
│ Token       │ Token       │ Token       │ Cliente hace
│ VÁLIDO      │ REVOCADO    │ EXPIRADO    │ request
│             │ (en BD)     │ (por fecha) │
│             │             │             │
│ ✓ Funciona  │ ✗ 401       │ ✗ 401       │ Validación:
│             │ REVOKED     │ EXPIRED     │ 1. ¿Revocado? SÍ → 401
│             │             │             │ 2. ¿Expirado? SÍ → 401
│             │             │             │
│             └─────────────┴─────────────┴────────>
│                Almacenado en BD      Verificación local
│                hasta que expire      (fecha en token)
```

---

## 🎯 Resumen de Tu Pregunta

### Pregunta:
> "¿Cómo se actualiza cuando un usuario ya se agotó el tiempo de su token?"

### Respuesta:

**NO se actualiza nada.** El sistema funciona así:

1. **Token contiene `exp`** (fecha de expiración) desde su creación
2. **Servidor verifica `exp`** cada vez que recibe el token
3. **Si `exp < ahora()`** → Token expirado → 401 Unauthorized
4. **Cliente recibe 401** → Limpia localStorage → Redirige a login
5. **Usuario hace nuevo login** → Obtiene nuevo token con nueva `exp`

### Flujo Completo:

```
Token expira (24h) → Cliente intenta usarlo →
Servidor valida exp < now() → 401 TOKEN_EXPIRED →
Cliente detecta 401 → localStorage.clear() →
Redirige a /login → Usuario inicia sesión →
Nuevo token (nueva exp +24h)
```

---

## 🆚 Comparación Final: Los 3 Estados del Token

### 1️⃣ Token Válido
- ✅ `exp > now()`
- ✅ NO está en `revoked_tokens`
- ✅ Usuario activo en BD
- **Resultado**: 200 OK

### 2️⃣ Token Revocado (Logout Manual)
- ⚠️ `exp > now()` (aún no expira naturalmente)
- ❌ **SÍ está en `revoked_tokens`** ← Consultamos BD
- **Resultado**: 401 TOKEN_REVOKED

### 3️⃣ Token Expirado (Natural)
- ❌ **`exp < now()`** ← Validación local
- ⚠️ Puede o no estar en `revoked_tokens`
- **Resultado**: 401 TOKEN_EXPIRED

---

## 🔧 Configuración de Tiempo de Expiración

Para cambiar el tiempo de vida del token:

```java
// JwtUtils.java línea 52
.withExpiresAt(new Date(System.currentTimeMillis() + 86400000)) // 24 horas

// Valores comunes:
// 3600000    = 1 hora
// 86400000   = 24 horas (actual)
// 604800000  = 7 días
// 2592000000 = 30 días
```

O desde `application.properties`:
```properties
# Agregar esta propiedad
security.jwt.expiration-ms=86400000

# Y usarla en JwtUtils:
@Value("${security.jwt.expiration-ms:86400000}")
private long expirationMs;

.withExpiresAt(new Date(System.currentTimeMillis() + expirationMs))
```

---

## 📝 Conclusión

**Tu token NO se "actualiza" cuando expira.**

✅ Simplemente **deja de ser válido** al llegar a la fecha `exp`
✅ El servidor **detecta la expiración** al validar el token
✅ El cliente **recibe 401** y debe hacer login nuevamente
✅ El nuevo login **genera un token completamente nuevo** con nueva `exp`

**Analogía**: Es como un boleto de autobús con fecha de vencimiento impresa. No se "actualiza" cuando expira, simplemente ya no sirve y necesitas comprar uno nuevo.

---

**Fecha**: 2025-11-04
**Sistema**: SIGMAV2
**Duración token**: 24 horas (configurable)

