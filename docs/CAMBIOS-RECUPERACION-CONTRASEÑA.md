# Corrección: Solo Administradores Pueden Consultar Recuperación de Contraseña

## 📋 Resumen del Problema

Cuando un usuario AUXILIAR solicitaba recuperación de contraseña:
1. ✅ El registro se creaba correctamente en `request_recovery_password`
2. ❌ NO aparecía en el endpoint `/api/sigmav2/auth/getPage`
3. ❌ Otros roles no debían poder verla tampoco

**Causa**: La lógica anterior permitía que múltiples roles vieran solicitudes según una jerarquía incorrecta.

---

## 🔧 Cambios Implementados

### 1. **RequestRecoveryPasswordService.java**

#### Método: `findRequest(Pageable pageable)`
**Antes:**
```java
// Jerarquía compleja que permitía a ALMACENISTA y AUXILIAR ver solicitudes
switch(role.toUpperCase()) {
    case "ADMINISTRADOR":
        return requestRecoveryPasswordRepository.getRequestByRoles(
                List.of(ERole.ALMACENISTA, ERole.AUXILIAR, ERole.AUXILIAR_DE_CONTEO), ...);
    case "ALMACENISTA":
        return requestRecoveryPasswordRepository.getRequestByRoles(
                List.of(ERole.AUXILIAR, ERole.AUXILIAR_DE_CONTEO), ...);
    // ... más casos
}
```

**Después:**
```java
// Solo ADMINISTRADOR puede consultar
if(!role.toUpperCase().equals("ADMINISTRADOR")) {
    throw new UnauthorizedAccessException(
            "Solo los administradores pueden consultar solicitudes de recuperación de contraseña");
}

// ADMINISTRADOR ve TODAS las solicitudes sin filtro de rol
return requestRecoveryPasswordRepository.findByStatus(BeanRequestStatus.PENDING, pageable);
```

#### Método: `completeRequest(RequestToResolveRequestDTO payload)`
**Cambio:**
- ✅ Agregada validación: Solo ADMINISTRADOR puede completar
- ✅ Removida lógica de validación de rol innecesaria

#### Método: `rejectRequest(RequestToResolveRequestDTO payload)`
**Cambio:**
- ✅ Agregada validación: Solo ADMINISTRADOR puede rechazar
- ✅ Simplificada la lógica de validación

#### Método: `getRequestHistory(Pageable pageable)`
**Cambio:**
- ✅ Solo ADMINISTRADOR puede ver historial
- ✅ Ve historial de TODOS los usuarios sin filtro de rol
- ✅ Usa nuevo método `findByStatuses()`

---

### 2. **IRequestRecoveryPassword.java** (Repositorio)

#### Método: `findByStatus()`
```java
@Query("SELECT new tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.ResponsePageRequestRecoveryDTO(" +
        "r.requestId, r.status, r.date, r.user.email, r.user.email, r.user.role) " +
        "FROM BeanRequestRecoveryPassword r " +
        "WHERE r.status = :status " +
        "ORDER BY r.date ASC")
Page<ResponsePageRequestRecoveryDTO> findByStatus(
        @Param("status") BeanRequestStatus status,
        Pageable pageable);
```
- **Propósito:** Obtener TODAS las solicitudes con un estado específico (sin filtrar por rol)
- **Usado por:** `findRequest()` para que ADMINISTRADOR vea todas las solicitudes pendientes

#### Método: `findByStatuses()`
```java
@Query("SELECT new tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.ResponsePageRequestRecoveryDTO(" +
        "r.requestId, r.status, r.date, r.user.email, r.user.email, r.user.role) " +
        "FROM BeanRequestRecoveryPassword r " +
        "WHERE r.status IN :statuses " +
        "ORDER BY r.date ASC")
Page<ResponsePageRequestRecoveryDTO> findByStatuses(
        @Param("statuses") java.util.List<BeanRequestStatus> statuses,
        Pageable pageable);
```
- **Propósito:** Obtener TODAS las solicitudes con múltiples estados (sin filtrar por rol)
- **Usado por:** `getRequestHistory()` para que ADMINISTRADOR vea todo el historial

---

## 🔐 Control de Acceso Actualizado

| Operación | ADMINISTRADOR | ALMACENISTA | AUXILIAR | AUXILIAR_DE_CONTEO |
|-----------|:-------------:|:-----------:|:--------:|:------------------:|
| Ver solicitudes pendientes | ✅ TODAS | ❌ Rechazado | ❌ Rechazado | ❌ Rechazado |
| Completar solicitud | ✅ | ❌ Rechazado | ❌ Rechazado | ❌ Rechazado |
| Rechazar solicitud | ✅ | ❌ Rechazado | ❌ Rechazado | ❌ Rechazado |
| Ver historial | ✅ TODAS | ❌ Rechazado | ❌ Rechazado | ❌ Rechazado |
| Solicitar cambio propio | ✅ | ✅ | ✅ | ✅ |

---

## 📝 Flujo de Uso Correcto

### Caso: Auxiliar solicita cambio de contraseña

1. **AUXILIAR realiza la solicitud** (cualquier usuario puede hacerlo):
   ```
   POST /api/sigmav2/auth/requestRecoveryPassword?email=jtorres@tokai.com.mx
   ```
   ✅ Resultado: Solicitud creada en `request_recovery_password` con estado PENDING

