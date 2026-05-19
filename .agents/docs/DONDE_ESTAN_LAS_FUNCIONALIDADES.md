# 📍 Ubicación de las Funcionalidades Implementadas

## 🗺️ Mapa de Navegación Completo

### 🎯 **Cómo Acceder al Módulo de Marbetes**

```
1. Iniciar sesión como ADMINISTRADOR
2. En el menú lateral izquierdo, buscar:
   🏷️ "Gestion de Marbetes"
3. Click en el menú
4. Se abrirá el módulo con 4 pestañas/submódulos
```

---

## 📑 **Estructura del Módulo de Marbetes**

### **Ruta Principal:**
```
URL: /Admin/MarbetesAdmin
Archivo: src/modules/admin/views/marbetesAdmin/MarbetesAdmin.vue
```

### **Layout del Módulo:**
```
Archivo: src/modules/admin/views/marbetesAdmin/MarbetesLayout.vue
```

Este layout contiene 4 submódulos accesibles mediante pestañas:

---

## 🔢 **1. Pestaña: CONSULTA Y CAPTURA** 📋

**Cómo acceder:**
```
Menú Lateral → 🏷️ Gestion de Marbetes → Pestaña "Consulta y Captura" (📋)
O directamente: /Admin/MarbetesAdmin?submodulo=consulta
```

**Archivo:**
```
src/modules/admin/views/marbetesAdmin/ConsultaCaptura.vue
```

**Funcionalidades:**
- Consultar marbetes existentes
- Capturar nuevos marbetes
- Generar marbetes por periodo y almacén

---

## 🖨️ **2. Pestaña: IMPRESIÓN** 🖨️

**Cómo acceder:**
```
Menú Lateral → 🏷️ Gestion de Marbetes → Pestaña "Impresión" (🖨️)
O directamente: /Admin/MarbetesAdmin?submodulo=impresion
```

**Archivo:**
```
src/modules/admin/views/marbetesAdmin/ImpresionMarbetes.vue
```

**Funcionalidades:**
- Imprimir marbetes generados
- Impresión normal o extraordinaria
- Selección por rango de folios

---

## 🔢 **3. Pestaña: CONTEO** 🔢 ⭐ **[MODIFICADO]**

**Cómo acceder:**
```
Menú Lateral → 🏷️ Gestion de Marbetes → Pestaña "Conteo" (🔢)
O directamente: /Admin/MarbetesAdmin?submodulo=conteo
```

**Archivo:**
```
src/modules/admin/views/marbetesAdmin/ConteoMarbetes.vue ⭐ MODIFICADO
```

### **✨ Funcionalidades NUEVAS/MEJORADAS:**

#### ✅ **1. Captura de Conteos (Existente - Ya funcionaba)**
- Buscar marbete por folio
- Ingresar primer conteo
- Ingresar segundo conteo
- Calcular diferencias automáticamente
- Guardar conteos

#### ❌ **2. CANCELACIÓN DE MARBETES (MEJORADO)** ⭐ NUEVO
**Pasos para cancelar un marbete:**

1. **Seleccionar periodo y almacén** (selectores en la parte superior)
2. **Ingresar el número de folio** del marbete a cancelar
3. **Click en "Buscar"** (🔍)
4. **Verificar datos** del marbete (producto, almacén, existencias)
5. **Click en botón rojo "Cancelar Marbete"** (❌)
6. **Se abrirá un modal** con:
   - Información del marbete
   - Campo de texto para **MOTIVO DE CANCELACIÓN** (obligatorio)
7. **Escribir el motivo** (por ejemplo: "Error en captura", "Producto duplicado", etc.)
8. **Confirmar** con "Sí, cancelar"
9. **El marbete queda cancelado** y se actualiza la lista

**API consumida:**
```javascript
POST /sigmav2/labels/cancel
Body: {
  "folio": 1001,
  "periodId": 1,
  "warehouseId": 2,
  "motivoCancelacion": "Error en captura"
}
```

**Reglas de negocio implementadas:**
- ✅ Todos los usuarios pueden cancelar marbetes
- ✅ El motivo es OBLIGATORIO
- ✅ La acción es irreversible
- ✅ Se valida periodo y almacén
- ✅ Se muestra advertencia antes de confirmar
- ✅ Se actualiza automáticamente después de cancelar

---

## 📊 **4. Pestaña: REPORTES** 📊 ⭐ **[NUEVO]**

**Cómo acceder:**
```
Menú Lateral → 🏷️ Gestion de Marbetes → Pestaña "Reportes" (📊) ⭐ NUEVA PESTAÑA
O directamente: /Admin/MarbetesAdmin?submodulo=reportes
```

