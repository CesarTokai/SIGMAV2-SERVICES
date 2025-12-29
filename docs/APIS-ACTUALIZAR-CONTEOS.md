# ✅ APIs para Actualizar Conteos - Documentación

## 📝 Resumen

Se han creado **2 nuevos endpoints** para permitir la **actualización de conteos** ya registrados (C1 y C2).

---

## 🆕 Nuevas APIs Implementadas

### **1. Actualizar Primer Conteo (C1)**

**Endpoint**: `PUT /api/sigmav2/labels/counts/c1`

**Descripción**: Permite actualizar el valor del primer conteo (C1) de un marbete.

**Roles permitidos**:
- ✅ ADMINISTRADOR
- ✅ ALMACENISTA
- ✅ AUXILIAR
- ✅ AUXILIAR_DE_CONTEO

**Body (JSON)**:
```json
{
  "folio": 10001,
  "countedValue": 105.50,
  "observaciones": "Corrección de conteo inicial"
}
```

**Ejemplo de uso**:
```bash
PUT http://localhost:8080/api/sigmav2/labels/counts/c1
Authorization: Bearer {tu_token_jwt}
Content-Type: application/json

Body:
{
  "folio": 10001,
  "countedValue": 105.50,
  "observaciones": "Se corrigió el conteo por error de captura"
}
```

**Respuesta exitosa** (200 OK):
```json
{
  "idCountEvent": 123,
  "folio": 10001,
  "userId": 5,
  "countNumber": 1,
  "countedValue": 105.50,
  "roleAtTime": "ALMACENISTA",
  "isFinal": false,
  "createdAt": "2025-12-09T10:30:00"
}
```

**Errores posibles**:
- `404`: "El folio no existe"
- `404`: "No existe un conteo C1 para actualizar"
- `403`: "No tiene permiso para actualizar C1"
- `400`: "El marbete está CANCELADO"
- `400`: "El marbete no está IMPRESO"

---

### **2. Actualizar Segundo Conteo (C2)**

**Endpoint**: `PUT /api/sigmav2/labels/counts/c2`

**Descripción**: Permite actualizar el valor del segundo conteo (C2) de un marbete.

**Roles permitidos** (más restrictivo):
- ✅ ADMINISTRADOR
- ✅ AUXILIAR_DE_CONTEO

**Body (JSON)**:
```json
{
  "folio": 10001,
  "countedValue": 103.00,
  "observaciones": "Corrección en segundo conteo"
}
```

**Ejemplo de uso**:
```bash
PUT http://localhost:8080/api/sigmav2/labels/counts/c2
Authorization: Bearer {tu_token_jwt}
Content-Type: application/json

Body:
{
  "folio": 10001,
  "countedValue": 103.00,
  "observaciones": "Ajuste por reconteo verificado"
}
```

**Respuesta exitosa** (200 OK):
```json
{
  "idCountEvent": 124,
  "folio": 10001,
  "userId": 7,
  "countNumber": 2,
  "countedValue": 103.00,
  "roleAtTime": "AUXILIAR_DE_CONTEO",
  "isFinal": true,
  "createdAt": "2025-12-09T11:00:00"
}
```

**Errores posibles**:
- `404`: "El folio no existe"
- `404`: "No existe un conteo C2 para actualizar"
- `403`: "No tiene permiso para actualizar C2. Solo ADMINISTRADOR o AUXILIAR_DE_CONTEO"
- `400`: "El marbete está CANCELADO"
- `400`: "El marbete no está IMPRESO"

---

## 🔄 Comparación: Registrar vs Actualizar

### **Registrar Conteo** (POST - Crear nuevo):
```
POST /api/sigmav2/labels/counts/c1
Body: { "folio": 10001, "countedValue": 100.00 }
```
- ✅ Crea un NUEVO registro de conteo
- ❌ Error si ya existe un conteo C1

