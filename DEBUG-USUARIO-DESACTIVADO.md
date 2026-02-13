# 🔍 DEBUGGING - Usuario Desactivado Aún Puede Hacer Login

## 📋 PASOS DE VERIFICACIÓN

### 1. Verifica que el usuario está REALMENTE desactivado en BD

```sql
-- Conectate a la BD y ejecuta:
SELECT id, email, status FROM users WHERE email = 'obotello@tokai.com.mx';

-- Deberías ver: status = false
```

### 2. Verifica que el cambio se reflejó con la API

```bash
curl -X POST http://localhost:8080/api/sigmav2/admin/users/2/toggle-status \
  -H "Authorization: Bearer {ADMIN_TOKEN}" \
  -H "Content-Type: application/json"

# Respuesta esperada:
# {
#   "success": true,
#   "message": "Estado del usuario cambiado",
#   "data": {
#     "id": 2,
#     "email": "obotello@tokai.com.mx",
#     "role": "ALMACENISTA",
#     "status": false  ← Debe ser FALSE
#   }
# }
```

### 3. Intenta hacer login con ese usuario

```bash
curl -X POST http://localhost:8080/api/sigmav2/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "obotello@tokai.com.mx",
    "password": "SuContraseña"
  }'

# ESPERADO: Error 400 o 500 con mensaje "El usuario está desactivado"
# ACTUAL: ¿Qué error recibas?
```

### 4. Revisa los logs de la aplicación

Busca estos logs después de desactivar y luego intentar login:

```
❌ USUARIO DESACTIVADO - Rechazando acceso para: obotello@tokai.com.mx
```

o

```
✅ Usuario activo - Permitiendo acceso
```

Si ves `✅` pero debería ser `❌`, significa que la BD no se actualizó correctamente.

---

## 🛠️ CAMBIOS REALIZADOS

### 1. **UserDetailsServicePer.java** - Validación de Status en Login
Moví la validación `if (!user2.isStatus())` al inicio del login, ANTES de validar la contraseña.

**Ubicación en el archivo:**
```
Línea ~109: if (!user2.isStatus()) throw exception
```

### 2. **JwtAuthenticationFilter.java** - Validación de Status en Cada Request
Se agregaron logs detallados para verificar que se está validando el status en CADA request autenticado.

**Ubicación en el archivo:**
```
Línea ~141-146: if (!beanUser.isStatus()) sendForbiddenResponse(...)
```

---

## 🚀 PRÓXIMOS PASOS

### Si el usuario AÚN puede hacer login después de recompilar:

1. **Limpia la caché del navegador:**
   - DevTools → Application → Clear All
   - O usa una pestaña incógnita

2. **Reinicia completamente la aplicación:**
   - Mata el proceso de Spring Boot
   - Limpia `target/` directory
   - Vuelve a compilar: `mvn clean install`
   - Inicia de nuevo

3. **Si aún falla, verifica:**
   - ¿Se recompilaron los archivos? 
   - ¿La aplicación realmente está usando el código nuevo?
   - ¿El usuario está desactivado en la BD? (Ejecuta el query SQL)

---

## 📊 FLUJO CORRECTO DE DESACTIVACIÓN

```
1. Admin: POST /admin/users/{userId}/toggle-status
   ↓
2. BD: UPDATE users SET status = false WHERE id = {userId}
   ↓
3. Respuesta: {"status": false}
   ↓
4. Usuario intenta login: POST /auth/login
   ↓
5. UserDetailsServicePer.login() 
   → if (!user2.isStatus()) throw "Usuario desactivado" ✅
   ↓
6. RECHAZADO - Error 400 "El usuario está desactivado"

---

Si el usuario hace request con token antiguo:
   ↓
7. JwtAuthenticationFilter.doFilterInternal()
   → Valida status en BD
   → if (!beanUser.isStatus()) sendForbiddenResponse() ✅
   ↓
8. RECHAZADO - Error 403 "El usuario se encuentra inactivo"
```

---

## ✅ CHECKLIST

- [ ] Ejecuté `mvn clean install`
- [ ] Reinicié la aplicación Spring Boot
- [ ] Verifiqué en BD que el usuario tiene `status = false`
- [ ] Intenté hacer login
- [ ] Reviso los logs buscando los mensajes de debug

Si todos los checks están hechos pero aún falla, el problema está en otro lugar que necesitamos identificar.

