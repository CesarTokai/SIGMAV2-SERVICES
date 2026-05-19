# Sistema de Revocación de Tokens JWT - SIGMAV2

## Descripción General

Este documento describe la implementación del sistema de revocación de tokens JWT en SIGMAV2. El sistema permite invalidar tokens inmediatamente cuando un usuario cierra sesión o cuando se requiere revocar acceso por razones de seguridad.

---

## ¿Cómo Funciona la Sesión Activa vs Inactiva?

### Sesión Activa
Un token JWT está **activo** cuando:
- El token tiene una firma válida
- No ha expirado (la fecha `exp` es futura)
- **NO está en la lista de tokens revocados** (tabla `revoked_tokens`)
- El usuario asociado está activo en la base de datos

### Sesión Inactiva
Un token JWT se vuelve **inactivo** cuando:
- El token ha expirado naturalmente (tiempo de vida terminado)
- El usuario hace logout (se agrega a `revoked_tokens` inmediatamente)
- Un administrador revoca el token manualmente
- El usuario es desactivado en la base de datos

### ¿Cuándo se Detecta la Inactividad?

**RESPUESTA DIRECTA:** Con la implementación de revocación persistente, la invalidación ocurre **inmediatamente en el servidor** cuando:

1. **Logout explícito**: Al llamar `POST /api/auth/logout`, el token se guarda en la BD como revocado
2. **Siguiente petición**: Cualquier petición posterior con ese token será rechazada por `JwtRevocationFilter` antes de procesar la autenticación
3. **No es necesario que el usuario intente login nuevamente** - el rechazo ocurre en la primera petición después del logout

---

## Arquitectura de la Solución

### Componentes Implementados

```
┌─────────────────────────────────────────────────────────────────┐
│                         Cliente (Frontend)                       │
│                 Almacena JWT en localStorage/cookies             │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                │ HTTP Request + Authorization: Bearer <token>
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Spring Security                          │
│                                                                  │
│  1. JwtRevocationFilter                                         │
│     ├─ Extrae JTI del token                                     │
│     ├─ Consulta revoked_tokens en BD                            │
│     └─ Si revocado → 401 Unauthorized                           │
│                                                                  │
│  2. JwtAuthenticationFilter                                     │
│     ├─ Reutiliza DecodedJWT (evita doble parsing)              │
│     ├─ Verifica usuario activo en BD                            │
│     └─ Establece Authentication en SecurityContext              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Base de Datos MySQL                         │
│                                                                  │
│  Tabla: revoked_tokens                                          │
│  ├─ id (PK)                                                     │
│  ├─ jti (UNIQUE) ← Identificador único del JWT                 │
│  ├─ revoked_at   ← Timestamp de revocación                     │
│  ├─ expires_at   ← Fecha de expiración natural del token       │
│  ├─ reason       ← LOGOUT, SECURITY_BREACH, etc.               │
│  └─ username     ← Usuario asociado (opcional)                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Archivos Creados/Modificados

### 1. Nuevos Archivos Creados

#### Entidad
- `security/infrastructure/entity/RevokedToken.java`
  - Entidad JPA para tokens revocados
  - Campos: id, jti, revokedAt, expiresAt, reason, username
  - Índices en `jti` y `expiresAt` para consultas rápidas

#### Repository
- `security/infrastructure/repository/RevokedTokenRepository.java`
  - `existsByJti(String jti)`: Verifica si un token está revocado
  - `deleteExpired(Instant now)`: Elimina tokens expirados (limpieza)

#### Service
- `security/infrastructure/service/TokenRevocationService.java`
  - `revokeToken()`: Guarda token en lista de revocados
  - `isRevoked()`: Verifica si un JTI está revocado
  - `purgeExpiredTokens()`: Tarea programada (@Scheduled) que limpia tokens expirados cada hora

#### Filter
- `security/infrastructure/filter/JwtRevocationFilter.java`
  - Extiende `OncePerRequestFilter`
  - Se ejecuta **antes** de `JwtAuthenticationFilter`
  - Extrae JTI y consulta si está revocado
  - Si está revocado: devuelve 401 inmediatamente
  - Si es válido: guarda `DecodedJWT` en atributo de request para evitar doble parsing

#### Controller
- `security/infrastructure/controller/LogoutController.java`
  - Endpoint: `POST /api/auth/logout`
  - Extrae token del header `Authorization`
  - Obtiene JTI y fecha de expiración
  - Llama a `TokenRevocationService.revokeToken()`
  - Limpia `SecurityContextHolder`

#### Migración
- `resources/db/migration/V1_0_7__Create_revoked_tokens_table.sql`
  - Crea tabla `revoked_tokens`
  - Define índices para optimizar consultas

### 2. Archivos Modificados

#### `SecurityConfig.java`
- Agregado `TokenRevocationService` como dependencia
- Registrado `JwtRevocationFilter` **antes** de `JwtAuthenticationFilter`
- Agregado `/api/auth/logout` a endpoints permitidos

#### `JwtAuthenticationFilter.java`
- Optimizado para reutilizar `DecodedJWT` del atributo `DECODED_JWT`
- Evita doble parsing del token si ya fue validado por `JwtRevocationFilter`
- Agregado `/api/auth/logout` a `shouldNotFilter()`

#### `Sigmav2Application.java`
- Agregada anotación `@EnableScheduling` para tareas programadas
- Actualizado `@EntityScan` y `@EnableJpaRepositories` para incluir package `security`

#### `application.properties`
- Agregada propiedad `security.revocation.purge-interval-ms=3600000` (1 hora)

---

## Flujo de Operación

### Flujo de Login (sin cambios)
```
1. Usuario → POST /api/sigmav2/auth/login
2. Backend valida credenciales
3. Backend genera JWT con JTI único
4. Frontend recibe token y lo guarda
```

### Flujo de Logout (NUEVO)
```
1. Usuario → POST /api/auth/logout + Authorization: Bearer <token>
2. LogoutController extrae token
3. JwtUtils valida y extrae JTI + expiración
4. TokenRevocationService.revokeToken() guarda en BD:
   - jti: "abc-123-def"
   - revokedAt: 2025-11-04T10:30:00Z
   - expiresAt: 2025-11-05T10:30:00Z
   - reason: "LOGOUT"
   - username: "user@example.com"
