# Diagramas de Flujo - Sistema de Revocación de Tokens

## Flujo Completo: Login → Uso → Logout → Intento de Acceso

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          1. FLUJO DE LOGIN                                  │
└─────────────────────────────────────────────────────────────────────────────┘

Cliente                    Backend                      Base de Datos
  │                           │                              │
  │  POST /auth/login         │                              │
  │  {email, password}        │                              │
  ├──────────────────────────>│                              │
  │                           │                              │
  │                           │  Valida credenciales         │
  │                           │──────────────────────────────>│
  │                           │                              │
  │                           │  Usuario válido              │
  │                           │<──────────────────────────────│
  │                           │                              │
  │                           │  Genera JWT:                 │
  │                           │  - sub: user@example.com     │
  │                           │  - jti: uuid-123-abc         │
  │                           │  - exp: +24h                 │
  │                           │  - authorities: ROLE_USER    │
  │                           │                              │
  │  200 OK                   │                              │
  │  {token: "eyJhbGc..."}    │                              │
  │<──────────────────────────│                              │
  │                           │                              │
  │  Guarda en localStorage   │                              │
  │                           │                              │


┌─────────────────────────────────────────────────────────────────────────────┐
│                     2. FLUJO DE USO NORMAL (Token Válido)                   │
└─────────────────────────────────────────────────────────────────────────────┘

Cliente                    JwtRevocationFilter         JwtAuthenticationFilter    BD
  │                              │                            │                   │
  │  GET /api/inventory          │                            │                   │
  │  Authorization: Bearer token │                            │                   │
  ├─────────────────────────────>│                            │                   │
  │                              │                            │                   │
  │                              │  Extrae JTI del token      │                   │
  │                              │  jti = "uuid-123-abc"      │                   │
  │                              │                            │                   │
  │                              │  ¿Token revocado?          │                   │
  │                              │────────────────────────────────────────────────>│
  │                              │  SELECT jti FROM           │                   │
  │                              │  revoked_tokens            │                   │
  │                              │  WHERE jti='uuid-123-abc'  │                   │
  │                              │                            │                   │
  │                              │  No encontrado (válido)    │                   │
  │                              │<────────────────────────────────────────────────│
  │                              │                            │                   │
  │                              │  Guarda DecodedJWT         │                   │
  │                              │  en request attribute      │                   │
  │                              │                            │                   │
  │                              │  Continúa cadena de filtros│                   │
  │                              ├───────────────────────────>│                   │
  │                              │                            │                   │
  │                              │                            │  Reutiliza        │
  │                              │                            │  DecodedJWT       │
  │                              │                            │  (no re-parsea)   │
  │                              │                            │                   │
  │                              │                            │  Establece        │
  │                              │                            │  Authentication   │
  │                              │                            │                   │
  │                              │                            │  Continúa         │
  │                              │                            │  al controlador   │
  │                              │                            │                   │
  │  200 OK                      │                            │                   │
  │  {data: [...]}               │                            │                   │
  │<─────────────────────────────┴────────────────────────────┴───────────────────│
  │                                                                                │


┌─────────────────────────────────────────────────────────────────────────────┐
│                           3. FLUJO DE LOGOUT                                 │
└─────────────────────────────────────────────────────────────────────────────┘

Cliente                   LogoutController          TokenRevocationService      BD
  │                              │                            │                  │
  │  POST /api/auth/logout       │                            │                  │
  │  Authorization: Bearer token │                            │                  │
  ├─────────────────────────────>│                            │                  │
  │                              │                            │                  │
  │                              │  Extrae token del header   │                  │
  │                              │  Valida y extrae:          │                  │
  │                              │  - jti: "uuid-123-abc"     │                  │
  │                              │  - exp: 2025-11-05T10:00   │                  │
  │                              │  - username: user@test.com │                  │
  │                              │                            │                  │
  │                              │  revokeToken()             │                  │
  │                              ├───────────────────────────>│                  │
  │                              │                            │                  │
  │                              │                            │  INSERT INTO     │
  │                              │                            │  revoked_tokens  │
  │                              │                            │  VALUES(         │
  │                              │                            │   jti=uuid-123   │
  │                              │                            │   revoked_at=NOW │
  │                              │                            │   expires_at=exp │
  │                              │                            │   reason=LOGOUT  │
  │                              │                            │  )               │
  │                              │                            ├─────────────────>│
  │                              │                            │                  │
  │                              │                            │  1 row affected  │
  │                              │                            │<─────────────────│
  │                              │                            │                  │
  │                              │  Token revocado OK         │                  │
  │                              │<───────────────────────────│                  │
  │                              │                            │                  │
  │                              │  SecurityContextHolder     │                  │
  │                              │  .clearContext()           │                  │
  │                              │                            │                  │
  │  200 OK                      │                            │                  │
  │  {success: true,             │                            │                  │
  │   message: "Sesión cerrada"} │                            │                  │
  │<─────────────────────────────│                            │                  │
  │                              │                            │                  │
  │  localStorage.removeItem()   │                            │                  │
  │  Redirige a /login           │                            │                  │
  │                              │                            │                  │


