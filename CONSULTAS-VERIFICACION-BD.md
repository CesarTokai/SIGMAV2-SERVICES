# 🔍 VERIFICACIÓN DIRECTA EN BASE DE DATOS

## Consultas SQL para Verificar que TODO se Registra

### **1. Ver todos los campos de un usuario específico**

```sql
SELECT 
    user_id,
    email,
    role,
    status,
    is_verified,
    created_at,
    updated_at,
    last_login_at,
    last_activity_at,
    password_changed_at,
    attempts,
    last_try_at
FROM users
WHERE email = 'cgonzalez@tokai.com.mx';
```

**¿Qué debería mostrar?**
```
user_id: 1
email: cgonzalez@tokai.com.mx
role: ADMINISTRADOR
status: 1 (true)
is_verified: 1 (true)
created_at: 2026-02-09 09:06:06.766126        ← Fecha de Registro ✅
updated_at: 2026-02-09 09:06:06.766126        ← Última Actualización ✅
last_login_at: 2026-02-12 09:00:00            ← Último Login ✅ (o NULL si no ha login)
last_activity_at: 2026-02-12 16:22:01.730891  ← Última Actividad ✅
password_changed_at: NULL                      ← Cambio Contraseña (NULL si no ha cambiado)
attempts: 0
last_try_at: NULL
```

---

### **2. Ver histórico de Login/Actividad de todos los usuarios**

```sql
SELECT 
    user_id,
    email,
    last_login_at,
    last_activity_at,
    TIMESTAMPDIFF(MINUTE, last_activity_at, NOW()) AS minutos_sin_actividad
FROM users
WHERE last_login_at IS NOT NULL
ORDER BY last_activity_at DESC;
```

**¿Qué debería mostrar?**
Una tabla con todos los usuarios que han iniciado sesión, mostrando:
- Última vez que iniciaron sesión
- Última actividad
- Cuántos minutos llevan sin hacer actividad

---

### **3. Ver usuarios que NUNCA han iniciado sesión (después de la implementación)**

```sql
SELECT 
    user_id,
    email,
    created_at,
    last_login_at
FROM users
WHERE last_login_at IS NULL
ORDER BY created_at DESC;
```

**¿Qué debería mostrar?**
Los usuarios con `last_login_at = NULL` son usuarios que:
- Fueron creados después de que se implementó el campo
- Pero aún no han iniciado sesión

---

### **4. Ver actividad más reciente**

```sql
SELECT 
    user_id,
    email,
    last_activity_at,
    created_at,
    updated_at
FROM users
ORDER BY last_activity_at DESC
LIMIT 10;
```

**¿Qué debería mostrar?**
Los 10 usuarios más activos recientemente.

---

### **5. Contar almacenes asignados por usuario (Para verificar que también funcionan)**

```sql
SELECT 
    uw.user_id,
    u.email,
    COUNT(uw.warehouse_id) AS total_almacenes
FROM user_warehouses uw
JOIN users u ON uw.user_id = u.user_id
GROUP BY uw.user_id, u.email
ORDER BY total_almacenes DESC;
```

**¿Qué debería mostrar?**
Los usuarios con su cantidad de almacenes asignados.

---

### **6. Verificar la estructura de la tabla users**

```sql
DESCRIBE users;
-- O en MySQL:
SHOW COLUMNS FROM users;
```

**¿Qué debería mostrar?**
Todos estos campos:
- ✅ `user_id` INT PRIMARY KEY
- ✅ `email` VARCHAR UNIQUE
- ✅ `created_at` DATETIME
- ✅ `updated_at` DATETIME
- ✅ `last_login_at` DATETIME (puede ser NULL)
- ✅ `last_activity_at` DATETIME (puede ser NULL)
- ✅ `password_changed_at` DATETIME (puede ser NULL)

---

## ✅ CHECKLIST DE VERIFICACIÓN

Ejecuta estas queries en orden y marca cada una:

### **Checklist:**

- [ ] Query #1: Verificar que usuario tiene `created_at`
- [ ] Query #1: Verificar que usuario tiene `updated_at`
- [ ] Query #1: Verificar que usuario tiene `last_activity_at` (no debe ser NULL si ha hecho requests)
- [ ] Query #2: Verificar que hay usuarios con `last_login_at` fechado
- [ ] Query #3: Verificar que hay usuarios con `last_login_at = NULL` (normales, nunca han login)
- [ ] Query #4: Verificar que `last_activity_at` se actualiza constantemente
- [ ] Query #5: Verificar que los almacenes se cuentan correctamente
- [ ] Query #6: Verificar que todos los campos existen en tabla

---

## 🔧 ¿Qué hacer si encuentras problemas?

### **Si `last_login_at` siempre es NULL:**
```sql
-- Busca si el campo existe realmente
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'users' AND COLUMN_NAME = 'last_login_at';

-- Si no existe, la migración no se aplicó
-- Solución: Reinicia la aplicación para que Flyway aplique las migraciones
```

### **Si `last_activity_at` no se actualiza:**
```sql
-- Verifica que el filtro UserActivityFilter está activo
-- Mira en los logs: "UPDATE USER ACTIVITY"

-- Si no aparece, el filtro no está siendo ejecutado
-- Solución: Verifica que el @Component esté registrado correctamente
```

### **Si los timestamps están vacíos:**
```sql
-- Asegúrate que los usuarios fueron creados/modificados DESPUÉS de añadir los campos
-- Crea un usuario nuevo y verifica que tiene createdAt
```

---

## 📊 Ejemplo de Resultado Esperado

Cuando ejecutes la Query #1, deberías ver algo como esto:

```
mysql> SELECT user_id, email, created_at, updated_at, last_login_at, last_activity_at 
       FROM users WHERE email = 'cgonzalez@tokai.com.mx';

| user_id | email                    | created_at          | updated_at          | last_login_at       | last_activity_at    |
|---------|--------------------------|---------------------|---------------------|---------------------|---------------------|
| 1       | cgonzalez@tokai.com.mx   | 2026-02-09 09:06:06 | 2026-02-09 09:06:06 | 2026-02-12 09:00:00 | 2026-02-12 16:22:01 |
```

✅ Si ves estas fechas, TODO está funcionando correctamente.

---

## 🎯 CONCLUSIÓN

Si todas las queries retornan datos con fechas/horas:
✅ **El sistema de registro de actividad funciona PERFECTAMENTE**


