# ⚠️ POTENCIALES CAUSAS POR LAS QUE "ROMPE LA LÓGICA"

**Objetivo:** Explicar qué específicamente podría hacer que el filtro "rompa" la lógica existente y cómo evitarlo.

---

## 🔴 CAUSA #1: Filtro en Posición Incorrecta

### ❌ Problema

```java
// INCORRECTO - Filtro ANTES de autenticación
.addFilterBefore(activityFilter, JwtAuthenticationFilter.class)
```

**Qué sucede:**
- El filtro intenta acceder a `SecurityContextHolder` antes de que JWT autentique
- `authentication` siempre es `null`
- El usuario nunca se actualiza
- Posible: ❌ Exception si el código no maneja null

### ✅ Solución

```java
// CORRECTO - Filtro DESPUÉS de autenticación
.addFilterAfter(activityFilter, JwtAuthenticationFilter.class)
```

**Por qué funciona:**
- JWT ya validó y estableció Authentication en SecurityContextHolder
- `authentication` es nunca null
- El usuario se actualiza correctamente

---

## 🔴 CAUSA #2: Mapper No Mapea los 3 Campos

### ❌ Problema

```java
// INCORRECTO - SecurityUserAdapter.java
public User toDomainUser(BeanUser beanUser) {
    User user = new User(
        beanUser.getId(),
        beanUser.getEmail(),
        // ... 8 parámetros más ...
    );
    // ❌ Falta mapear los 3 campos
    return user;
}
```

**Qué sucede:**
- Campos `lastActivityAt`, `lastLoginAt`, `passwordChangedAt` quedan como `null` en el dominio
- Al guardar, se sobreescriben con `null` en BD
- Datos históricos se pierden
- ❌ Auditoría no funciona

### ✅ Solución

```java
public User toDomainUser(BeanUser beanUser) {
    User user = new User(
        beanUser.getId(),
        beanUser.getEmail(),
        // ... 8 parámetros ...
    );
    
    // ✅ Mapear SIEMPRE los 3 campos
    user.setLastLoginAt(beanUser.getLastLoginAt());
    user.setLastActivityAt(beanUser.getLastActivityAt());
    user.setPasswordChangedAt(beanUser.getPasswordChangedAt());
    
    return user;
}
```

---

## 🔴 CAUSA #3: Migración BD No Aplicada

### ❌ Problema

```sql
-- Base de datos SIN las columnas
DESCRIBE users;
-- Resultado:
-- id, email, password_hash, role, status, verified, ...
-- ❌ NO está: last_activity_at, last_login_at, password_changed_at
```

**Qué sucede:**
- Filtro intenta actualizar campo que NO existe en BD
- SQL Error: `Column 'last_activity_at' doesn't exist`
- Hibernate/JPA crashea o lanza exception
- ❌ Request se bloquea si no hay try-catch

### ✅ Verificar

```bash
# PowerShell - Verificar migración
.\mvnw.cmd clean install
# Esto ejecuta Flyway automáticamente

# En MySQL
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'users' 
AND COLUMN_NAME IN ('last_login_at', 'last_activity_at', 'password_changed_at');

# Debe retornar 3 filas
```

---

## 🔴 CAUSA #4: UserRepository Incorrecto

### ❌ Problema

```java
// INCORRECTO - Inyectar JpaUserRepository directamente
@Component
public class UserActivityFilter extends OncePerRequestFilter {
    private final JpaUserRepository jpaRepository; // ❌ MALO
    
    private void updateUserActivity(String email) {
        var user = jpaRepository.findByEmail(email); // ❌ Retorna Optional<BeanUser>
        user.ifPresent(beanUser -> {
            beanUser.setLastActivityAt(LocalDateTime.now());
            jpaRepository.save(beanUser); // ❌ Guarda BeanUser directamente
        });
    }
}
```

**Problemas:**
1. ❌ Viola arquitectura hexagonal (usa entidad JPA en infraestructura de seguridad)
2. ❌ No usa puerto de dominio
3. ❌ Difícil de testear
4. ❌ Acoplamiento innecesario
5. ❌ Cambios en BD rompen el filtro

### ✅ Solución

