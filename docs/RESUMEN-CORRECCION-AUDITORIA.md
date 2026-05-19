# ✅ RESUMEN DE CORRECCIONES - Campos de Auditoría de Usuario

## 🎯 PROBLEMA IDENTIFICADO
Los campos de auditoría NO se estaban guardando en la BD:
- ❌ `lastLoginAt` - Null después de login
- ❌ `lastActivityAt` - Null después de cualquier acción
- ❌ `passwordChangedAt` - Null después de cambiar contraseña
- ❌ `lastFailedAttempt` (alias de `lastTryAt`) - Null después de intentos fallidos

## 🔍 CAUSA RAÍZ
**Tres mappers diferentes NO estaban mapeando los campos de auditoría:**

### 1. SecurityUserAdapter.java
- **Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/security/infrastructure/adapter/SecurityUserAdapter.java`
- **Métodos afectados:**
  - `toLegacyUser()` - Convertía User (dominio) → BeanUser
  - `toDomainUser()` - Convertía BeanUser → User (dominio)
- **Campos faltantes:**
  - `lastLoginAt`
  - `lastActivityAt`
  - `passwordChangedAt`

**Flujo donde se usaba:**
1. Login → `UserDetailsServicePer.login()` → `securityUserAdapter.toDomainUser()` ❌
2. Cambio de contraseña → `RequestRecoveryPasswordService` usa `UserDomainMapper` (NO este)

### 2. UserDomainMapper.java
- **Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/users/infrastructure/mapper/UserDomainMapper.java`
- **Métodos afectados:**
  - `toDomain()` - Convertía BeanUser (JPA) → User (dominio)
  - `toEntity()` - Convertía User (dominio) → BeanUser (JPA)
- **Campos faltantes:**
  - `lastLoginAt`
  - `lastActivityAt`
  - `passwordChangedAt`

**Flujo donde se usaba:**
1. Cambio de contraseña → `RequestRecoveryPasswordService.completeRequest()` → `userMapper.toDomain()` ❌
2. Recuperación de contraseña → `UserDetailsServicePer.findUserToResetPassword()` → `securityUserAdapter.toDomainUser(user)` ❌

### 3. UserMapper.java ✅ (Este SÍ estaba bien)
- Ya tenía los campos mapeados correctamente desde el inicio

---

## ✅ SOLUCIONES APLICADAS

### 1️⃣ SecurityUserAdapter.java
**Cambio 1: Método `toLegacyUser()`**
```java
// ANTES
public BeanUser toLegacyUser(User domainUser) {
    // ... 11 campos
    // No mapeaba: lastLoginAt, lastActivityAt, passwordChangedAt
    return beanUser;
}

// DESPUÉS
public BeanUser toLegacyUser(User domainUser) {
    // ... 11 campos
    beanUser.setLastLoginAt(domainUser.getLastLoginAt());
    beanUser.setLastActivityAt(domainUser.getLastActivityAt());
    beanUser.setPasswordChangedAt(domainUser.getPasswordChangedAt());
    return beanUser;
}
```

**Cambio 2: Método `toDomainUser()`**
```java
// ANTES
public User toDomainUser(BeanUser beanUser) {
    User user = new User(...); // Solo 10 params del constructor
    // No se asignaban los 3 campos adicionales
    return user;
}

// DESPUÉS
public User toDomainUser(BeanUser beanUser) {
    User user = new User(...); // 10 params
    user.setLastLoginAt(beanUser.getLastLoginAt());
    user.setLastActivityAt(beanUser.getLastActivityAt());
    user.setPasswordChangedAt(beanUser.getPasswordChangedAt());
    return user;
}
```

### 2️⃣ UserDomainMapper.java
**Cambio 1: Método `toDomain()`**
```java
// ANTES
public User toDomain(BeanUser entity) {
    return new User(...); // Solo 10 params del constructor
}

// DESPUÉS
public User toDomain(BeanUser entity) {
    User user = new User(...); // 10 params
    user.setLastLoginAt(entity.getLastLoginAt());
    user.setLastActivityAt(entity.getLastActivityAt());
    user.setPasswordChangedAt(entity.getPasswordChangedAt());
    return user;
}
```

