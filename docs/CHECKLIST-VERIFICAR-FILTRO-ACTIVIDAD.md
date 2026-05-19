# 🔍 CHECKLIST: VERIFICAR QUE EL FILTRO FUNCIONA CORRECTAMENTE

**Objetivo:** Diagnosticar si el filtro de rastreo de actividad está funcionando sin problemas.

---

## ✅ PASO 1: VERIFICAR CONFIGURACIÓN DE SPRING

### 1.1 Confirmar que el filtro está registrado

**Archivo:** `security/config/SecurityConfig.java` (línea 72)

```java
.addFilterAfter(activityFilter, JwtAuthenticationFilter.class)
```

**Verificar:**
- [ ] La línea existe tal cual
- [ ] `activityFilter` está inyectado en método `securityFilterChain()`
- [ ] Está DESPUÉS de `JwtAuthenticationFilter` (orden correcto)
- [ ] Está ANTES de `.build()`

**Si falta:** Agregarlo en la cadena de filtros

---

## ✅ PASO 2: VERIFICAR COMPONENTE SPRING

**Archivo:** `security/infrastructure/filter/UserActivityFilter.java` (línea 29)

```java
@Component  // ← IMPORTANTE
@Slf4j
@RequiredArgsConstructor
public class UserActivityFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;
```

**Verificar:**
- [ ] Tiene anotación `@Component`
- [ ] Tiene anotación `@RequiredArgsConstructor`
- [ ] Inyecta `UserRepository` (no JpaUserRepository)
- [ ] Extiende `OncePerRequestFilter`

**Si falta:** Agregarlo

---

## ✅ PASO 3: VERIFICAR MODELO DE DOMINIO

**Archivo:** `modules/users/domain/model/User.java`

```java
@Getter
@Setter
public class User {
    // ...
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime passwordChangedAt;
```

**Verificar:**
- [ ] Tiene atributo `lastLoginAt` con getter/setter
- [ ] Tiene atributo `lastActivityAt` con getter/setter  
- [ ] Tiene atributo `passwordChangedAt` con getter/setter
- [ ] Son tipo `LocalDateTime` (no String, no Date)

**Si falta:** Agregarlo:
```java
private LocalDateTime lastLoginAt;
private LocalDateTime lastActivityAt;
private LocalDateTime passwordChangedAt;
```

---

## ✅ PASO 4: VERIFICAR ENTIDAD JPA

**Archivo:** `modules/users/model/BeanUser.java`

```java
@Entity
@Table(name = "users")
public class BeanUser {
    // ...
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;
    
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
```

**Verificar:**
- [ ] Tiene anotación `@Column(name = "last_login_at")`
- [ ] Tiene anotación `@Column(name = "last_activity_at")`
- [ ] Tiene anotación `@Column(name = "password_changed_at")`
- [ ] Los nombres en BD coinciden exactamente (lower_snake_case)

**Si falta:** Agregarlo

---

## ✅ PASO 5: VERIFICAR ADAPTADOR DE SEGURIDAD

**Archivo:** `security/infrastructure/adapter/SecurityUserAdapter.java`

```java
public User toDomainUser(BeanUser beanUser) {
    User user = new User(
        // ... 10 parámetros ...
    );
    
    // IMPORTANTE: Estos 3 campos se mapean después del constructor
    user.setLastLoginAt(beanUser.getLastLoginAt());
    user.setLastActivityAt(beanUser.getLastActivityAt());
    user.setPasswordChangedAt(beanUser.getPasswordChangedAt());
    
    return user;
}

public BeanUser toLegacyUser(User domainUser) {
    // ...
    beanUser.setLastLoginAt(domainUser.getLastLoginAt());
    beanUser.setLastActivityAt(domainUser.getLastActivityAt());
    beanUser.setPasswordChangedAt(domainUser.getPasswordChangedAt());
    
    return beanUser;
}
```

