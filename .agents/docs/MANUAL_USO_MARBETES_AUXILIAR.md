# 🎯 INSTRUCCIONES DE USO - Pantallas de Marbetes por Rol

## 📚 Descripción General

Se han implementado las **3 pantallas de gestión de marbetes** para los roles **AUXILIAR** y **AUXILIAR DE CONTEO**, replicando exactamente las funcionalidades del rol **ALMACENISTA**.

---

## 🔓 Acceso por Rol

### 🏢 ALMACENISTA
**URL:** `https://tu-app.com/almacenista/marbetes`

**Rutas Disponibles:**
- Dashboard: `/almacenista`
- Gestión de Marbetes: `/almacenista/marbetes`

### 👤 AUXILIAR
**URL:** `https://tu-app.com/auxiliar/marbetes`

**Rutas Disponibles:**
- Dashboard: `/auxiliar`
- Gestión de Marbetes: `/auxiliar/marbetes`

### 👤👤 AUXILIAR DE CONTEO
**URL:** `https://tu-app.com/auxiliar-de-conteo/marbetes`

**Rutas Disponibles:**
- Dashboard: `/auxiliar-de-conteo`
- Gestión de Marbetes: `/auxiliar-de-conteo/marbetes`

---

## 📖 Manual de Uso

### ✅ Pantalla 1: Consulta y Captura de Marbetes

**Acceso:** Botón "📋 Consulta y Captura"

**Funciones:**
1. **Seleccionar Período y Almacén**
   - Dropdown de períodos disponibles
   - Dropdown de almacenes asignados
   - El período se guarda automáticamente

2. **Buscar Marbetes**
   - Usar el buscador para filtrar por:
     - Clave de producto
     - Nombre de producto
     - Clave de almacén
     - Almacén
     - Estado

3. **Tabla de Marbetes**
   - Columnas: Folios Solicitados, Folios Existentes, Clave Producto, Producto, Clave Almacén, Almacén, Estado, Existencias
   - Hacer clic en header para ordenar (↑ ASC, ↓ DESC)
   - Editar "Folios Solicitados" directamente en la tabla
   - Cambios se guardan automáticamente

4. **Generar Marbetes**
   - Botón verde "Generar Marbetes"
   - Valida que haya folios solicitados
   - Muestra resumen de generación
   - Opción para reactivar marbetes cancelados

5. **Paginación**
   - Navegar entre páginas
   - Cambiar cantidad de registros (20, 50, 100, 200, 500)

---

### ✅ Pantalla 2: Conteo de Marbetes

**Acceso:** Botón "🔢 Conteo"

**Funciones:**
1. **Seleccionar Período**
   - Dropdown de períodos

2. **Buscar Marbete**
   - Ingresar número de folio
   - Presionar "Buscar" o ENTER
   - El sistema valida que exista el folio

3. **Capturar Primer Conteo (C1)**
   - Ingresar cantidad en primer campo
   - Solo acepta números enteros
   - Presionar ENTER o botón "Guardar"
   - Se guarda automáticamente

4. **Capturar Segundo Conteo (C2)**
   - Una vez guardado C1, ingresar segundo conteo
   - Solo acepta números enteros
   - Presionar ENTER o botón "Guardar"
   - Se calcula automáticamente la diferencia

5. **Diferencia**
   - Se calcula y muestra automáticamente: C2 - C1
   - Valores positivos: más producto del esperado
   - Valores negativos: menos producto del esperado

6. **Acciones**
   - **Guardar:** Guarda los conteos ingresados
   - **Limpiar:** Borra todos los datos y enfoca en buscar nuevo folio
   - **Cancelar Marbete:** Marca el folio como cancelado

**Atajos de Teclado:**
- `Alt + F` → Enfoca búsqueda de folio
- `Alt + L` → Limpia formulario
- `ESC` → Limpia formulario

---

### ✅ Pantalla 3: Impresión de Marbetes

**Acceso:** Botón "🖨️ Impresión"

**Funciones:**
1. **Seleccionar Período y Almacén**
   - Dropdowns para seleccionar

2. **Contador de Pendientes**
   - Muestra cantidad de marbetes listos para imprimir
   - Color verde si hay pendientes
   - Gris si no hay pendientes

3. **Tabla de Marbetes**
   - Columnas: Folio, Clave Producto, Producto, Clave Almacén, Almacén, Existencias, Cantidad Folios, Rango Folios, Impreso, Fecha Impresión
   - Vista de lectura (sin edición)

4. **Botón Imprimir**
   - Genera PDF con los marbetes pendientes
   - Mostrar resumen de generación
   - Mensaje: "X Marbetes pendientes de impresión"

5. **Visor de PDFs**
   - Lista de PDFs generados
   - Click para seleccionar PDF
   - Vista previa en iframe
   - Opción: Abrir en nueva pestaña

6. **Acciones sobre PDFs**
   - **💾 Descargar:** Descarga el PDF a tu computadora
   - **🖨️ Imprimir:** Abre ventana de impresión del navegador
   - **🗑️ Eliminar:** Elimina de la lista
   - **Descargar Todos:** Descarga todos los PDFs generados

---

## 🎨 Elementos de Interfaz