```java
// CORRECTO - Usar puerto de dominio
@Component
@RequiredArgsConstructor
public class UserActivityFilter extends OncePerRequestFilter {
    private final UserRepository userRepository; // ✅ Puerto de dominio
    
    private void updateUserActivity(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastActivityAt(LocalDateTime.now());
            userRepository.save(user); // ✅ Usa User (dominio)
        });
    }
}
```

---

## 🔴 CAUSA #5: Campos No Inicializados en User Constructor

### ❌ Problema

```java
// INCORRECTO - Constructor NO inicializa los 3 campos
public class User {
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime passwordChangedAt;
    
    public User(Long id, String email, String passwordHash, Role role, 
                boolean status, boolean verified, int attempts, 
                LocalDateTime lastTryAt, String verificationCode, 
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        // ❌ NO setea los 3 campos
        this.id = id;
        this.email = email;
        // ...
        // Falta: lastLoginAt, lastActivityAt, passwordChangedAt quedan null
    }
}
```

**Qué sucede:**
- Al crear nuevo User, los 3 campos son `null`
- Aunque BeanUser los tenga en BD, se pierden al mapear
- Auditoría no funciona

### ✅ Solución

```java
public class User {
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime passwordChangedAt;
    
    public User(Long id, String email, /* ... 8 más ... */) {
        this.id = id;
        this.email = email;
        // ...
        // Los 3 campos se setean después del constructor
        // en SecurityUserAdapter.toDomainUser()
    }
    
    // ✅ Setters disponibles
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    // etc.
}
```

---

## 🔴 CAUSA #6: SecurityConfig Inyección Incorrecta

### ❌ Problema

```java
// INCORRECTO - Filtro NO se inyecta correctamente
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        // ❌ UserActivityFilter NO está inyectado
        var activityFilter = new UserActivityFilter(); // ❌ Instanciación manual
        
        return httpSecurity
            .addFilterAfter(activityFilter, JwtAuthenticationFilter.class)
            .build();
    }
}
```

**Problemas:**
1. ❌ `UserActivityFilter` no está en contexto Spring
2. ❌ `UserRepository` no se inyecta
3. ❌ NullPointerException al intentar usar repository
4. ❌ El filtro se ejecuta pero crashea

### ✅ Solución

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity, 
            UserActivityFilter activityFilter) throws Exception { // ✅ Inyectado aquí
        
        return httpSecurity
            .addFilterAfter(activityFilter, JwtAuthenticationFilter.class)
            .build();
    }
}
```

**Y en el filtro:**
```java
@Component // ✅ Es @Component
@RequiredArgsConstructor
public class UserActivityFilter extends OncePerRequestFilter {
    private final UserRepository userRepository; // ✅ Se inyecta automáticamente
}
```

---

## 🔴 CAUSA #7: Exception No Manejada Bloquea Request

### ❌ Problema

```java
// INCORRECTO - Exception relanzada
@Override
protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
    
    String email = extractEmail(authentication);
    userRepository.findByEmail(email).ifPresent(user -> {
        user.setLastActivityAt(LocalDateTime.now());
        userRepository.save(user);
        // ❌ Si save() falla, exception se propaga
    });
    
    filterChain.doFilter(request, response); // ❌ Nunca se ejecuta si hubo error
}
```

**Qué sucede:**
- Si BD está lenta o caída → Exception en updateUserActivity
- Exception NO se captura
- Request se bloquea y nunca llega al controlador
- Cliente recibe 500 Internal Server Error
- ❌ Todo el sistema se ralentiza/caída

### ✅ Solución

```java
@Override
protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
    
    try {
        String email = extractEmail(authentication);
        updateUserActivity(email); // ✅ Llamada en bloque try
    } catch (Exception e) {
        log.warn("⚠️ Error al actualizar actividad: {}", e.getMessage());
        // ✅ Exception capturada, NO relanzada
    }
    
    filterChain.doFilter(request, response); // ✅ SIEMPRE se ejecuta
}

private void updateUserActivity(String email) {
    try {
        userRepository.findByEmail(email).ifPresent(user -> {
            try {
                user.setLastActivityAt(LocalDateTime.now());
                userRepository.save(user);
            } catch (Exception e) {
                log.debug("❌ Error guardando: {}", e.getMessage());
            }
        });
    } catch (Exception e) {
        log.debug("❌ Error en actualización: {}", e.getMessage());
    }
}
```

---

## 🔴 CAUSA #8: Transacciones Perdidas

### ❌ Problema

```java
// INCORRECTO - Sin @Transactional en repository
public class UserRepositoryAdapter implements UserRepository {
    