**Verificar:**
- [ ] `toDomainUser()` mapea los 3 campos después de constructor
- [ ] `toLegacyUser()` mapea los 3 campos
- [ ] Ningún campo está siendo ignorado o nulificado

**Si falta:** Agregarlo

---

## ✅ PASO 6: VERIFICAR MIGRACIÓN DE BD

**Archivo:** `src/main/resources/db/migration/V1_2_0__Add_user_activity_tracking.sql`

```sql
ALTER TABLE users
ADD COLUMN last_login_at DATETIME NULL,
ADD COLUMN last_activity_at DATETIME NULL,
ADD COLUMN password_changed_at DATETIME NULL;

CREATE INDEX idx_users_last_login ON users(last_login_at);
CREATE INDEX idx_users_last_activity ON users(last_activity_at);
```

**Verificar:**
- [ ] El archivo existe
- [ ] Las 3 columnas están presentes en tabla `users`
- [ ] Son tipo `DATETIME` o `TIMESTAMP`
- [ ] Permiten NULL (nullable)
- [ ] Índices están creados para performance

**En MySQL:**
```sql
DESCRIBE users;  -- Ver estructura
-- Debería mostrar:
-- | last_login_at      | datetime     | YES  |
-- | last_activity_at   | datetime     | YES  |
-- | password_changed_at| datetime     | YES  |
```

**Si falta:** Ejecutar migración manualmente o esperar a que Flyway la aplique

---

## ✅ PASO 7: VERIFICAR PUERTO DE DOMINIO

**Archivo:** `modules/users/domain/port/output/UserRepository.java`

```java
public interface UserRepository {
    Optional<User> findByEmail(String email);
    User save(User user);
    // ... otros métodos ...
}
```

**Verificar:**
- [ ] Tiene método `findByEmail(String email)`
- [ ] Retorna `Optional<User>`
- [ ] Tiene método `save(User user)`
- [ ] NO usa clases JPA directamente

**Si falta:** Agregarlo

---

## ✅ PASO 8: VERIFICAR IMPLEMENTACIÓN DEL PUERTO

**Archivo:** `modules/users/infrastructure/persistence/UserRepositoryAdapter.java`

```java
@Repository
@Primary
public class UserRepositoryAdapter implements UserRepository {
    private final JpaUserRepository jpaRepository;
    private final UserMapper mapper;
    
    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toModel);
    }
    
    @Override
    public User save(User user) {
        BeanUser entity = mapper.toEntity(user);
        return mapper.toModel(jpaRepository.save(entity));
    }
}
```

**Verificar:**
- [ ] Tiene anotación `@Repository`
- [ ] Tiene anotación `@Primary`
- [ ] Implementa interfaz `UserRepository`
- [ ] Usa mapper para conversión entre domain y JPA
- [ ] Inyecta `JpaUserRepository` (Spring Data JPA)

**Si falta:** Agregarlo

---

## ✅ PASO 9: PROBAR CON LOGS

### 9.1 Habilitar DEBUG logging

**Archivo:** `src/main/resources/application.properties`

```properties
# Agregar estas líneas si no existen
logging.level.tokai.com.mx.SIGMAV2.security.infrastructure.filter.UserActivityFilter=DEBUG
logging.level.tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence=DEBUG
```

### 9.2 Hacer un request autenticado

```bash
# Terminal PowerShell
$headers = @{
    "Authorization" = "Bearer YOUR_JWT_TOKEN_HERE"
}

Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/users/me" `
    -Method GET `
    -Headers $headers
```

### 9.3 Ver logs

**Esperado en consola:**
```
🔄 Intentando actualizar actividad para usuario: admin@tokai.com
✅ Usuario encontrado: admin@tokai.com (ID: 1)
⏰ Estableciendo lastActivityAt a: 2026-03-23T14:32:15.123456
💾 Usuario guardado exitosamente: admin@tokai.com
```

**Si ves esto:** ✅ El filtro funciona perfectamente

**Si ves error:** Pasar a PASO 10

