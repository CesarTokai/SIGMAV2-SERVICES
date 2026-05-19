# Solución: Personal Information Mismatch

## Problema Identificado

- **Usuario autenticado**: `tgonzalez@tokai.com.mx` tiene `user_id = 7` en la tabla `users`
- **Información personal**: existe en `personal_information` con `user_id = 37` (NO con user_id = 7)
- **Resultado**: La API devuelve `personalInformation: null` porque no hay coincidencia entre el user_id del usuario autenticado (7) y el user_id en personal_information (37)

## Consultas SQL de Verificación

### 1. Verificar qué emails tienen los user_id 7 y 37

```sql
SELECT user_id, email, role, is_verified, status
FROM users
WHERE user_id IN (7, 37);
```

### 2. Verificar información personal para ambos usuarios

```sql
SELECT
    pi.personal_information_id,
    pi.user_id,
    pi.name,
    pi.first_last_name,
    pi.second_last_name,
    pi.phone_number,
    u.email
FROM personal_information pi
LEFT JOIN users u ON pi.user_id = u.user_id
WHERE pi.user_id IN (7, 37);
```

### 3. Ver la relación completa usuario ↔ información personal

```sql
SELECT
    u.user_id,
    u.email,
    u.role,
    p.personal_information_id,
    p.name,
    p.first_last_name,
    p.phone_number
FROM users u
LEFT JOIN personal_information p ON p.user_id = u.user_id
WHERE u.user_id IN (7, 37) OR u.email LIKE '%tgonzalez%';
```

## Soluciones Disponibles

### OPCIÓN 1: Reasignar información personal existente (RECOMENDADA si es el mismo usuario)

Si la información personal con `personal_information_id = 7` debe pertenecer al `user_id = 7`:

```sql
-- Paso 1: Verificar que NO exista ya una fila para user_id = 7
SELECT * FROM personal_information WHERE user_id = 7;

-- Paso 2: Si NO existe, reasignar la fila
BEGIN;

UPDATE personal_information
SET user_id = 7,
    updated_at = NOW()
WHERE personal_information_id = 7
  AND user_id = 37;

-- Paso 3: Verificar el cambio
SELECT * FROM personal_information WHERE user_id = 7;

COMMIT;
```

**⚠️ IMPORTANTE**: Si ya existe una fila para `user_id = 7`, el UPDATE fallará por la constraint única `uk_personal_information_user`. En ese caso, deberás decidir:
- Eliminar la fila existente para user_id = 7 antes del UPDATE, o
- Fusionar los datos, o
- Eliminar la fila de user_id = 37 y actualizar la de user_id = 7

### OPCIÓN 2: Crear nueva información personal para user_id = 7

Si quieres crear una nueva entrada para el usuario 7 (y mantener la de 37 intacta):

```sql
-- Verificar primero que NO exista
SELECT * FROM personal_information WHERE user_id = 7;

-- Crear nueva fila para user_id = 7
INSERT INTO personal_information (
    user_id,
    name,
    first_last_name,
    second_last_name,
    phone_number,
    created_at,
    updated_at
)
VALUES (
    7,
    'Cesar Uriel',
    'Gonzalez Saldaña',
    'asdasd',
    '7341218621',
    NOW(),
    NOW()
);

-- Verificar la inserción
SELECT * FROM personal_information WHERE user_id = 7;
```

### OPCIÓN 3: Copiar datos de user_id 37 a user_id 7

Si quieres duplicar la información:

```sql
-- Copiar la información del usuario 37 al usuario 7
INSERT INTO personal_information (
    user_id,
    name,
    first_last_name,
    second_last_name,
    phone_number,
    image,
    comments,
    created_at,
    updated_at
)
SELECT
    7 AS user_id,
    name,
    first_last_name,
    second_last_name,
    phone_number,
    image,
    comments,
    NOW() AS created_at,
    NOW() AS updated_at
FROM personal_information
WHERE user_id = 37;

-- Verificar
SELECT * FROM personal_information WHERE user_id = 7;
```

### OPCIÓN 4: Eliminar información de user_id 37 y crear para user_id 7

```sql
BEGIN;

-- Guardar datos temporalmente
SELECT * FROM personal_information WHERE user_id = 37;

-- Eliminar fila antigua
DELETE FROM personal_information WHERE user_id = 37;

-- Crear nueva para user_id 7
INSERT INTO personal_information (
    user_id,
    name,
    first_last_name,
    second_last_name,
    phone_number,
    created_at,
    updated_at
)
VALUES (
    7,
    'Cesar Uriel',
    'Gonzalez Saldaña',
    'asdasd',
    '7341218621',
    NOW(),
    NOW()
);

COMMIT;
```

## Verificación desde la API

### Después de ejecutar el SQL, verifica con la API:

#### 1. Endpoint autenticado (como el usuario)
```http
GET /api/sigmav2/users/me/complete
Authorization: Bearer <TU_TOKEN>
```

Debería devolver:
```json
{
  "success": true,
  "data": {
    "userId": 7,
    "email": "tgonzalez@tokai.com.mx",
    "personalInformation": {
      "personalInformationId": 7,
      "name": "Cesar Uriel",
      "firstName": "Gonzalez Saldaña",
      ...
    }
  },
  "hasPersonalInformation": true
}
```

#### 2. Endpoint de administrador (por ID)
```http
GET /api/sigmav2/users/complete/id/7
Authorization: Bearer <ADMIN_TOKEN>
```

#### 3. Endpoint de administrador (por email)
```http
GET /api/sigmav2/users/complete/email/tgonzalez@tokai.com.mx
Authorization: Bearer <ADMIN_TOKEN>
```

## Logs Mejorados

He añadido logs adicionales en `UserCompleteController` que ahora mostrarán:

```
Usuario encontrado: email=tgonzalez@tokai.com.mx, userId=7
Información personal encontrada: personalInfoId=7, userId=7, name=Cesar Uriel
```

O si no hay información:
```
Usuario encontrado: email=tgonzalez@tokai.com.mx, userId=7
NO se encontró información personal para userId=7
```

## Problema Potencial Identificado en el Código

⚠️ **Advertencia**: El proyecto tiene **dos clases JPA diferentes** mapeando la tabla `users`:

1. `tokai.com.mx.SIGMAV2.modules.users.model.BeanUser` - Usado por repositorios de usuarios
2. `tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.UserEntity` - Usado por `BeanPersonalInformation`

Esto puede causar inconsistencias. **Recomendación a futuro**: Unificar usando solo `BeanUser` en todas las relaciones.

## Pasos Siguientes

1. **Ejecuta las consultas SQL de verificación** (sección "Consultas SQL de Verificación")
2. **Elige la opción de solución** que corresponda a tu caso
3. **Ejecuta el SQL de la solución elegida**
4. **Verifica desde la API** usando los endpoints mostrados
5. **Revisa los logs** para confirmar que ahora encuentra la información personal

## Contacto para Soporte

Si necesitas ayuda adicional o los pasos no resuelven el problema, proporciona:
- Resultado de las consultas SQL de verificación
- Logs completos de la aplicación al llamar `/api/sigmav2/users/me/complete`
- Confirmación de qué opción de solución ejecutaste

