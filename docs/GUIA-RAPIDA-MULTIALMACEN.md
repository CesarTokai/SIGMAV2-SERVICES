# 📋 GUÍA RÁPIDA - MULTIALMACÉN

**Documento:** Resumen Ejecutivo de Reglas de Negocio  
**Basado en:** Manual de Usuario SIGMA (páginas 26-30)  
**Versión:** 2.0  
**Fecha:** 26 de Enero de 2026

---

## 🎯 DEFINICIÓN EN 30 SEGUNDOS

**MultiAlmacén** es un catálogo que **suministra al SIGMA del inventario por almacén** de toda la empresa para un determinado periodo (mes-año).

```
📁 multialmacen.xlsx → 📊 Catálogo MultiAlmacén → 🏷️ Marbetes
```

---

## 👤 ACCESO

⚠️ **SOLO ADMINISTRADOR**

```
✅ Administrador → Acceso completo
❌ Supervisor → Sin acceso
❌ Almacenista → Sin acceso
❌ Consulta → Sin acceso
```

---

## 🔧 ACCIONES DISPONIBLES

### 1️⃣ Consultar
- Ver inventario de todos los almacenes
- Buscar productos
- Ordenar resultados
- Paginar (10, 25, 50, 100)

### 2️⃣ Importar
- Cargar archivo Excel
- Actualizar existencias
- Crear almacenes/productos nuevos
- Marcar productos dados de baja

---

## 📁 ARCHIVO DE IMPORTACIÓN

**Ubicación obligatoria:**
```
C:\Sistemas\SIGMA\Documentos\multialmacen.xlsx
```

**Estructura:**

| CVE_ALM | CVE_ART | DESCR | STATUS | EXIST |
|---------|---------|-------|--------|-------|
| ALM-01 | PROD-001 | Laptop Dell | A | 100.50 |
| ALM-02 | PROD-001 | Laptop Dell | A | 75.00 |
| ALM-01 | PROD-002 | Mouse Logitech | B | 0.00 |

### Columnas

| Nombre | ¿Qué es? | ¿Obligatorio? |
|--------|----------|---------------|
| **CVE_ALM** | Clave del almacén | ✅ SÍ |
| **CVE_ART** | Clave del producto | ✅ SÍ |
| **DESCR** | Descripción | ⚠️ Opcional* |
| **STATUS** | Estado (A/B) | ✅ SÍ |
| **EXIST** | Existencias | ✅ SÍ |

*Si el producto existe, se ignora. Se obtiene del inventario.

---

## ⚙️ LAS 5 REGLAS DE ORO

### Regla #1: Almacenes Nuevos
```
CVE_ALM en Excel NO existe en SIGMA
    ↓
✅ Se crea automáticamente
✅ Observación: "Este almacén no existía y fue creado en la importación"
```

### Regla #2: Productos Nuevos
```
CVE_ART en Excel NO existe en inventario
    ↓
✅ Se crea automáticamente
✅ Estado: "A" (Alta)
```

### Regla #3: Productos a Importar
```
CVE_ART existe en inventario pero NO en multialmacén
    ↓
✅ Se importa al catálogo
✅ Son los productos "nuevos"
```

### Regla #4: Productos a Actualizar
```
CVE_ART existe en inventario Y en multialmacén
    ↓
✅ Se actualizan sus valores
✅ Con base en lo que esté en el Excel
```

### Regla #5: Productos Dados de Baja
```
CVE_ART existe en multialmacén pero NO en Excel
    ↓
✅ Status cambia a "B" (Baja)
✅ Existencias NO se modifican
```

---

## 🔄 CONCEPTO CLAVE

> **"La operación de importar multialmacén funciona como una variación de ACTUALIZACIÓN del multialmacén para el periodo seleccionado."**

**NO es destructiva:**
- ✅ Permite múltiples importaciones
- ✅ Actualiza/complementa datos
- ✅ Preserva histórico

---

## 📊 INTERFAZ DE CONSULTA

### Tareas que puedes hacer:

#### 1. Consultar Listado
```
Ver todos los productos de todos los almacenes
Paginado y ordenado
```

#### 2. Personalizar Paginación
```
Opciones: 10 | 25 | 50 | 100 registros por página
```

#### 3. Buscar Producto
```
Busca en: Clave producto, Producto, Almacén, Existencias
Búsqueda: Case-insensitive, parcial
```

#### 4. Ordenar Resultados
```
Click en encabezado de columna:
- Clave de producto
- Producto
- Clave de almacén
- Almacén
- Estado (A/B)
- Existencias
```

---

## 📈 ESTADOS DE PRODUCTOS

### A = Alta
- ✅ Producto vigente
- ✅ Aparece en consultas normales
- ✅ Se puede generar marbete

### B = Baja
- ❌ Producto NO vigente
- ⚠️ Aparece en consultas pero marcado
- ❌ NO se puede generar marbete
- ℹ️ Dado de baja para el periodo elegido en el almacén designado

---

## 🚀 FLUJO DE IMPORTACIÓN

```
1. Menú principal
   ↓
2. Catálogos
   ↓
3. Multialmacén
   ↓
4. Botón "Importar inventario"
   ↓
5. Seleccionar periodo (MM-yyyy)
   ↓
6. Botón "Importar"
   ↓
7. Sistema procesa archivo
   ↓
8. Mensaje de confirmación
```

---

## ✅ CONDICIONES PARA IMPORTAR

### Antes de importar, verificar:

- [ ] **Rol:** Soy Administrador
- [ ] **Periodo:** Seleccionado de la lista
- [ ] **Archivo:** Existe en `C:\Sistemas\SIGMA\Documentos\multialmacen.xlsx`
- [ ] **Formato:** Archivo proporcionado por TOKAI (plantilla oficial)
- [ ] **Columnas:** CVE_ALM, CVE_ART, STATUS, EXIST presentes