5. SecurityContextHolder.clearContext()
6. Responde 200 OK con mensaje "Sesión cerrada exitosamente"
```

### Flujo de Petición con Token Revocado (NUEVO)
```
1. Cliente → GET /api/sigmav2/inventory + Authorization: Bearer <token-revocado>
2. JwtRevocationFilter:
   - Extrae token del header
   - Valida y extrae JTI
   - Consulta: SELECT EXISTS(SELECT 1 FROM revoked_tokens WHERE jti = 'abc-123')
   - Resultado: TRUE (está revocado)
   - Responde: 401 Unauthorized
   {
     "success": false,
     "error": {
       "code": "TOKEN_REVOKED",
       "message": "El token ha sido revocado",
       "details": "Este token ya no es válido. Por favor, inicie sesión nuevamente."
     }
   }
3. NO continúa a JwtAuthenticationFilter
4. NO llega al controlador
```

### Flujo de Petición con Token Válido
```
1. Cliente → GET /api/sigmav2/inventory + Authorization: Bearer <token-válido>
2. JwtRevocationFilter:
   - Extrae y valida token
   - Extrae JTI
   - Consulta BD: no está revocado
   - Guarda DecodedJWT en request.setAttribute("DECODED_JWT", decodedJWT)
   - Continúa cadena de filtros
3. JwtAuthenticationFilter:
   - Reutiliza DecodedJWT del atributo (NO re-parsea)
   - Verifica usuario activo en BD
   - Establece Authentication en SecurityContext
   - Continúa cadena
4. Llega al controlador y procesa petición
```

### Tarea Programada de Purga
```
Cada 1 hora (configurable):
1. TokenRevocationService.purgeExpiredTokens()
2. Ejecuta: DELETE FROM revoked_tokens WHERE expires_at < NOW()
3. Log: "Purgados X tokens expirados de la lista de revocación"
```

---

## Ventajas de Esta Implementación

### 1. Revocación Inmediata
- No es necesario esperar a que el token expire naturalmente
- El servidor rechaza tokens revocados en la primera petición

### 2. Persistencia en Base de Datos
- Funciona en entornos con múltiples instancias/servidores
- No se pierde información al reiniciar la aplicación
- A diferencia de `JwtBlacklistService` (en memoria), esta solución es distribuida

### 3. Sin Duplicación de Lógica
- `JwtRevocationFilter` valida una vez y guarda resultado
- `JwtAuthenticationFilter` reutiliza el `DecodedJWT`
- No hay doble parsing del token

### 4. Optimización
- Índice en `jti` para consultas rápidas O(1)
- Purga automática de registros expirados (ahorra espacio)
- Solo guarda JTI (no el token completo) para eficiencia

### 5. Auditoría
- Registra quién cerró sesión y cuándo
- Campo `reason` permite diferenciar logout normal de revocación por seguridad
- Logs completos en cada operación

---

## Configuración

### Variables en `application.properties`

```properties
# Intervalo de purga de tokens revocados (en milisegundos)
# Por defecto: 3600000 ms = 1 hora
security.revocation.purge-interval-ms=3600000