### Estructura General
```
┌─────────────────────────────────────────────────┐
│   🏷️ Gestión de Marbetes                         │
├─────────────────────────────────────────────────┤
│  [📋 Consulta]  [🖨️ Impresión]  [🔢 Conteo]    │
├─────────────────────────────────────────────────┤
│   Período: [Dropdown]  Almacén: [Dropdown]     │
├─────────────────────────────────────────────────┤
│   [Contenido de la Pantalla Seleccionada]      │
└─────────────────────────────────────────────────┘
```

### Badges de Estado
- **Verde (Éxito):** Operación completada
- **Rojo (Error):** Falló la operación
- **Amarillo (Advertencia):** Sin existencias
- **Gris (Secundario):** Estado normal

### Botones
- **Verde Oscuro:** Acción principal (Guardar, Generar, Imprimir)
- **Gris:** Acciones secundarias (Limpiar, Cancelar)
- **Rojo:** Acciones destructivas (Eliminar, Cancelar Marbete)

---

## ⚠️ Validaciones y Mensajes

### Búsqueda de Marbete
- ❌ "Ingresa un folio para buscar" - Campo vacío
- ❌ "Selecciona período y almacén" - No hay selección
- ❌ "No se encontró el folio X" - Folio no existe
- ✅ "Marbete encontrado" - Búsqueda exitosa

### Captura de Conteos
- ❌ "Ingresa el primer conteo para guardar" - C1 vacío
- ❌ "Ingresa el segundo conteo para guardar" - C2 vacío
- ✅ "Primer conteo guardado" - C1 guardado
- ✅ "Segundo conteo guardado" - C2 guardado

### Generación de Marbetes
- ❌ "No hay productos para generar" - Sin folios solicitados
- ⚠️ "Productos sin existencias" - Advertencia
- ✅ "Marbetes generados exitosamente" - Éxito

---

## 🔍 Búsqueda y Filtrado

### Búsqueda por Texto
- Se busca en tiempo real (con debounce de 500ms)
- Busca en múltiples campos
- No diferencia mayúsculas/minúsculas

### Ordenamiento
- Click en encabezado de columna para ordenar
- ↑ Ascendente (A-Z, 0-9)
- ↓ Descendente (Z-A, 9-0)
- ↕️ Sin ordenar

### Paginación
- Botones: « Primera, ‹ Anterior, Siguiente ›, Última »
- Indicador: Página X de Y
- Selector de registros por página

---

## 💾 Almacenamiento de Datos

### Período Guardado Automáticamente
- Cada vez que seleccionas un período, se guarda
- Se recupera la próxima vez que accedes
- Se sincroniza entre pantallas

### Conteos Guardados
- Se guardan en tiempo real al presionar ENTER
- Se pueden actualizar
- No se pierden al navegar

### PDFs Generados
- Se guardan en memoria del navegador
- Se pierden al cerrar la pestaña
- Se pueden descargar para guardar permanentemente

---

## 🖥️ Compatibilidad

### Navegadores Soportados
- ✅ Chrome 90+
- ✅ Firefox 88+
- ✅ Safari 14+
- ✅ Edge 90+

### Dispositivos
- ✅ Desktop (recomendado)
- ✅ Tablet (responsive)
- ⚠️ Mobile (interfaz simplificada)

---

## 🚨 Resolución de Problemas

### "No se puede acceder a /marbetes"
- **Causa:** No estás logueado o tu rol no es correcto
- **Solución:** Verifica tu usuario y rol asignado

### "El folio no se encuentra"
- **Causa:** El folio no existe en este período/almacén
- **Solución:** Verifica el número de folio y la selección

### "Error al guardar conteo"
- **Causa:** Problema de conexión o validación de datos
- **Solución:** Revisa tu conexión y verifica los datos

### "PDF no se abre"
- **Causa:** navegador bloqueó popup o PDF corrupto
- **Solución:** Descarga el PDF o intenta en otro navegador

### "Período no se guarda"
- **Causa:** Local storage del navegador deshabilitado
- **Solución:** Habilita cookies/storage en tu navegador

---

## 📞 Soporte

Si encuentras problemas:

1. **Verifica:**
   - Tu conexión a internet
   - Tu rol de usuario
   - Que hayas seleccionado período y almacén

2. **Limpia Caché:**
   - Press: Ctrl + Shift + Supr
   - Limpia cookies y cache
   - Recarga la página

3. **Reporta el Problema:**
   - Anota el error exacto
   - Indica qué pasos realizabas
   - Comunica a tu administrador

---

## ✨ Tips y Trucos

### ⚡ Atajos de Teclado
- `ENTER` en folio → Busca automáticamente
- `ENTER` en conteo → Guarda automáticamente
- `TAB` → Navega entre campos
- `Alt + F` → Enfoca búsqueda
- `Alt + L` → Limpia formulario
- `ESC` → Limpia y enfoca búsqueda

### 🎯 Captura Rápida
1. Ingresa folio
2. Presiona ENTER (busca)
3. Ingresa primer conteo
4. Presiona ENTER (guarda y enfoca C2)
5. Ingresa segundo conteo
6. Presiona ENTER (guarda y limpia)
7. Repite desde paso 1

### 📊 Generación Eficiente
1. Ingresa folios en la tabla
2. Presiona TAB entre campos (auto-guarda)
3. Haz clic en "Generar Marbetes"
4. Confirma en el modal
5. Descarga PDF inmediatamente

---

## 📅 Versión

**Última Actualización:** 2026-02-09  
**Versión:** 1.0  
**Estado:** Producción ✅

---

*Escrito para usuarios de AUXILIAR, AUXILIAR DE CONTEO y ALMACENISTA*