**Archivo:**
```
src/modules/admin/views/marbetesAdmin/ReportesMarbetes.vue ⭐ ARCHIVO NUEVO
```

### **✨ TODOS LOS REPORTES DISPONIBLES:**

La interfaz muestra **8 tarjetas interactivas**, cada una con un tipo de reporte:

---

#### 📊 **1. Reporte: DISTRIBUCIÓN DE MARBETES**

**Cómo generar:**
1. Click en tarjeta "📊 Distribución de Marbetes"
2. Seleccionar **Periodo**
3. Click en "Generar Reporte"
4. (Opcional) Click en "Descargar Excel"

**Muestra:**
- Usuario que generó los marbetes
- Clave y nombre del almacén
- Primer folio asignado
- Último folio asignado

**API:** `POST /sigmav2/labels/reports/distribution`

---

#### 📋 **2. Reporte: LISTADO DE MARBETES**

**Cómo generar:**
1. Click en tarjeta "📋 Listado de Marbetes"
2. Seleccionar **Periodo**
3. Seleccionar **Almacén** (o marcar "Todos los almacenes")
4. Click en "Generar Reporte"
5. (Opcional) Click en "Descargar Excel"

**Muestra:**
- Número de folio
- Clave del producto
- Descripción del producto
- Unidad de medida
- Almacén
- Conteo 1
- Conteo 2
- Estado (Cancelado o activo)

**API:** `POST /sigmav2/labels/reports/list`

---

#### ⏳ **3. Reporte: MARBETES PENDIENTES**

**Cómo generar:**
1. Click en tarjeta "⏳ Marbetes Pendientes"
2. Seleccionar **Periodo**
3. Seleccionar **Almacén** (o marcar "Todos")
4. Click en "Generar Reporte"

**Muestra:**
- Solo marbetes que NO tienen los dos conteos completos
- Mismo formato que Listado de Marbetes

**API:** `POST /sigmav2/labels/reports/pending`

---

#### ⚠️ **4. Reporte: MARBETES CON DIFERENCIAS**

**Cómo generar:**
1. Click en tarjeta "⚠️ Marbetes con Diferencias"
2. Seleccionar **Periodo**
3. Seleccionar **Almacén** (o marcar "Todos")
4. Click en "Generar Reporte"

**Muestra:**
- Solo marbetes donde Conteo 1 ≠ Conteo 2
- Útil para identificar discrepancias
- Mismo formato que Listado de Marbetes

**API:** `POST /sigmav2/labels/reports/with-differences`

---

#### ❌ **5. Reporte: MARBETES CANCELADOS**

**Cómo generar:**
1. Click en tarjeta "❌ Marbetes Cancelados"
2. Seleccionar **Periodo**
3. Seleccionar **Almacén** (o marcar "Todos")
4. Click en "Generar Reporte"

**Muestra:**
- Solo marbetes que fueron cancelados
- Incluye el motivo de cancelación
- Mismo formato que Listado de Marbetes

**API:** `POST /sigmav2/labels/reports/cancelled`

---

#### 🔄 **6. Reporte: COMPARATIVO**

**Cómo generar:**
1. Click en tarjeta "🔄 Comparativo"
2. Seleccionar **Periodo**
3. Seleccionar **Almacén** (o marcar "Todos")
4. Click en "Generar Reporte"

**Muestra:**
- Clave del almacén
- Clave del producto
- Descripción del producto
- **Existencias Físicas** (lo contado)
- **Existencias Teóricas** (lo que debería haber)
- **Diferencia** (físicas - teóricas)

**API:** `POST /sigmav2/labels/reports/comparative`

**Útil para:**
- Identificar faltantes o sobrantes
- Ajustes de inventario
- Auditorías

---

#### 🏪 **7. Reporte: ALMACÉN CON DETALLE**

**Cómo generar:**
1. Click en tarjeta "🏪 Almacén con Detalle"
2. Seleccionar **Periodo**
3. Seleccionar **Almacén** (o marcar "Todos")
4. Click en "Generar Reporte"

**Muestra:**
- Desglose completo por almacén
- Cada marbete con su producto
- Clave del almacén
- Clave del producto
- Descripción
- Unidad
- Número de marbete
- Cantidad contada
- Estado del marbete

**API:** `POST /sigmav2/labels/reports/warehouse-detail`

---

#### 📦 **8. Reporte: PRODUCTO CON DETALLE**

**Cómo generar:**
1. Click en tarjeta "📦 Producto con Detalle"
2. Seleccionar **Periodo**
3. Click en "Generar Reporte"

**Muestra:**
- Desglose completo por producto
- Ubicación del producto en diferentes almacenes
- Clave del producto
- Descripción
- Unidad
- Almacén donde se encuentra
- Número de marbete
- Existencias en ese almacén
- Total de existencias del producto (suma de todos los almacenes)