# JWT existente (sin cambios)
security.jwt.key.private=C4S4RB4CkJND
security.jwt.user.generator=S1GM4V2
```

---

## Uso del API

### Endpoint de Logout

**Request:**
```http
POST /api/auth/logout HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Sesión cerrada exitosamente",
  "data": null,
  "timestamp": "2025-11-04T10:30:00"
}
```

**Response (Token ya revocado):**
```json
{
  "success": false,
  "error": {
    "code": "TOKEN_REVOKED",
    "message": "El token ha sido revocado",
    "details": "Este token ya no es válido. Por favor, inicie sesión nuevamente."
  },
  "timestamp": "2025-11-04T10:30:00"
}
```

---

## Testing

### Pruebas Manuales Recomendadas

1. **Login y Logout Normal**
   ```bash
   # 1. Login
   curl -X POST http://localhost:8080/api/sigmav2/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"user@test.com","password":"password123"}'

   # Guarda el token recibido
   TOKEN="eyJhbGciOiJIUzI1NiIs..."

   # 2. Usa el token (debe funcionar)
   curl http://localhost:8080/api/sigmav2/users/profile \
     -H "Authorization: Bearer $TOKEN"

   # 3. Logout
   curl -X POST http://localhost:8080/api/auth/logout \
     -H "Authorization: Bearer $TOKEN"

   # 4. Intenta usar el mismo token (debe fallar con 401)
   curl http://localhost:8080/api/sigmav2/users/profile \
     -H "Authorization: Bearer $TOKEN"
   ```

2. **Verificar Base de Datos**
   ```sql
   -- Ver tokens revocados
   SELECT * FROM revoked_tokens;

   -- Ver tokens que expiran en las próximas 24 horas
   SELECT * FROM revoked_tokens
   WHERE expires_at < DATE_ADD(NOW(), INTERVAL 24 HOUR);
   ```

3. **Probar Purga Automática**
   - Esperar 1 hora después de insertar un token expirado
   - Verificar que se haya eliminado de la BD
   - Revisar logs: "Purgados X tokens expirados..."

---

## Consideraciones de Producción

### 1. Escalabilidad
- **Actual**: Consulta a MySQL por cada petición con token
- **Optimización**: Agregar Redis como caché para `revoked_tokens`
  ```java
  // Pseudocódigo
  if (redisCache.exists(jti)) return true;
  boolean revoked = repository.existsByJti(jti);
  if (revoked) redisCache.set(jti, "1", TTL);
  return revoked;
  ```

### 2. Performance
- **Índice en `jti`**: Ya implementado
- **Purga frecuente**: Ajustar `purge-interval-ms` según carga
- **Connection pool**: Asegurar suficientes conexiones a BD

### 3. Seguridad
- **JTI único**: Ya implementado con `UUID.randomUUID()` en `JwtUtils`
- **HTTPS obligatorio**: Asegurar tokens no viajen en texto plano
- **Rotation de claves**: Cambiar `security.jwt.key.private` periódicamente

### 4. Monitoreo
- Agregar métricas con Micrometer/Prometheus:
  - Cantidad de tokens revocados por hora
  - Tiempo de respuesta de `existsByJti()`
  - Tokens purgados en cada ejecución

---

## Comparación: Antes vs Después

| Aspecto | Antes (JwtBlacklistService en memoria) | Después (RevokedTokens en BD) |
|---------|----------------------------------------|--------------------------------|
| **Persistencia** | No (se pierde al reiniciar) | Sí (en MySQL) |
| **Multi-instancia** | No (cada servidor tiene su lista) | Sí (compartida entre servidores) |
| **Purga** | Manual en cada consulta | Automática programada |
| **Auditoría** | No (solo token y exp) | Sí (reason, username, timestamps) |
| **Escalabilidad** | Limitada por memoria | Escalable con índices |
| **Consulta** | O(1) en ConcurrentHashMap | O(1) con índice en BD |

---

## Troubleshooting

### Problema: "Cannot resolve table 'revoked_tokens'"
**Causa**: La migración no se ha ejecutado aún
**Solución**: Ejecutar la aplicación para que Flyway cree la tabla automáticamente

### Problema: Token no se revoca después de logout
**Causa**: JwtRevocationFilter no está registrado antes de JwtAuthenticationFilter
**Solución**: Verificar orden en `SecurityConfig.securityFilterChain()`

### Problema: Purga no se ejecuta
**Causa**: `@EnableScheduling` no está presente
**Solución**: Verificar que `Sigmav2Application` tiene la anotación

### Problema: Performance degradado
**Causa**: Muchos tokens revocados sin purgar
**Solución**:
1. Reducir `purge-interval-ms` a 15 minutos (900000)
2. Implementar caché Redis

---

## Próximos Pasos (Mejoras Futuras)

1. **Redis Cache Layer**
   - Agregar dependencia `spring-boot-starter-data-redis`
   - Cachear consultas de `existsByJti()` con TTL

2. **Refresh Tokens**
   - Implementar tokens de corta duración (15 min access + 7 días refresh)
   - Reducir ventana de revocación necesaria

3. **Admin Panel**
   - Endpoint para listar tokens activos por usuario
   - Endpoint para revocar todos los tokens de un usuario
   - Dashboard de sesiones activas

4. **Métricas y Alertas**
   - Integrar Micrometer para métricas
   - Alertar si tasa de revocación es anormalmente alta

---

## Conclusión

✅ **Implementación Completa**: Sistema robusto de revocación de tokens JWT
✅ **Sin Código Duplicado**: Filtros cooperan y reutilizan validaciones
✅ **Respuesta a Pregunta Original**: El estado de revocación se marca **inmediatamente en el servidor** al hacer logout, y se detecta en la **primera petición posterior** sin necesidad de que el usuario intente login nuevamente
✅ **Producción-Ready**: Con índices, purga automática y logging completo

---

**Autor**: Sistema SIGMAV2
**Fecha**: 2025-11-04
**Versión**: 1.0.0