    public User save(User user) {
        BeanUser entity = mapper.toEntity(user);
        return mapper.toModel(jpaRepository.save(entity));
        // ❌ Sin @Transactional - cambios no se confirman si otra exception después
    }
}
```

**Qué sucede:**
- Si hay error DESPUÉS de save() → rollback implícito
- Datos se pierden
- Auditoría no registra nada

### ✅ Solución

```java
@Repository
public class UserRepositoryAdapter implements UserRepository {
    
    @Transactional // ✅ Asegura ACID
    public User save(User user) {
        BeanUser entity = mapper.toEntity(user);
        return mapper.toModel(jpaRepository.save(entity));
        // ✅ Cambios se confirman automáticamente
    }
}
```

---

## 🔴 CAUSA #9: Thread Safety Issues

### ❌ Problema (Si usaras @Async sin cuidado)

```java
// INCORRECTO - Race condition
@Async
private CompletableFuture<Void> updateUserActivityAsync(String email) {
    var user = userRepository.findByEmail(email);
    user.ifPresent(u -> {
        // T0: Thread A lee lastActivityAt = 14:30
        u.setLastActivityAt(LocalDateTime.now()); // T1: Thread A set = 14:31
        // T2: Thread B lee lastActivityAt = 14:30 (no ve cambio de A)
        userRepository.save(u); // T3: Thread A guarda
        // T4: Thread B guarda, SOBREESCRIBE con 14:30 ❌
    });
}
```

**Qué sucede:**
- Múltiples threads actualizando simultáneamente
- Últimas escrituras ganan (Lost Update)
- Auditoría es incorrecta

### ✅ Solución

```java
// CORRECTO - Usa implementación SÍNCRONA actual
// NO uses @Async para este caso
private void updateUserActivity(String email) {
    try {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastActivityAt(LocalDateTime.now());
            userRepository.save(user); // ✅ Síncrono - no hay race conditions
        });
    } catch (Exception e) {
        log.debug("Error: {}", e.getMessage());
    }
}
```

---

## 📊 TABLA RESUMEN

| Causa | Síntoma | Solución |
|-------|---------|----------|
| #1: Filtro en posición incorrecta | `lastActivityAt` siempre null | `.addFilterAfter()` |
| #2: Mapper no mapea | Datos se pierden después de mapear | Agregar 3 `setXxx()` en adapter |
| #3: Migración no aplicada | SQL Error: `Column not found` | Ejecutar Flyway o migración manual |
| #4: UserRepository incorrecto | NullPointerException en filtro | Usar puerto de dominio |
| #5: Constructor incompleto | Campos null en User | Mapear fuera del constructor |
| #6: Inyección incorrecta | NullPointerException en repository | Inyectar en SecurityConfig |
| #7: Exception no manejada | Request bloqueado | try-catch en filtro |
| #8: Transacciones perdidas | Datos no persisten | Agregar @Transactional |
| #9: Thread safety | Race conditions (remoto) | Mantener síncrono |

---

## ✅ RESUMEN: CÓMO EVITAR "ROMPER LA LÓGICA"

### Regla #1: No Toques el Filtro Sin Razón
✅ La implementación actual funciona  
❌ No intentes hacer async sin profundo conocimiento

### Regla #2: Respeta la Arquitectura Hexagonal
✅ Usa puertos (UserRepository)  
❌ No uses entidades JPA en seguridad

### Regla #3: Manejo de Excepciones Siempre
✅ try-catch en cada nivel  
❌ Nunca relances sin necesidad

### Regla #4: Verifica el Orden de Filtros
✅ Filtro DESPUÉS de autenticación  
❌ No lo pongas ANTES

### Regla #5: Asegura que BD está actualizada
✅ Ejecuta migraciones Flyway  
❌ No modifiques schema manualmente

---

**Conclusión:** La implementación actual está correctamente diseñada para NO romper la lógica. Si experimentaste problemas, probablemente fue por una de estas 9 causas.

**Última actualización:** 2026-03-23