┌─────────────────────────────────────────────────────────────────────────────┐
│              4. FLUJO CON TOKEN REVOCADO (Después de Logout)                │
└─────────────────────────────────────────────────────────────────────────────┘

Cliente                  JwtRevocationFilter          BD                 Response
  │                              │                     │                     │
  │  GET /api/inventory          │                     │                     │
  │  Authorization: Bearer token │                     │                     │
  │  (mismo token de antes)      │                     │                     │
  ├─────────────────────────────>│                     │                     │
  │                              │                     │                     │
  │                              │  Extrae JTI         │                     │
  │                              │  jti = "uuid-123"   │                     │
  │                              │                     │                     │
  │                              │  ¿Token revocado?   │                     │
  │                              │─────────────────────>│                     │
  │                              │  EXISTS(jti)        │                     │
  │                              │                     │                     │
  │                              │  ✓ ENCONTRADO       │                     │
  │                              │  (está revocado)    │                     │
  │                              │<─────────────────────│                     │
  │                              │                     │                     │
  │                              │  RECHAZAR PETICIÓN  │                     │
  │                              │  Status: 401        │                     │
  │                              │  Code: TOKEN_REVOKED│                     │
  │                              │─────────────────────────────────────────>│
  │                              │                     │                     │
  │  401 Unauthorized            │                     │                     │
  │  {                           │                     │                     │
  │    "success": false,         │                     │                     │
  │    "error": {                │                     │                     │
  │      "code": "TOKEN_REVOKED",│                     │                     │
  │      "message": "Token       │                     │                     │
  │         revocado",           │                     │                     │
  │      "details": "Inicie      │                     │                     │
  │         sesión nuevamente"   │                     │                     │
  │    }                         │                     │                     │
  │  }                           │                     │                     │
  │<─────────────────────────────┴─────────────────────┴─────────────────────│
  │                                                                           │
  │  Interceptor detecta 401                                                  │
  │  localStorage.clear()                                                     │
  │  Redirige a /login                                                        │
  │                                                                           │

  ❌ NO llega a JwtAuthenticationFilter
  ❌ NO llega al controlador
  ❌ Rechazado inmediatamente


┌─────────────────────────────────────────────────────────────────────────────┐
│                      5. TAREA PROGRAMADA DE PURGA                           │
└─────────────────────────────────────────────────────────────────────────────┘

Scheduler                TokenRevocationService         Base de Datos
  │                              │                            │
  │  Cada 1 hora                 │                            │
  │  (configurable)              │                            │
  │─────────────────────────────>│                            │
  │                              │                            │
  │                              │  purgeExpiredTokens()      │
  │                              │                            │
  │                              │  DELETE FROM               │
  │                              │  revoked_tokens            │
  │                              │  WHERE expires_at < NOW()  │
  │                              ├───────────────────────────>│
  │                              │                            │
  │                              │  3 rows deleted            │
  │                              │<───────────────────────────│
  │                              │                            │
  │                              │  Log: "Purgados 3 tokens   │
  │                              │   expirados"               │
  │                              │                            │


┌─────────────────────────────────────────────────────────────────────────────┐
│                  6. COMPARACIÓN: ANTES vs DESPUÉS                           │
└─────────────────────────────────────────────────────────────────────────────┘

╔═══════════════════════════════════════════════════════════════════════════╗
║                              ANTES                                         ║
║                     (Solo JwtBlacklistService)                            ║
╚═══════════════════════════════════════════════════════════════════════════╝

  Logout → Guarda en ConcurrentHashMap (memoria) → Reinicio servidor →
  Lista se pierde → Token vuelve a ser válido ❌

  Servidor A: tiene token en blacklist
  Servidor B: NO tiene token en blacklist → Token funciona ❌

╔═══════════════════════════════════════════════════════════════════════════╗
║                             DESPUÉS                                        ║
║                   (RevokedTokens + Base de Datos)                         ║
╚═══════════════════════════════════════════════════════════════════════════╝

  Logout → Guarda en tabla revoked_tokens → Reinicio servidor →
  Consulta BD → Token sigue revocado ✓

  Servidor A: consulta BD → token revocado
  Servidor B: consulta BD → token revocado ✓

  Compartido entre todos los servidores ✓


┌─────────────────────────────────────────────────────────────────────────────┐
│                    7. ORDEN DE EJECUCIÓN DE FILTROS                         │
└─────────────────────────────────────────────────────────────────────────────┘

Request con Token
     │
     ▼
