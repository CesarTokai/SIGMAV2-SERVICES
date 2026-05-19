# 📋 Implementación de Marbetes Cancelados - Instrucciones de Uso

## 🎯 Funcionalidades Implementadas

Se han agregado las siguientes funcionalidades a los módulos de marbetes:

### 1. **Modal de Resumen de Generación de Marbetes**
Al generar marbetes, ahora se muestra un modal con información detallada:
- ✅ **Total Generados**: Cantidad total de marbetes generados
- ✅ **Con Existencias**: Marbetes generados con existencias disponibles
- ⚠️ **Sin Existencias (Cancelados)**: Marbetes que no tienen existencias
- 🔢 **Rango de Folios**: Primer y último folio generado

### 2. **Tabla de Marbetes Cancelados**
Nueva sección que muestra los marbetes que fueron generados sin existencias:
- Vista de todos los marbetes con estado "Cancelado"
- Información de folio, producto, clave, existencias actuales
- Botón para mostrar/ocultar la tabla

### 3. **Actualización de Existencias**
Permite actualizar las existencias de marbetes cancelados:
- Campo de entrada para nueva existencia
- Botón "Actualizar" para guardar cambios
- Si la existencia es mayor a 0, el marbete se reactiva automáticamente
- Solo se actualiza la existencia, no el folio ni otros datos

---

## 📍 Ubicación de los Cambios

### **Archivo: ConsultaCaptura.vue**
Ruta: `src/modules/admin/views/marbetesAdmin/ConsultaCaptura.vue`

**Nuevas funcionalidades:**
- ✨ Modal de resumen después de generar marbetes en lote
- 📊 Tabla de marbetes cancelados con botón para mostrar/ocultar
- 🔄 Función para actualizar existencias de marbetes cancelados

### **Archivo: ImpresionMarbetes.vue**
Ruta: `src/modules/admin/views/marbetesAdmin/ImpresionMarbetes.vue`

**Nuevas funcionalidades:**
- 📊 Tabla de marbetes cancelados con botón para mostrar/ocultar
- 🔄 Función para actualizar existencias de marbetes cancelados
- La función `generarMarbetesConResumen` está disponible para uso futuro

---

## 🚀 Cómo Usar

### **Paso 1: Generar Marbetes**
1. Ve a "Consulta y Captura de Marbetes"
2. Selecciona un **Período** y un **Almacén**
3. Ingresa los **Folios Solicitados** para cada producto (cantidad de marbetes a imprimir)
4. Click en el botón **"Generar Marbetes"**
5. Se mostrará un **modal de resumen** con:
   - Total de marbetes generados
   - Cantidad con existencias
   - Cantidad sin existencias (cancelados)
   - Rango de folios generados

### **Paso 2: Ver Marbetes Cancelados**
1. En la misma pantalla o en "Impresión de Marbetes"
2. Click en el botón **"Mostrar Cancelados"**
3. Se desplegará una tabla con los marbetes sin existencias
4. Verás: Folio, Clave Producto, Producto, Existencias Actuales, Estado

### **Paso 3: Actualizar Existencias**
1. En la tabla de marbetes cancelados
2. Busca el marbete que ahora tiene existencias
3. Ingresa la **Nueva Existencia** en el campo correspondiente
4. Click en el botón **"Actualizar"**
5. Se mostrará un mensaje de confirmación
6. Si la existencia es > 0, el marbete se **reactiva automáticamente**
7. El marbete desaparecerá de la tabla de cancelados y volverá a la tabla principal

---

## 🔗 APIs Utilizadas

### **1. Consultar Marbetes Cancelados**
```
GET /api/sigmav2/labels/cancelled
Parámetros: periodId, warehouseId
```

### **2. Actualizar Existencias**
```
PUT /api/sigmav2/labels/cancelled/update-stock
Body: { folio, existenciasActuales, notas }
```

### **3. Generar Marbetes en Lote**
```
POST /api/sigmav2/labels/generate/batch
Body: { periodId, warehouseId, products: [{productId, labelsToGenerate}] }
```

---

## ⚙️ Servicios Creados

Se creó el archivo **MarbetesService.ts** en:
```
src/services/MarbetesService.ts
```