---

## ✅ PASO 10: DIAGNOSTICAR ERRORES

### Error 1: "Bean de tipo UserRepository no encontrado"
```
BeanCurrentlyInCreationException: Error creating bean with name 'userActivityFilter'
```

**Solución:**
- [ ] Verificar que `UserRepositoryAdapter` existe
- [ ] Verificar que tiene `@Repository` y `@Primary`
- [ ] Limpiar build: `.\mvnw.cmd clean build`
- [ ] Reiniciar aplicación

### Error 2: "Usuario NO encontrado en BD"
```
⚠️ Usuario NO encontrado en BD: admin@tokai.com
```

**Solución:**
- [ ] El usuario no existe en BD (es normal si es nuevo)
- [ ] Verificar en MySQL: `SELECT * FROM users WHERE email='admin@tokai.com';`
- [ ] Si no existe, crear usuario primero

### Error 3: "Error guardando usuario"
```
❌ Error guardando usuario admin@tokai.com: Connection refused
```

**Solución:**
- [ ] Verificar conexión MySQL
- [ ] Verificar propiedades en `application.properties`
- [ ] Ejecutar: `SELECT 1` en MySQL para verificar conexión

### Error 4: "Mapeo de campos falta"
```
NullPointerException en setLastActivityAt
```

**Solución:**
- [ ] Verificar que `User` tiene setter
- [ ] Verificar que `SecurityUserAdapter` mapea los campos
- [ ] Verificar que migración BD se ejecutó

---

## ✅ PASO 11: VERIFICAR EN BASE DE DATOS

```sql
-- Conectar a MySQL
mysql -u root -p

-- Usar BD
USE SIGMAV2_2;

-- Ver estructura de tabla users
DESCRIBE users;

-- Buscar un usuario autenticado
SELECT id, email, last_login_at, last_activity_at, password_changed_at 
FROM users 
WHERE email = 'admin@tokai.com';

-- Ver cuándo fue la última actividad
SELECT email, last_activity_at, NOW() AS ahora 
FROM users 
WHERE last_activity_at IS NOT NULL 
ORDER BY last_activity_at DESC 
LIMIT 5;
```

**Esperado:**
- `last_activity_at` debe ser reciente (dentro de últimos segundos)
- Debe cambiar con cada request

**Si no cambia:**
- [ ] El filtro NO está ejecutándose
- [ ] Verificar SecurityConfig de nuevo
- [ ] Verificar logs

---

## ✅ PASO 12: CHECKLIST FINAL

| Verificación | Estado | Nota |
|-------------|--------|------|
| SecurityConfig tiene el filtro en cadena | [ ] | Línea 72 |
| UserActivityFilter tiene @Component | [ ] | Necesario para inyección |
| User domain tiene los 3 campos | [ ] | lastLoginAt, lastActivityAt, passwordChangedAt |
| BeanUser entity tiene los 3 campos | [ ] | Con @Column mappings |
| SecurityUserAdapter mapea los 3 campos | [ ] | En toDomainUser y toLegacyUser |
| Migración V1_2_0 está aplicada | [ ] | Ver en BD |
| UserRepository interface existe | [ ] | Puerto de dominio |
| UserRepositoryAdapter implementa puerto | [ ] | @Repository @Primary |
| Logs muestran actualización exitosa | [ ] | DEBUG level |
| BD muestra lastActivityAt actualizado | [ ] | SELECT query |

**Si todos están ✅:** El filtro funciona perfectamente

**Si alguno está ❌:** Completar ese paso

---

## 📞 SI NECESITAS AYUDA

**Proporciona:**
1. Logs completos de la aplicación (application-full-output.txt)
2. Resultado de: `DESCRIBE users;` en MySQL
3. Resultado de: `SELECT * FROM users LIMIT 1;` en MySQL
4. El error exacto que ves
5. Qué cambios hiciste antes del problema

---

**Última actualización:** 2026-03-23


