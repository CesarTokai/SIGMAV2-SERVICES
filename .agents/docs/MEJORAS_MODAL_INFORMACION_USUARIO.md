# Mejoras al Modal de Información del Usuario

## Fecha: 2026-01-23

## Resumen
Se ha actualizado el modal de información personal del usuario para mostrar **todos los campos** que devuelve la API de usuarios, organizados en secciones lógicas para facilitar su lectura.

## Cambios Realizados

### 1. Componente PersonalInfoModal.vue

Se agregaron los siguientes campos que faltaban en el modal:

#### **Sección: Información Personal**
- ✅ Nombre
- ✅ Apellido Paterno
- ✅ Apellido Materno
- ✅ Teléfono
- ✅ Correo Electrónico
- ✅ Rol
- ✅ Estado (con badge visual: Activo/Inactivo)
- ✅ **NUEVO:** Verificado (con badge: ✓ Verificado / ✗ No Verificado)
- ✅ **NUEVO:** Cuenta Bloqueada (con badge: 🔒 Bloqueada / 🔓 Desbloqueada)
- ✅ **NUEVO:** Sesión Activa (con badge: ● En línea / ○ Desconectado)
- ✅ Comentarios

#### **Sección: Seguridad**
- ✅ **NUEVO:** Intentos Fallidos (con badge de color según la cantidad)
  - Verde: 0 intentos (safe)
  - Amarillo: 1-2 intentos (warning)
  - Rojo: 3+ intentos (danger)
- ✅ **NUEVO:** Último Intento Fallido
- ✅ **NUEVO:** Códigos de Verificación (total generados)
- ✅ **NUEVO:** Último Código Enviado
- ✅ **NUEVO:** Último Bloqueo de Cuenta
- ✅ **NUEVO:** Último Cambio de Contraseña

#### **Sección: Actividad**
- ✅ **NUEVO:** Último Login
- ✅ **NUEVO:** Última Actividad
- ✅ Fecha de Registro
- ✅ Última Actualización

#### **Sección: Almacenes Asignados**
- ✅ **NUEVO:** Lista de almacenes asignados al usuario
  - Se muestran como badges morados
  - Muestra "Sin almacenes asignados" si el array está vacío

### 2. Componente AdminUserManagement.vue

Se modificó el método `viewUser()` para:
- Combinar los datos del usuario de la lista con la información personal adicional
- Intentar obtener información personal adicional (nombre, apellidos, etc.) del endpoint `/personal-information/user/{id}`
- Si falla la petición de información personal, mostrar solo los datos del usuario de la lista
- Mapear todos los campos de la API al objeto `personalInfo`

### 3. Mejoras Visuales

#### Badges y Estilos
Se agregaron estilos para:
- **Status badges**: Activo, Inactivo, Verificado, No Verificado, Bloqueada, Desbloqueada, En línea, Desconectado
- **Attempts badges**: Safe (verde), Warning (amarillo), Danger (rojo)
- **Warehouse badges**: Badges morados para cada almacén asignado
- **Section titles**: Títulos de sección con borde inferior azul

#### Layout
- Se aumentó el ancho máximo del modal de 760px a **900px**
- Se agregó scroll vertical para contenido largo
- Se ajustó el layout para que sea responsive
- La columna de detalles ahora tiene flex-shrink para evitar desbordamiento

#### Colores de Badges

| Badge | Color de Fondo | Color de Texto | Uso |
|-------|---------------|----------------|-----|
| Activo | Verde claro | Verde oscuro | Estado activo |
| Inactivo | Rojo claro | Rojo oscuro | Estado inactivo |
| Verificado | Azul claro | Azul oscuro | Usuario verificado |
| No Verificado | Amarillo claro | Amarillo oscuro | Usuario no verificado |
| Bloqueada | Rojo claro | Rojo oscuro | Cuenta bloqueada |
| Desbloqueada | Verde claro | Verde oscuro | Cuenta desbloqueada |
| En línea | Verde claro | Verde oscuro | Sesión activa |
| Desconectado | Gris claro | Gris oscuro | Sesión inactiva |
| Safe | Verde claro | Verde oscuro | 0 intentos fallidos |
| Warning | Amarillo claro | Amarillo oscuro | 1-2 intentos fallidos |
| Danger | Rojo claro | Rojo oscuro | 3+ intentos fallidos |
| Almacén | Morado claro | Morado oscuro | Almacenes asignados |

## Estructura de Datos de la API

La API de usuarios devuelve la siguiente estructura:

```json
{
  "id": 4,
  "email": "usuario@tokai.com.mx",
  "role": "AUXILIAR",
  "status": true,
  "verified": true,
  "attempts": 0,
  "lastTryAt": null,
  "createdAt": "2026-01-23T16:10:55.644247",
  "updatedAt": "2026-01-23T16:10:55.644247",
  "verificationCode": null,
  "totalVerificationCodes": 0,
  "lastVerificationCodeSent": null,
  "accountLocked": false,
  "comments": null,
  "assignedWarehouses": [],
  "lastActivityAt": null,
  "lastLoginAt": null,
  "lastAccountLockAt": null,
  "lastPasswordChangeAt": null,
  "sessionActive": false
}
```

Todos estos campos ahora se muestran en el modal de información del usuario.

## Método getAttemptsClass()

Se agregó un método para clasificar los intentos fallidos:

```javascript
getAttemptsClass(attempts) {
  if (!attempts || attempts === 0) return 'safe';
  if (attempts <= 2) return 'warning';
  return 'danger';
}
```

## Archivos Modificados

1. `src/components/Admin/PersonalInfoModal.vue`
   - Se agregaron todas las nuevas secciones y campos
   - Se agregaron estilos para badges
   - Se ajustó el layout del modal

2. `src/components/Admin/AdminUserManagement.vue`
   - Se modificó el método `viewUser()` para combinar datos

## Validación

✅ No hay errores de compilación en los componentes modificados
✅ Los warnings son solo sobre selectores CSS no usados (comportamiento normal de Vue)
✅ Todos los campos de la API están mapeados en el modal

## Capturas de Ejemplo

### Antes
- Solo mostraba: Nombre, Apellidos, Teléfono, Email, Rol, Estado, Fechas, Comentarios

### Después
- **Información Personal**: 11 campos (incluyendo estado de verificación, bloqueo y sesión)
- **Seguridad**: 6 campos (intentos, códigos, bloqueos, cambios de contraseña)
- **Actividad**: 4 campos (logins, actividad, fechas)
- **Almacenes**: Lista de almacenes asignados

## Recomendaciones

1. **Permisos**: Considerar si todos los roles deben ver toda esta información o si algunos campos deberían estar restringidos por rol.

2. **Acciones**: Agregar botones de acción en el modal:
   - "Desbloquear cuenta" (si está bloqueada)
   - "Resetear intentos" (si tiene intentos fallidos)
   - "Enviar código de verificación" (si no está verificado)

3. **Auditoría**: Los campos de actividad y seguridad son valiosos para auditoría. Considerar agregar un log de eventos completo.

4. **Notificaciones**: Considerar agregar alertas visuales si:
   - La cuenta está bloqueada
   - Hay muchos intentos fallidos
   - El usuario no está verificado

## Próximos Pasos Sugeridos

1. Implementar acciones desde el modal (desbloquear, resetear intentos, etc.)
2. Agregar filtros en la tabla por estado de verificación y bloqueo
3. Agregar indicadores visuales en la tabla principal para cuentas bloqueadas
4. Implementar un sistema de notificaciones para administradores cuando una cuenta se bloquea

---
**Documento generado automáticamente el 2026-01-23**