2. **ADMINISTRADOR consulta solicitudes pendientes**:
   ```
   GET /api/sigmav2/auth/getPage
   Authorization: Bearer <admin_token>
   ```
   ✅ Resultado: Ver la solicitud de jtorres junto con las demás

3. **ALMACENISTA intenta consultar** (debe fallar):
   ```
   GET /api/sigmav2/auth/getPage
   Authorization: Bearer <almacenista_token>
   ```
   ❌ Resultado: 
   ```json
   {
     "error": "UnauthorizedAccessException",
     "message": "Solo los administradores pueden consultar solicitudes de recuperación de contraseña"
   }
   ```

4. **ADMINISTRADOR completa la solicitud**:
   ```
   POST /api/sigmav2/auth/completeRequest
   Body: { "requestId": 3 }
   Authorization: Bearer <admin_token>
   ```
   ✅ Resultado: Se genera nueva contraseña y se envía por correo

5. **ADMINISTRADOR consulta historial**:
   ```
   GET /api/sigmav2/auth/getRequestHistory
   Authorization: Bearer <admin_token>
   ```
   ✅ Resultado: Ver solicitudes ACEPTADAS y RECHAZADAS de todos los usuarios

---

## 🧪 Testing

### Con Postman o cURL

**Test 1: ADMINISTRADOR ve solicitudes**
```bash
curl -X GET "http://localhost:8080/api/sigmav2/auth/getPage?page=0&size=20" \
  -H "Authorization: Bearer <token_admin>" \
  -H "Content-Type: application/json"

# Expected: Status 200 con lista de solicitudes
```

**Test 2: ALMACENISTA intenta ver solicitudes**
```bash
curl -X GET "http://localhost:8080/api/sigmav2/auth/getPage?page=0&size=20" \
  -H "Authorization: Bearer <token_almacenista>" \
  -H "Content-Type: application/json"

# Expected: Status 403 o 401 con mensaje de rechazo
```

**Test 3: AUXILIAR intenta ver solicitudes**
```bash
curl -X GET "http://localhost:8080/api/sigmav2/auth/getPage?page=0&size=20" \
  -H "Authorization: Bearer <token_auxiliar>" \
  -H "Content-Type: application/json"

# Expected: Status 403 o 401 con mensaje de rechazo
```

---

## 📊 Datos de Ejemplo

### Base de datos antes/después

**Tabla: user**
```
ID  | email                    | role
--- | ------------------------ | ------------------
1   | cgonzalez@tokai.com.mx   | ADMINISTRADOR
2   | obotello@tokai.com.mx    | ALMACENISTA
3   | jtorres@tokai.com.mx     | AUXILIAR
4   | dcardoso@tokai.com.mx    | AUXILIAR_DE_CONTEO
```

**Tabla: request_recovery_password**
```
ID  | date       | status  | user_id
--- | ---------- | ------- | --------
4   | 2026-02-09 | PENDING | 3
5   | 2026-02-09 | PENDING | 2
```

**Antes (Problema):**
- ADMINISTRADOR: ✅ Ve solicitudes 4 y 5
- ALMACENISTA: ✅ Ve solicitud 4 (¡INCORRECTO!)
- AUXILIAR: ✅ Ve nada (porque busca AUXILIAR_DE_CONTEO)

**Después (Corrección):**
- ADMINISTRADOR: ✅ Ve solicitudes 4 y 5
- ALMACENISTA: ❌ Acceso denegado
- AUXILIAR: ❌ Acceso denegado

---

## 🔍 Logs Esperados

Cuando un ALMACENISTA intenta consultar:
```
2026-02-09 12:00:00 WARN  RequestRecoveryPasswordService - Rol ALMACENISTA sin permisos para ver solicitudes de recuperación
2026-02-09 12:00:00 ERROR RequestRecoveryPasswordService - Acceso no autorizado: Solo los administradores pueden consultar solicitudes de recuperación de contraseña
```

Cuando un ADMINISTRADOR consulta (éxito):
```
2026-02-09 12:00:00 INFO  RequestRecoveryPasswordService - Buscando solicitudes de recuperación para usuario: cgonzalez@tokai.com.mx con rol: ADMINISTRADOR
2026-02-09 12:00:00 DEBUG RequestRecoveryPasswordService - Se encontraron 2 solicitudes pendientes
```

---

## ✅ Validación de la Corrección

- [x] Solo ADMINISTRADOR puede ver solicitudes pendientes
- [x] Solo ADMINISTRADOR puede completar solicitudes
- [x] Solo ADMINISTRADOR puede rechazar solicitudes
- [x] Solo ADMINISTRADOR puede ver historial
- [x] Otros roles reciben `UnauthorizedAccessException`
- [x] Solicitud del AUXILIAR ahora es visible para ADMINISTRADOR
- [x] Los logs registran intentos no autorizados
- [x] La lógica es consistente en todos los métodos

---

## 📚 Archivos Modificados

1. ✅ `RequestRecoveryPasswordService.java` - Lógica de control de acceso
2. ✅ `IRequestRecoveryPassword.java` - Nuevos métodos de consulta sin filtro de rol

**Archivos NO modificados** (siguen funcionando igual):
- `RequestRecoveryPasswordController.java` - Las rutas siguen siendo las mismas
- `BeanRequestRecoveryPassword.java` - Entidad sin cambios
- `BeanRequestStatus.java` - Estados sin cambios