**API:** `POST /sigmav2/labels/reports/product-detail`

---

## 🎯 **Resumen Visual de Ubicaciones**

```
📱 APLICACIÓN SIGMAV2
│
├─ 🔐 Login (como ADMINISTRADOR)
│
└─ 📊 Panel de Administración (/Admin)
    │
    └─ 🏷️ Gestion de Marbetes (/Admin/MarbetesAdmin)
        │
        ├─ 📋 [1] Consulta y Captura (ConsultaCaptura.vue)
        │   └─ Consultar y capturar marbetes
        │
        ├─ 🖨️ [2] Impresión (ImpresionMarbetes.vue)
        │   └─ Imprimir marbetes
        │
        ├─ 🔢 [3] Conteo (ConteoMarbetes.vue) ⭐ MODIFICADO
        │   ├─ Capturar conteos
        │   └─ ❌ CANCELAR MARBETES (NUEVO CON MOTIVO)
        │
        └─ 📊 [4] Reportes (ReportesMarbetes.vue) ⭐ NUEVO
            ├─ 📊 Distribución
            ├─ 📋 Listado
            ├─ ⏳ Pendientes
            ├─ ⚠️ Con Diferencias
            ├─ ❌ Cancelados
            ├─ 🔄 Comparativo
            ├─ 🏪 Almacén Detalle
            └─ 📦 Producto Detalle
```

---

## 📂 **Estructura de Archivos en el Proyecto**

```
src/
│
├─ components/
│   └─ AdminSidebar.vue ⭐ MODIFICADO
│       └─ Menú: "🏷️ Gestion de Marbetes" → /Admin/MarbetesAdmin
│
├─ modules/admin/views/marbetesAdmin/
│   ├─ MarbetesAdmin.vue          (Punto de entrada)
│   ├─ MarbetesLayout.vue ⭐ MODIFICADO (Layout con 4 pestañas)
│   ├─ ConsultaCaptura.vue        (Pestaña 1)
│   ├─ ImpresionMarbetes.vue      (Pestaña 2)
│   ├─ ConteoMarbetes.vue ⭐ MODIFICADO (Pestaña 3 - Cancelación mejorada)
│   └─ ReportesMarbetes.vue ⭐ NUEVO (Pestaña 4 - 8 reportes)
│
└─ router/
    └─ index.ts
        └─ Ruta: /Admin/MarbetesAdmin → MarbetesAdmin
```

---

## 🚀 **Instrucciones de Uso Rápido**

### ✅ **Para CANCELAR un Marbete:**
```
1. Login como ADMINISTRADOR
2. Menú → 🏷️ Gestion de Marbetes
3. Pestaña → 🔢 Conteo
4. Seleccionar periodo y almacén
5. Ingresar folio → Buscar
6. Botón rojo "❌ Cancelar Marbete"
7. Escribir motivo → Confirmar
```

### 📊 **Para GENERAR un Reporte:**
```
1. Login como ADMINISTRADOR
2. Menú → 🏷️ Gestion de Marbetes
3. Pestaña → 📊 Reportes (NUEVA)
4. Click en el tipo de reporte deseado
5. Seleccionar periodo (y almacén si aplica)
6. "Generar Reporte"
7. (Opcional) "Descargar Excel"
```

---

## 🎨 **Identificación Visual en la UI**

### **Menú Lateral:**
- Buscar el icono: **🏷️ Gestion de Marbetes**
- Color: Según el tema activo
- Posición: Entre "Gestion de Periodos" e inicio de la lista

### **Pestañas del Módulo:**
```
┌─────────────────────────────────────────────────────────┐
│  📋 Consulta y Captura  │  🖨️ Impresión  │  🔢 Conteo  │  📊 Reportes ⭐  │
└─────────────────────────────────────────────────────────┘
```

### **En Conteo:**
- Botón rojo con icono: **❌ Cancelar Marbete**
- Ubicado debajo de los inputs de conteo
- Al lado de "💾 Guardar Conteo" y "🔄 Limpiar"

### **En Reportes:**
- **8 tarjetas grandes** con iconos
- Tarjeta activa: **fondo azul con gradiente**
- Tarjeta inactiva: **fondo blanco con borde**
- Botones:
  - Azul: **📊 Generar Reporte**
  - Verde: **📥 Descargar Excel**

---

## ✅ **Todo está Implementado y Funcional**

Las funcionalidades están **100% integradas** en el sistema existente:
- ✅ Rutas configuradas
- ✅ Menú lateral actualizado
- ✅ Navegación entre pestañas
- ✅ APIs conectadas
- ✅ Validaciones implementadas
- ✅ Estilos coherentes con el sistema

**¡El sistema está listo para usar!** 🎉

