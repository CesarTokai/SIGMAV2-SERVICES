# Sistema de Reenvío de Códigos de Verificación - SIGMAV2

## Resumen de Funcionalidad

Esta implementación añade un sistema robusto para reenviar códigos de verificación con control de rate limiting, seguimiento de uso y expiración automática.

## Componentes Implementados

### 1. VerificationCodeLog (Entity)
- **Ubicación**: `tokai.com.mx.SIGMAV2.modules.users.domain.model.VerificationCodeLog`
- **Propósito**: Entidad de dominio para rastrear códigos de verificación enviados
- **Campos clave**:
  - `email`: Email del usuario
  - `verificationCode`: Código generado
  - `purpose`: Propósito del código (registro, reenvío, etc.)
  - `expiresAt`: Fecha de expiración
  - `isUsed`: Estado de uso del código
  - `attemptsCount`: Contador de intentos de validación

### 2. VerificationCodeLogRepository (Repository)
- **Ubicación**: `tokai.com.mx.SIGMAV2.modules.users.domain.port.output.VerificationCodeLogRepository`
- **Propósito**: Puerto de salida para operaciones de base de datos
- **Métodos principales**:
  - `findActiveCodesByEmail()`: Códigos activos para un email
  - `countCodesSentInLastHour()`: Rate limiting por hora
  - `countCodesSentToday()`: Rate limiting diario
  - `findByEmailAndCode()`: Validación de código específico

### 3. VerificationCodeService (Service)
- **Ubicación**: `tokai.com.mx.SIGMAV2.modules.users.application.service.VerificationCodeService`
- **Propósito**: Lógica de negocio para manejo avanzado de códigos
- **Características**:
  - **Rate Limiting**: Máximo 5 códigos por hora, 10 por día
  - **Expiración**: Códigos válidos por 24 horas
  - **Seguimiento**: Logs completos de uso y validación
  - **Validación**: Verificación de códigos con auto-marcado como usados

### 4. Endpoint de Reenvío
- **URL**: `POST /api/sigmav2/users/resend-verification-code`
- **DTO**: `ResendVerificationCodeRequest`
- **Validaciones**:
  - Email válido y obligatorio
  - Usuario debe existir
  - Usuario no debe estar verificado
  - Respeta límites de rate limiting

## Flujo de Funcionamiento

### Registro de Usuario
1. Usuario se registra con email/password
2. Sistema genera código de verificación usando `VerificationCodeService`
3. Código se almacena en `VerificationCodeLog` con expiración de 24h
4. Usuario recibe email con código

### Reenvío de Código
1. Usuario solicita reenvío a través del endpoint
2. Sistema valida:
   - Usuario existe y no está verificado
   - No se han excedido límites de rate limiting (5/hora, 10/día)
3. Genera nuevo código y marca códigos anteriores como expirados
4. Envía email con nuevo código
5. Registra el evento en `VerificationCodeLog`

### Validación de Código
1. Usuario envía código para verificación
2. Sistema busca código activo y no expirado
3. Valida el código y lo marca como usado
4. Incrementa contador de intentos
5. Marca usuario como verificado si es exitoso

## Rate Limiting

### Límites Implementados
- **Por hora**: Máximo 5 códigos de verificación
- **Por día**: Máximo 10 códigos de verificación
- **Propósito**: Prevenir abuso del sistema de emails

### Lógica de Control
```java
// Verificar límite por hora
int codesLastHour = repository.countCodesSentInLastHour(email);
if (codesLastHour >= MAX_CODES_PER_HOUR) {
    throw new RateLimitExceededException("Máximo de códigos por hora excedido");
}

// Verificar límite diario
int codesToday = repository.countCodesSentToday(email);
if (codesToday >= MAX_CODES_PER_DAY) {
    throw new RateLimitExceededException("Máximo de códigos diarios excedido");
}
```

## Seguridad

### Medidas Implementadas
1. **Expiración automática**: Códigos válidos solo por 24 horas
2. **Uso único**: Códigos se marcan como usados después de validación exitosa
3. **Rate limiting**: Previene spam de emails
4. **Logging completo**: Auditoría de todos los códigos generados y usados
5. **Validación de entrada**: DTOs con validaciones Jakarta