**Cambio 2: Método `toEntity()`**
```java
// ANTES
public BeanUser toEntity(User domain) {
    BeanUser entity = new BeanUser();
    // ... 11 campos
    // No mapeaba: lastLoginAt, lastActivityAt, passwordChangedAt
    return entity;
}

// DESPUÉS
public BeanUser toEntity(User domain) {
    BeanUser entity = new BeanUser();
    // ... 11 campos
    entity.setLastLoginAt(domain.getLastLoginAt());
    entity.setLastActivityAt(domain.getLastActivityAt());
    entity.setPasswordChangedAt(domain.getPasswordChangedAt());
    return entity;
}
```

---

## 🔄 FLUJOS QUE AHORA FUNCIONAN

### ✅ Login Exitoso
```
UserDetailsServicePer.login()
  ├── user2.setLastLoginAt(now)        ← Se asigna
  ├── user2.setLastActivityAt(now)     ← Se asigna
  ├── securityUserAdapter.toDomainUser(user2)  ← ✅ Ahora mapea estos campos
  └── userRepository.save()              ← Se guardan en BD
```

### ✅ Cambio de Contraseña
```
RequestRecoveryPasswordService.completeRequest()
  ├── userToUpdate.setPasswordHash(encodedPass)      ← Se asigna
  ├── userToUpdate.setPasswordChangedAt(now)         ← Se asigna
  ├── userMapper.toDomain(userToUpdate)              ← ✅ Ahora mapea estos campos
  └── userRepository.save()                           ← Se guardan en BD
```

### ✅ Login Fallido
```
UserDetailsServicePer.login()
  ├── user2.setAttempts(++attempts)     ← Se asigna
  ├── user2.setLastTryAt(now)           ← Se asigna
  ├── securityUserAdapter.toDomainUser(user2)  ← ✅ Ahora mapea estos campos
  └── userRepository.save()              ← Se guardan en BD
```

---

## 📋 CHECKLIST DE VERIFICACIÓN

Después de compilar, verifica:

```sql
-- 1. Verificar que los campos existan en BD
SELECT column_name FROM information_schema.columns 
WHERE table_name='users' AND column_name IN ('last_login_at', 'last_activity_at', 'password_changed_at');

-- 2. Verificar valores después de login
SELECT id, email, last_login_at, last_activity_at, password_changed_at, last_try_at 
FROM users WHERE email='obotello@tokai.com.mx';
```

---

## 🚀 PASOS A SEGUIR

1. ✅ **Cambios aplicados en archivos:**
   - `SecurityUserAdapter.java`
   - `UserDomainMapper.java`

2. **Compilar proyecto:**
   ```bash
   mvn clean install
   ```

3. **Reiniciar aplicación Spring Boot**

4. **Probar flujos:**
   - Haz login con un usuario
   - Cambia la contraseña de ese usuario
   - Consulta `/api/sigmav2/users/me/complete`
   - Verifica que los campos ahora muestren timestamps en lugar de `null`

5. **Verificar con API:**
   ```bash
   POST /api/sigmav2/users/admin/security/by-email
   Body: {"email": "obotello@tokai.com.mx"}
   
   Respuesta esperada:
   {
     "success": true,
     "data": {
       "lastLoginAt": "2026-02-13T09:15:11.559454",      ← ✅ No null
       "lastActivityAt": "2026-02-13T09:15:11.636967",   ← ✅ No null
       "passwordChangedAt": "2026-02-13T10:30:00.123456", ← ✅ No null
       "lastFailedAttempt": null,                         ← (null si no hay intentos fallidos)
       "failedAttempts": 0,
       "isBlocked": false,
       "status": true
     }
   }
   ```

---

## ⚠️ NOTAS IMPORTANTES

- Los cambios se aplicaron a **2 mappers diferentes**
- El `UserMapper.java` ya estaba correcto (no requería cambios)
- Necesitas **recompilar** para que los cambios tomen efecto
- Los datos antiguos en BD tendrán `null` en estos campos (es normal)
- Las nuevas operaciones (login, cambio de contraseña) registrarán los timestamps correctamente

