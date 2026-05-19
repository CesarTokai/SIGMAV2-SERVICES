# 🧪 PRUEBA: Intentos Fallidos de Cambio de Contraseña

## Endpoints para Probar

### 1️⃣ Obtener código de reset (Step 1)
```bash
POST /api/sigmav2/auth/findUserToResetPassword
Content-Type: application/json

{
  "Email": "obotello@tokai.com.mx"
}
```

**Respuesta esperada:**
```json
{
  "success": true,
  "message": "Usuario encontrado para recuperación de contraseña",
  "data": {
    "email": "obotello@tokai.com.mx",
    "verificationCode": "ABC123",
    "userSavedId": 2,
    "mailSent": true,
    "requestSaved": true,
    "requestId": 5
  }
}
```

### 2️⃣ Intentar validar código INCORRECTO 3 veces (Step 2 - FALLIDO)

**Intento 1 (Código incorrecto):**
```bash
POST /api/sigmav2/auth/compareCodeToResetPassword
Content-Type: application/json

{
  "Email": "obotello@tokai.com.mx",
  "verificationCode": "WRONG123"
}
```

**Respuesta esperada:**
```json
{
  "success": false,
  "message": "Verification code is incorrect",
  "error": "INVALID_CODE"
}
```

**Intento 2 (Código incorrecto):**
```bash
POST /api/sigmav2/auth/compareCodeToResetPassword
Content-Type: application/json

{
  "Email": "obotello@tokai.com.mx",
  "verificationCode": "WRONG456"
}
```

**Intento 3 (Código incorrecto - DEBE BLOQUEAR):**
```bash
POST /api/sigmav2/auth/compareCodeToResetPassword
Content-Type: application/json

{
  "Email": "obotello@tokai.com.mx",
  "verificationCode": "WRONG789"
}
```

**Respuesta esperada:**
```json
{
  "success": false,
  "message": "Demasiados intentos fallidos. Intente de nuevo más tarde",
  "error": "INVALID_CODE"
}
```

---

## 🔍 Verificar Intentos Registrados

### Endpoint de DEBUG (sin restricción):
```bash
POST /api/sigmav2/users/debug/password-reset-attempts-all/by-email
Content-Type: application/json

{
  "email": "obotello@tokai.com.mx"
}
```

**Respuesta esperada después de 3 intentos fallidos:**
```json
{
  "success": true,
  "data": {
    "userId": 2,
    "email": "obotello@tokai.com.mx",
    "totalFailedAttempts": 3,
    "failedAttemptsLast15Minutes": 3,
    "isBlockedForCodeValidation": true,
    "allFailedAttempts": [
      {
        "attemptId": 1,
        "attemptType": "CODE_VALIDATION",
        "successful": false,
        "attemptAt": "2026-02-16T15:30:00",
        "errorMessage": "Código de verificación incorrecto"
      },
      {
        "attemptId": 2,
        "attemptType": "CODE_VALIDATION",
        "successful": false,
        "attemptAt": "2026-02-16T15:30:05",
        "errorMessage": "Código de verificación incorrecto"
      },
      {
        "attemptId": 3,
        "attemptType": "CODE_VALIDATION",
        "successful": false,
        "attemptAt": "2026-02-16T15:30:10",
        "errorMessage": "Código de verificación incorrecto"
      }
    ]
  }
}
```

---

## 📊 Endpoint de Admin (con autorización)
```bash
POST /api/sigmav2/users/admin/password-reset-attempts/by-email
Content-Type: application/json
Authorization: Bearer <ADMIN_TOKEN>

{
  "email": "obotello@tokai.com.mx"
}
```

**Respuesta esperada:**
```json
{
  "success": true,
  "data": {
    "userId": 2,
    "email": "obotello@tokai.com.mx",
    "failedCodeAttemptsLast15Minutes": 3,
    "isBlockedForCodeValidation": true,
    "recentFailedAttempts": [
      {
        "attemptId": 3,
        "attemptType": "CODE_VALIDATION",
        "attemptAt": "2026-02-16T15:30:10",
        "errorMessage": "Código de verificación incorrecto"
      },
      ...
    ]
  }
}
```

---

## 🔐 Endpoint de Historial Completo de Seguridad
```bash
POST /api/sigmav2/users/admin/security-history/by-email
Content-Type: application/json
Authorization: Bearer <ADMIN_TOKEN>

{
  "email": "obotello@tokai.com.mx"
}
```

**Respuesta esperada:**
```json
{
  "success": true,
  "data": {
    "userId": 2,
    "email": "obotello@tokai.com.mx",
    "isVerified": true,
    "status": true,
    "failedLoginAttempts": 0,
    "lastFailedLoginAttempt": null,
    "lastPasswordChange": "2026-02-10T14:30:00",
    "pendingPasswordChangeRequests": 0,
    "completedPasswordChangeRequests": 1,
    "rejectedPasswordChangeRequests": 0
  }
}
```

---

## ⏰ Comportamiento Esperado

### Línea de Tiempo:
1. **T+0min** → Solicita código (Step 1)
2. **T+1min** → Intento fallido #1 (registro creado)
3. **T+2min** → Intento fallido #2 (registro creado)
4. **T+3min** → Intento fallido #3 (registro creado) → **BLOQUEADO por 15 minutos**
5. **T+4min** → Intento fallido #4 → `"Demasiados intentos fallidos"`
6. **T+18min** → Bloqueo expira → Puede intentar de nuevo

### Logs esperados:
```
⚠️ Intento fallido de código para usuario: obotello@tokai.com.mx (intento 1 en 15 min)
⚠️ Intento fallido de código para usuario: obotello@tokai.com.mx (intento 2 en 15 min)
⚠️ Intento fallido de código para usuario: obotello@tokai.com.mx (intento 3 en 15 min)
❌ BLOQUEADO: Usuario obotello@tokai.com.mx ha excedido 3 intentos fallidos de código en los últimos 15 minutos
```

---

## 📝 Notas Importantes

- Los intentos se limpian automáticamente después de 15 minutos
- Solo se cuentan intentos fallidos de **validación de código** (CODE_VALIDATION)
- No afecta los intentos de **login** (esos están en tabla `users.attempts`)
- El bloqueo es temporal (15 minutos), no permanente
- Los intentos exitosos también se registran para auditoría