---

## ⚠️ ERRORES COMUNES

### "Importación podría fallar"
**Causa:** No usar archivo proporcionado por TOKAI  
**Solución:** Usar plantilla oficial

### "Periodo no disponible"
**Causa:** Periodo está CLOSED o LOCKED  
**Solución:** Seleccionar periodo OPEN

### "Acceso denegado"
**Causa:** No tienes rol Administrador  
**Solución:** Contactar administrador del sistema

### "Archivo no encontrado"
**Causa:** Archivo no está en ubicación correcta  
**Solución:** Colocar en `C:\Sistemas\SIGMA\Documentos\`

---

## 💡 TIPS Y MEJORES PRÁCTICAS

### ✅ Hacer

1. **Usar plantilla oficial** proporcionada por TOKAI
2. **Verificar periodo** antes de importar
3. **Revisar estados** (A/B) en el Excel
4. **Mantener formato** de columnas
5. **Hacer respaldo** antes de importación grande

### ❌ Evitar

1. **Modificar estructura** de columnas
2. **Cambiar ubicación** del archivo
3. **Importar en periodo cerrado**
4. **Dejar celdas vacías** en columnas obligatorias
5. **Usar formato CSV** sin verificar encoding

---

## 🔍 BÚSQUEDA RÁPIDA

### ¿Cómo buscar...?

**Un producto específico:**
```
Escribir código en "Buscar" → Enter
Ejemplo: "PROD-001"
```

**Todos los productos de un almacén:**
```
Escribir clave almacén en "Buscar" → Enter
Ejemplo: "ALM-01"
```

**Productos con existencias bajas:**
```
Ordenar por columna "Existencias" → Ascendente
```

**Productos dados de baja:**
```
Ordenar por columna "Estado" → Ver los marcados con "B"
```

---

## 📊 EJEMPLO PRÁCTICO

### Escenario: Actualización de inventario

**Situación:**
- 3 almacenes: CEDIS, TIENDA_A, TIENDA_B
- 50 productos diferentes
- Periodo: Enero 2026
- 15 productos nuevos
- 5 productos descontinuados

**Archivo Excel contiene:**
```
CVE_ALM    CVE_ART      STATUS  EXIST
CEDIS      PROD-001     A       500.00
CEDIS      PROD-002     A       300.00
TIENDA_A   PROD-001     A       100.00
TIENDA_A   PROD-NEW     A       50.00    ← Nuevo
TIENDA_B   PROD-001     A       75.00
```

**Productos NO en Excel:**
```
PROD-999 (existe en BD) → Se marca como "B"
```

**Resultado después de importar:**
```
✅ 150 registros procesados (50 productos × 3 almacenes)
✅ 15 productos nuevos creados
✅ 135 productos actualizados
✅ 5 productos marcados como "B"
✅ Sincronización automática con inventory_stock
```

---

## 🎓 GLOSARIO RÁPIDO

| Término | Significado |
|---------|-------------|
| **CVE_ALM** | Clave del Almacén - Identificador único |
| **CVE_ART** | Clave del Artículo/Producto |
| **DESCR** | Descripción del producto |
| **STATUS A** | Alta - Producto vigente |
| **STATUS B** | Baja - Producto no vigente |
| **EXIST** | Existencias - Cantidad disponible |
| **Periodo** | Mes y año (formato MM-yyyy) |
| **Importar** | Cargar/actualizar datos |
| **Consultar** | Visualizar datos |

---

## 📞 SOPORTE

### ¿Necesitas ayuda?

**Documento completo:**
- Ver: `REGLAS-NEGOCIO-MULTIALMACEN.md` (31 reglas detalladas)

**Manual de Usuario:**
- Ver: Manual de Usuario SIGMA (páginas 26-30)

**Documentación técnica:**
- `FORMATO-EXCEL-MULTIALMACEN.md`
- `TESTING-MULTIALMACEN.md`

---

## 🚦 DIAGRAMA DE FLUJO SIMPLIFICADO

```
┌─────────────────────────────────────┐
│  ¿Tienes rol Administrador?         │
└─────────────┬───────────────────────┘
              │
         ┌────▼────┐
         │   SÍ    │
         └────┬────┘
              │
┌─────────────▼───────────────────────┐
│  Menú → Catálogos → Multialmacén    │
└─────────────┬───────────────────────┘
              │
         ┌────▼────────┐
         │  Consultar  │◄──── Ver inventario
         │     o       │
         │  Importar   │◄──── Actualizar datos
         └────┬────────┘
              │
       [IMPORTAR]
              │
┌─────────────▼───────────────────────┐
│  1. Seleccionar periodo             │
│  2. Verificar archivo existe        │
│  3. Click "Importar"                │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│  SISTEMA APLICA 5 REGLAS:           │
│  1. Crear almacenes nuevos          │
│  2. Crear productos nuevos          │
│  3. Importar productos              │
│  4. Actualizar existentes           │
│  5. Marcar como baja                │
└─────────────┬───────────────────────┘
              │
         ┌────▼────┐
         │  ÉXITO  │
         └────┬────┘
              │
┌─────────────▼───────────────────────┐
│  Multialmacén actualizado           │
│  Sincronizado con inventory_stock   │
│  Listo para generar marbetes        │
└─────────────────────────────────────┘
```

---

**Documento:** Guía Rápida MultiAlmacén  
**Fuente:** Manual de Usuario SIGMA + Reglas de Negocio SIGMAV2  
**Versión:** 2.0  
**Actualizado:** 26 de Enero de 2026
