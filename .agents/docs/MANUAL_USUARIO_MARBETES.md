# 📘 Manual de Usuario - Sistema de Marbetes

## Índice
1. [Acceso al Sistema](#acceso-al-sistema)
2. [Consulta y Captura de Marbetes](#1-consulta-y-captura-de-marbetes)
3. [Impresión de Marbetes](#2-impresión-de-marbetes)
4. [Conteo de Marbetes](#3-conteo-de-marbetes)

---

## Acceso al Sistema

1. Inicia sesión en SIGMA V2
2. En el menú lateral, haz clic en **"Gestión de marbetes"**
3. Se mostrará el módulo de marbetes con 3 pestañas:
   - 📋 **Consulta y Captura**
   - 🖨️ **Impresión**
   - 🔢 **Conteo**

---

## 1. Consulta y Captura de Marbetes

### Objetivo
Generar los marbetes necesarios para cada producto y almacén basados en un período de inventario.

### Pasos para Generar Marbetes

#### Paso 1: Seleccionar Período y Almacén
1. En la sección superior, selecciona el **período** deseado
2. Selecciona el **almacén** correspondiente
3. El sistema mostrará información resumida (estado del período y total de registros)

#### Paso 2: Generar Marbetes
1. Haz clic en el botón **"✨ Generar Marbetes"** (esquina superior derecha)
2. Aparecerá un modal de confirmación con:
   - Período seleccionado
   - Almacén seleccionado
3. Confirma la operación haciendo clic en **"Sí, generar"**
4. El sistema procesará la solicitud y mostrará un mensaje de éxito
5. La tabla se actualizará automáticamente con los marbetes generados

#### Paso 3: Consultar Marbetes Generados
- **Buscador**: Usa el campo de búsqueda para filtrar por:
  - Clave de producto
  - Nombre de producto
  - Clave de almacén
  - Almacén
  - Estado

- **Tabla de Marbetes**: Visualiza:
  - **Folios Solicitados**: Cantidad de folios que se pidieron
  - **Folios Existentes**: Cantidad de folios ya generados
  - **Clave Producto**: Código del producto
  - **Producto**: Nombre del producto
  - **Clave Almacén**: Código del almacén
  - **Almacén**: Nombre del almacén
  - **Estado**: Estado del marbete (Activo, Pendiente, etc.)
  - **Existencias**: Cantidad en stock

- **Paginación**: 
  - Usa los botones « ‹ › » para navegar entre páginas
  - Cambia el tamaño de página (20, 50, 100, 200, 500 registros)

### Notas Importantes
- ⚠️ Solo se pueden generar marbetes una vez por período y almacén
- 📝 Asegúrate de que los catálogos de inventario y multi-almacén estén importados previamente
- 👤 Los almacenistas solo pueden generar marbetes para su almacén asignado

---

## 2. Impresión de Marbetes

### Objetivo
Imprimir los marbetes generados en formato PDF para su uso en el inventario físico.

### Tipos de Impresión

#### A. Impresión Normal
- **¿Cuándo usar?**: Para imprimir marbetes recién generados
- **Característica**: El sistema muestra automáticamente el último rango de folios
- **Ventaja**: Rápido y sin necesidad de buscar folios

#### B. Impresión Extraordinaria
- **¿Cuándo usar?**: Para reimprimir marbetes específicos
- **Característica**: Puedes seleccionar cualquier folio o rango manualmente
- **Ventaja**: Útil para reposición de marbetes extraviados o dañados

### Pasos para Imprimir Marbetes

#### Paso 1: Seleccionar Período y Almacén
1. Elige el **período** del cual deseas imprimir marbetes
2. Selecciona el **almacén**
   - **Almacenistas**: Solo verás tu almacén asignado
   - **Administradores y Auxiliares**: Puedes cambiar de almacén

#### Paso 2: Configurar Impresión

##### Elegir Tipo de Impresión:
- Marca **"Impresión Normal"** para folios recientes
- Marca **"Impresión Extraordinaria"** para reimpresión

##### Seleccionar Folios:
- ✅ **Imprimir rango de folios** (activado por defecto):
  - En modo **Normal**: El rango se completa automáticamente
  - En modo **Extraordinario**: Ingresa manualmente:
    - **Folio Inicial**: Ejemplo: 1001
    - **Folio Final**: Ejemplo: 1050

- ☐ **Imprimir folio único** (desactivar rango):
  - Ingresa un solo **folio** para imprimir

#### Paso 3: Imprimir
1. Haz clic en el botón **"🖨️ Imprimir Marbetes"**
2. Revisa la información en el modal de confirmación:
   - Período
   - Almacén
   - Folios a imprimir
   - Total de marbetes
3. Confirma con **"Sí, imprimir"**
4. El PDF se abrirá automáticamente en una nueva pestaña
5. Usa las opciones de tu navegador para imprimir o guardar

#### Paso 4: Verificar Impresión
En la sección **"Marbetes Generados del Periodo"**:
- Verifica la columna **"Impreso"**:
  - ✓ **Sí** (verde): Ya fue impreso
  - ✗ **No** (rojo): Aún no se ha impreso
- Consulta la **Fecha de Impresión** para cada marbete

### Formato del Marbete Impreso
Cada marbete incluye:
- Folio único
- Código y nombre del producto
- Almacén
- Existencias esperadas
- Código de barras (si aplica)
- Espacios para captura de conteos

---

## 3. Conteo de Marbetes

### Objetivo
Capturar los resultados del conteo físico de cada marbete impreso.

### Pasos para Capturar Conteos

#### Paso 1: Seleccionar Período y Almacén
1. Elige el **período** activo
2. Selecciona el **almacén**
3. Observa el **panel de estadísticas**:
   - 📊 **Total Marbetes**: Cantidad total de marbetes generados
   - ✅ **Contados**: Marbetes con conteo completo (verde)
   - ⏳ **Pendientes**: Marbetes sin contar o parciales (amarillo)
   - ❌ **Cancelados**: Marbetes no utilizados (rojo)

#### Paso 2: Buscar Marbete
1. En **"Número de Marbete (Folio)"**, ingresa el folio del marbete físico
2. Presiona **Enter** o haz clic en **"🔍 Buscar"**
3. El sistema mostrará la información del marbete:
   - Producto y clave
   - Almacén
   - **Existencias Esperadas** (destacado)

#### Paso 3: Capturar Conteos

##### Método Recomendado (Teclado):
1. **Primer Conteo**:
   - El cursor se posicionará automáticamente
   - Ingresa la cantidad contada
   - Presiona **Enter**

2. **Segundo Conteo**:
   - Ingresa la segunda cantidad
   - Presiona **Enter** para guardar automáticamente

##### Método Alternativo (Mouse):
1. Ingresa **Conteo 1**
2. Ingresa **Conteo 2**
3. Observa la **Diferencia** (se calcula automáticamente):
   - **Verde** si ambos conteos coinciden
   - **Rojo** si hay diferencia
4. Haz clic en **"💾 Guardar Conteo"**

#### Paso 4: Acciones Adicionales

##### Limpiar Formulario
- Botón **"🔄 Limpiar"**: Resetea el formulario para capturar otro marbete

##### Cancelar Marbete No Utilizado
- Si un marbete no se utilizó (dañado, extraviado, etc.):
  1. Busca el marbete por folio
  2. Haz clic en **"❌ Cancelar Marbete"**
  3. Confirma la acción
  4. ⚠️ **Esta acción es irreversible**

#### Paso 5: Verificar Conteos Capturados
En la tabla **"Marbetes del Periodo"**:
- **Folio**: Número del marbete
- **Producto**: Información del producto
- **Existencias Esperadas**: Cantidad inicial
- **Conteo 1**: Primer conteo capturado
- **Conteo 2**: Segundo conteo capturado
- **Diferencia**: Diferencia entre conteos
- **Estado**: 
  - 🔴 **Pendiente**: Sin conteos
  - 🟡 **Parcial**: Solo un conteo
  - 🟢 **Completo**: Ambos conteos capturados
  - ⚫ **Cancelado**: Marbete cancelado

### Técnicas para Captura Eficiente

#### Flujo Óptimo de Trabajo:
1. Ten los marbetes físicos ordenados por folio
2. Ingresa el folio → Enter
3. Ingresa conteo 1 → Enter
4. Ingresa conteo 2 → Enter
5. El sistema guarda y limpia automáticamente
6. Repite desde el paso 2

#### Manejo de Diferencias:
- Si hay diferencia entre conteos:
  1. Verifica físicamente el producto
  2. Si es necesario, realiza un tercer conteo manual
  3. Edita el conteo si el sistema lo permite
  4. Documenta las diferencias significativas

#### Marbetes Cancelados:
- Cancela marbetes solo cuando:
  - No se encontró el producto físicamente
  - El marbete está dañado y no se puede usar
  - El producto no está disponible para conteo
- **Normativa**: Todos los marbetes no utilizados deben cancelarse

---

## Atajos de Teclado

### En Conteo de Marbetes:
- **Enter** en campo de folio: Buscar marbete
- **Enter** en conteo 1: Pasar a conteo 2
- **Enter** en conteo 2: Guardar conteo
- **Tab**: Navegar entre campos

---

## Permisos por Rol

| Función | Almacenista | Auxiliar | Aux. Conteo | Administrador |
|---------|:-----------:|:--------:|:-----------:|:-------------:|
| Consultar marbetes | ✓ (su almacén) | ✓ (todos) | ✓ (todos) | ✓ (todos) |
| Generar marbetes | ✓ (su almacén) | ✓ (todos) | ✓ (todos) | ✓ (todos) |
| Imprimir marbetes | ✓ (su almacén) | ✓ (todos) | ✓ (todos) | ✓ (todos) |
| Capturar conteos | ✓ (su almacén) | ✓ (todos) | ✓ (todos) | ✓ (todos) |
| Cancelar marbetes | ✓ | ✓ | ✓ | ✓ |
| Cambiar almacén | ✗ | ✓ | ✓ | ✓ |

---

## Solución de Problemas

### No puedo generar marbetes
- ✅ Verifica que no existan marbetes ya generados para ese período/almacén
- ✅ Asegúrate de que el período esté activo
- ✅ Confirma que existan productos en el inventario

### No aparecen marbetes para imprimir
- ✅ Verifica que hayas generado marbetes primero
- ✅ Confirma que el período y almacén seleccionados sean correctos
- ✅ Intenta recargar la página

### No puedo capturar conteo
- ✅ Verifica que el folio sea correcto
- ✅ Asegúrate de que el marbete no esté cancelado
- ✅ Confirma que ambos campos de conteo estén llenos

### El PDF no se abre
- ✅ Verifica que tu navegador permita ventanas emergentes
- ✅ Revisa que tengas un lector de PDF instalado
- ✅ Intenta con otro navegador

---

## Mejores Prácticas

1. **Antes de empezar**:
   - Verifica que los catálogos estén actualizados
   - Genera todos los marbetes necesarios antes de imprimir
   - Imprime con anticipación suficiente

2. **Durante la impresión**:
   - Verifica la calidad de impresión
   - Usa papel resistente
   - Imprime copias de respaldo si es necesario

3. **Durante el conteo**:
   - Organiza los marbetes físicos por folio
   - Mantén un registro manual de respaldo
   - Captura los conteos el mismo día
   - Documenta cualquier anomalía

4. **Al finalizar**:
   - Verifica que todos los marbetes estén contados o cancelados
   - Revisa las diferencias significativas
   - Genera reportes de auditoría

---

## Soporte

Si tienes problemas o preguntas:
1. Contacta a tu supervisor inmediato
2. Comunícate con el área de TI
3. Consulta este manual para procedimientos específicos

---

**Versión del Manual**: 1.0  
**Última Actualización**: 27 de Noviembre de 2025  
**Sistema**: SIGMA V2 - Módulo de Marbetes