### Prevención de Ataques
- **Fuerza bruta**: Rate limiting y expiración de códigos
- **Spam de emails**: Límites estrictos por usuario
- **Replay attacks**: Códigos de uso único
- **Timing attacks**: Validación consistente independiente del resultado

## Base de Datos

### Tabla: verification_code_log
```sql
CREATE TABLE verification_code_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    verification_code VARCHAR(10) NOT NULL,
    purpose VARCHAR(100) NOT NULL DEFAULT 'Email verification',
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP NULL,
    attempts_count INT NOT NULL DEFAULT 0,
    -- Índices para optimización
    INDEX idx_email (email),
    INDEX idx_email_code (email, verification_code),
    INDEX idx_expires_at (expires_at)
);
```

### Optimizaciones
- **Índices estratégicos**: Para búsquedas por email, código y expiración
- **Limpieza automática**: Opción para eliminar códigos muy antiguos
- **Particionamiento**: Posible implementación futura por fecha

## Configuración

### Variables de Rate Limiting
```java
// En VerificationCodeService
private static final int MAX_CODES_PER_HOUR = 5;
private static final int MAX_CODES_PER_DAY = 10;
private static final int CODE_VALIDITY_HOURS = 24;
```

### Personalización
Estos valores pueden convertirse en propiedades configurables:
```properties
# application.properties
sigmav2.verification.max-codes-per-hour=5
sigmav2.verification.max-codes-per-day=10
sigmav2.verification.code-validity-hours=24
```

## Testing

### Casos de Prueba Sugeridos
1. **Registro exitoso**: Usuario nuevo recibe código
2. **Reenvío exitoso**: Usuario no verificado puede solicitar nuevo código
3. **Rate limiting**: Superar límites arroja excepción
4. **Expiración**: Códigos expirados no son válidos
5. **Uso único**: Código usado no puede reutilizarse
6. **Usuario verificado**: No puede solicitar nuevos códigos

### Pruebas de Integración
- Verificar integración con servicio de email
- Probar endpoint completo con diferentes escenarios
- Validar comportamiento de base de datos

## Monitoreo y Métricas

### Métricas Recomendadas
- Códigos generados por día/hora
- Tasa de verificación exitosa
- Códigos expirados sin usar
- Intentos de rate limiting
- Tiempo promedio entre envío y verificación

### Logs Importantes
- Generación de códigos (email, propósito)
- Intentos de validación (exitosos y fallidos)
- Violaciones de rate limiting
- Errores de envío de email

## Mantenimiento

### Tareas Periódicas
1. **Limpieza de códigos antiguos**: Eliminar registros > 30 días
2. **Análisis de uso**: Revisar patrones de solicitud
3. **Optimización**: Ajustar rate limits según uso real

### Scripts de Mantenimiento
```sql
-- Limpiar códigos muy antiguos
DELETE FROM verification_code_log 
WHERE expires_at < NOW() - INTERVAL 30 DAY;

-- Estadísticas de uso
SELECT 
    DATE(sent_at) as date,
    COUNT(*) as codes_sent,
    SUM(is_used) as codes_used,
    ROUND(SUM(is_used) / COUNT(*) * 100, 2) as success_rate
FROM verification_code_log 
WHERE sent_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(sent_at)
ORDER BY date DESC;
```

## Posibles Mejoras Futuras

1. **Códigos alfanuméricos**: Mayor entropía en lugar de solo números
2. **Notificaciones SMS**: Alternativa al email
3. **Configuración por usuario**: Límites personalizados por tipo de cuenta
4. **Cache de códigos**: Redis para mejor rendimiento
5. **Análisis de patrones**: Detección de comportamiento sospechoso
6. **API de estadísticas**: Endpoint para métricas de administración

---

**Nota**: Esta implementación proporciona una base sólida y segura para el manejo de códigos de verificación con room para futuras expansiones según las necesidades del negocio.