┌─────────────────────────────────────────┐
│   1. JwtRevocationFilter                │
│   ────────────────────────              │
│   ✓ Extrae JTI                          │
│   ✓ Consulta revoked_tokens             │
│   ✓ Si revocado → 401 (STOP)            │
│   ✓ Si válido → guarda DecodedJWT       │
│   ✓ Continúa cadena                     │
└─────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│   2. JwtAuthenticationFilter            │
│   ────────────────────────              │
│   ✓ Reutiliza DecodedJWT (no re-parsea) │
│   ✓ Extrae username y roles             │
│   ✓ Verifica usuario activo en BD       │
│   ✓ Establece Authentication            │
│   ✓ Continúa cadena                     │
└─────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────┐
│   3. Controlador                        │
│   ────────────────────────              │
│   ✓ Procesa la petición                 │
│   ✓ Devuelve respuesta                  │
└─────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────────┐
│                    8. ESTRUCTURA DE BASE DE DATOS                           │
└─────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                    Tabla: revoked_tokens                        │
├────────────────────────────────────────────────────────────────┤
│  id (PK)       │ BIGINT AUTO_INCREMENT                         │
│  jti (UNIQUE)  │ VARCHAR(512) NOT NULL  ← Índice único        │
│  revoked_at    │ TIMESTAMP NOT NULL                            │
│  expires_at    │ TIMESTAMP NOT NULL     ← Índice para purga   │
│  reason        │ VARCHAR(100)                                  │
│  username      │ VARCHAR(255)                                  │
└────────────────────────────────────────────────────────────────┘

Ejemplo de registro:
┌────┬──────────────┬─────────────────────┬─────────────────────┬────────┬──────────────┐
│ id │     jti      │    revoked_at       │    expires_at       │ reason │   username   │
├────┼──────────────┼─────────────────────┼─────────────────────┼────────┼──────────────┤
│ 1  │ uuid-123-abc │ 2025-11-04 10:30:00 │ 2025-11-05 10:30:00 │ LOGOUT │ user@test.com│
│ 2  │ uuid-456-def │ 2025-11-04 11:00:00 │ 2025-11-05 11:00:00 │ LOGOUT │ admin@test.  │
└────┴──────────────┴─────────────────────┴─────────────────────┴────────┴──────────────┘


┌─────────────────────────────────────────────────────────────────────────────┐
│                          9. CÓDIGOS DE ERROR                                │
└─────────────────────────────────────────────────────────────────────────────┘

TOKEN_REVOKED
├─ Status: 401 Unauthorized
├─ Causa: Token fue revocado (logout, admin, seguridad)
├─ Acción: Limpiar localStorage y redirigir a login
└─ Mensaje: "El token ha sido revocado"

TOKEN_EXPIRED
├─ Status: 401 Unauthorized
├─ Causa: Token expiró naturalmente (>24h)
├─ Acción: Limpiar localStorage y redirigir a login
└─ Mensaje: "El token ha expirado"

TOKEN_INVALID
├─ Status: 401 Unauthorized
├─ Causa: Firma inválida, claims incorrectos, malformado
├─ Acción: Limpiar localStorage y redirigir a login
└─ Mensaje: "Token inválido"

ACCESS_DENIED
├─ Status: 403 Forbidden
├─ Causa: Usuario desactivado o sin permisos
├─ Acción: Mostrar mensaje y redirigir
└─ Mensaje: "El usuario se encuentra inactivo"


┌─────────────────────────────────────────────────────────────────────────────┐
│                    10. TIMELINE DE VIDA DE UN TOKEN                         │
└─────────────────────────────────────────────────────────────────────────────┘

T=0h          T=1h          T=23h         T=24h         T=25h
│             │             │             │             │
│ Login       │ Uso normal  │ Logout      │ Expiración  │ Purga
│ genera JWT  │ sin issues  │ manual      │ natural     │ automática
│             │             │             │             │
├─────────────┼─────────────┼─────────────┼─────────────┼──────>
│             │             │             │             │
│ Token       │ Token       │ Token       │ Token       │ Registro
│ VÁLIDO      │ VÁLIDO      │ REVOCADO    │ EXPIRADO    │ eliminado
│             │             │ (en BD)     │ (por fecha) │ de BD
│             │             │             │             │
│ ✓ 200 OK    │ ✓ 200 OK    │ ✗ 401       │ ✗ 401       │
│             │             │ REVOKED     │ EXPIRED     │


┌─────────────────────────────────────────────────────────────────────────────┐
│                      RESPUESTA A LA PREGUNTA ORIGINAL                       │
└─────────────────────────────────────────────────────────────────────────────┘

  Pregunta: "¿Se cambia el estado o se queda así hasta que vuelva a
             iniciar sesión?"

  Respuesta:

  ✅ SÍ SE CAMBIA EL ESTADO INMEDIATAMENTE en el servidor:

     Logout → INSERT en tabla revoked_tokens → Estado cambiado

  ✅ LA DETECCIÓN OCURRE EN LA PRIMERA PETICIÓN:

     Request → JwtRevocationFilter → Consulta BD → Encuentra revocado → 401

  ✅ NO ES NECESARIO QUE EL USUARIO INTENTE LOGIN:

     Cualquier petición con token revocado es rechazada inmediatamente

  ✅ EL TOKEN NO "SE QUEDA ASÍ":

     El servidor activamente marca como inválido y rechaza en cada intento


═══════════════════════════════════════════════════════════════════════════════
                               FIN DEL DIAGRAMA
═══════════════════════════════════════════════════════════════════════════════