### **Actualizar Conteo** (PUT - Modificar existente):
```
PUT /api/sigmav2/labels/counts/c1
Body: { "folio": 10001, "countedValue": 105.00 }
```
- ✅ Actualiza el registro EXISTENTE
- ❌ Error si NO existe un conteo C1

---

## 📊 Flujo Completo de Trabajo

```
1. Registrar C1 (Primera vez)
   POST /api/sigmav2/labels/counts/c1
   Body: { "folio": 10001, "countedValue": 100.00 }

   ↓

2. Si cometiste un error, ACTUALIZAR C1
   PUT /api/sigmav2/labels/counts/c1
   Body: { "folio": 10001, "countedValue": 105.50 }

   ↓

3. Registrar C2 (Primera vez)
   POST /api/sigmav2/labels/counts/c2
   Body: { "folio": 10001, "countedValue": 103.00 }

   ↓

4. Si necesitas corregir C2, ACTUALIZAR C2
   PUT /api/sigmav2/labels/counts/c2
   Body: { "folio": 10001, "countedValue": 102.00 }
```

---

## 🔐 Permisos por Endpoint

| Endpoint | Método | ADMINISTRADOR | ALMACENISTA | AUXILIAR | AUXILIAR_DE_CONTEO |
|----------|--------|---------------|-------------|----------|-------------------|
| `/counts/c1` (registrar) | POST | ✅ | ✅ | ✅ | ✅ |
| `/counts/c1` (actualizar) | PUT | ✅ | ✅ | ✅ | ✅ |
| `/counts/c2` (registrar) | POST | ❌ | ❌ | ❌ | ✅ |
| `/counts/c2` (actualizar) | PUT | ✅ | ❌ | ❌ | ✅ |

---

## 📝 DTO Utilizado

```java
public class UpdateCountDTO {
    @NotNull
    private Long folio;

    @NotNull
    private BigDecimal countedValue;

    private String observaciones; // Opcional
}
```

---

## ⚠️ Reglas de Negocio

1. **Solo se pueden actualizar conteos de marbetes IMPRESOS**
2. **NO se pueden actualizar conteos de marbetes CANCELADOS**
3. **Debe existir el conteo previamente** (no se crea uno nuevo)
4. **Para C2, solo ADMINISTRADOR y AUXILIAR_DE_CONTEO pueden actualizar**
5. **El usuario debe tener acceso al almacén del marbete**

---

## 🎯 Casos de Uso

### **Caso 1: Corrección de Error de Captura**
```
Usuario capturó 100 en vez de 105
→ Usar PUT /counts/c1 para corregir
```

### **Caso 2: Reconteo por Discrepancia**
```
C1=100, C2=98 (diferencia de 2)
Se verifica físicamente y el correcto es 99
→ Usar PUT /counts/c2 para ajustar
```

### **Caso 3: Error de Tecla**
```
Usuario ingresó 1000 en vez de 100
→ Usar PUT /counts/c1 o c2 según corresponda
```

---

## 🚀 Estado de Implementación

✅ **DTO creado**: `UpdateCountDTO.java`
✅ **Métodos en servicio**: `updateCountC1()` y `updateCountC2()`
✅ **Endpoints en controlador**: `PUT /counts/c1` y `PUT /counts/c2`
✅ **Validaciones implementadas**: Permisos, estado del marbete, existencia de conteo previo
✅ **Compilación exitosa**: Sin errores
✅ **Listo para usar**: ✅

---

## 📞 Resumen Rápido

**Para actualizar C1:**
```bash
PUT http://localhost:8080/api/sigmav2/labels/counts/c1
Body: { "folio": 10001, "countedValue": 105.50 }
```

**Para actualizar C2:**
```bash
PUT http://localhost:8080/api/sigmav2/labels/counts/c2
Body: { "folio": 10001, "countedValue": 103.00 }
```

**Ahora SÍ puedes actualizar los conteos ya registrados** 🎉