Este servicio incluye todas las funciones necesarias para:
- ✅ Solicitar folios
- ✅ Generar marbetes
- ✅ Consultar resumen
- ✅ Consultar marbetes cancelados
- ✅ Actualizar existencias
- ✅ Imprimir marbetes
- ✅ Contar marbetes

---

## 📊 Flujo de Trabajo

```
1. Usuario ingresa folios solicitados
   ↓
2. Usuario genera marbetes
   ↓
3. Sistema genera marbetes:
   - Con existencias → Estado: Activo
   - Sin existencias → Estado: Cancelado
   ↓
4. Se muestra modal con resumen:
   - Total generados: 100
   - Con existencias: 85
   - Sin existencias: 15
   ↓
5. Los 15 marbetes sin existencias van a tabla de "Cancelados"
   ↓
6. Más adelante, si un producto recibe existencias:
   - Usuario actualiza existencia en tabla de cancelados
   - Sistema valida y reactiva el marbete automáticamente
   - Marbete vuelve a tabla principal como "Activo"
```

---

## 🎨 Interfaz Visual

### **Modal de Resumen**
- Diseño moderno con tarjetas de colores
- Morado: Total generados
- Verde: Con existencias
- Amarillo: Sin existencias (cancelados)
- Azul: Rango de folios
- Alerta informativa si hay marbetes cancelados

### **Tabla de Marbetes Cancelados**
- Botón "Mostrar/Ocultar Cancelados"
- Tabla con columnas: Folio, Clave, Producto, Existencias, Nueva Existencia, Estado, Acciones
- Campo de entrada numérico para nueva existencia
- Botón "Actualizar" por cada fila
- Badge rojo para folio y estado "Cancelado"
- Badge amarillo para existencias actuales
- Información contextual sobre el proceso

---

## ✅ Validaciones Implementadas

1. **Al actualizar existencias:**
   - No permite valores negativos
   - Valida que se ingrese un número válido
   - Confirma con modal antes de actualizar
   - Muestra mensaje de éxito/error

2. **Al cargar marbetes cancelados:**
   - Valida que exista período y almacén seleccionado
   - Muestra mensaje si no hay cancelados
   - Loading state mientras carga

3. **Al generar marbetes:**
   - Valida período y almacén
   - Valida que haya productos con folios solicitados
   - Muestra resumen completo

---

## 🔄 Recarga Automática

Después de cada acción, el sistema recarga automáticamente:
- ✅ Tabla de marbetes generados
- ✅ Tabla de marbetes cancelados
- ✅ Actualización de contadores

---

## 📱 Responsive

Todas las nuevas funcionalidades son completamente responsive:
- ✅ Desktop (1920px+)
- ✅ Laptop (1200px+)
- ✅ Tablet (768px+)
- ✅ Mobile (< 768px)

---

## 🐛 Manejo de Errores

- Mensajes de error claros y descriptivos
- Toast notifications para feedback inmediato
- Validación de datos antes de enviar a API
- Loading states para mejorar UX

---

## 📝 Notas Importantes

1. **Solo se actualiza la existencia**: No se puede modificar el folio ni otros datos del marbete.
2. **Reactivación automática**: Si la existencia es > 0, el marbete cambia de "Cancelado" a "Activo" automáticamente.
3. **Sincronización**: Las tablas se sincronizan automáticamente después de cada actualización.
4. **Persistencia**: Los cambios se guardan inmediatamente en la base de datos.

---

## 🎯 Beneficios

✅ **Mayor control**: Vista completa de marbetes con y sin existencias
✅ **Flexibilidad**: Posibilidad de reactivar marbetes cuando llegan existencias
✅ **Transparencia**: Modal de resumen muestra información detallada
✅ **Eficiencia**: Actualización rápida sin necesidad de regenerar marbetes
✅ **Trazabilidad**: Historial de cambios en existencias

---

## 👨‍💻 Soporte Técnico

Si tienes dudas o encuentras algún problema:
1. Verifica que las APIs estén respondiendo correctamente
2. Revisa la consola del navegador para errores
3. Asegúrate de tener permisos adecuados
4. Verifica que el backend esté configurado correctamente

---

**Fecha de implementación**: Diciembre 2025
**Versión**: 2.0
**Estado**: ✅ Implementado y Funcionando

