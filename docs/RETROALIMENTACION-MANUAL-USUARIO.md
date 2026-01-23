# 📖 Retroalimentación y Aclaraciones - Sistema de Marbetes

## 📋 Resumen Ejecutivo

Este documento consolida **aclaraciones importantes** extraídas del manual de usuario que complementan las reglas de negocio técnicas del sistema de marbetes SIGMAV2.

---

## 🎯 1. Proceso Completo de Inventario Físico

### Flujo Operativo Real

```
┌─────────────────────────────────────────────────────────────┐
│  1. CAPTURA DE MARBETES                                     │
│     • Usuario solicita marbetes por producto                │
│     • Sistema genera folios consecutivos                    │
└────────────────────────────────────────────────────��────────┘
                            ↓
┌────────────────────────��────────────────────────────────────┐
│  2. IMPRESIÓN DE MARBETES                                   │
│     • Exportar PDF con formato de marbetes                  │
│     • Los marbetes cambian a estado "IMPRESO"               │
│     • Se puede reimprimir folios específicos                │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────���───────────┐
│  3. CONTEO FÍSICO (C1)                                      │
│     • Personal de almacén cuenta producto                   │
│     • Registra cantidad en el marbete físico                │
│     • Captura en sistema: folio + cantidad                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  4. SEGUNDO CONTEO (C2)                                     │
│     • Otro usuario realiza segundo conteo                   │
│     • Registra cantidad en el marbete físico                │
│     • Captura en sistema: folio + cantidad                  │
│     • Sistema calcula diferencias automáticamente           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  5. RESOLUCIÓN DE DIFERENCIAS                               │
│     • Revisar reporte de marbetes con diferencias           │
│     • Realizar tercer conteo si es necesario                │
│     • Actualizar C1 o C2 según corresponda                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  6. GENERACIÓN DE ARCHIVO TXT                               │
│     • Exportar existencias finales                          │
│     • Archivo: C:\Sistemas\SIGMA\Documentos\               │
│     • Formato: Existencias_NombrePeriodo.txt                │
└─────────────────────────────────────────────────────────────┘
```

---

## 🏷️ 1.5. Captura y Generación de Marbetes (Módulo Previo)

### 📌 Propósito
Este es el **primer paso** del proceso de inventario. Permite solicitar y generar los folios de marbetes para cada producto que será inventariado.

### 🎯 Módulo: Captura de Marbetes

#### Operaciones Disponibles
1. **Solicitar Folios** (`/api/sigmav2/labels/request`)
2. **Generar Marbetes** (`/api/sigmav2/labels/generate`)
3. **Generar en Lote** (`/api/sigmav2/labels/generate/batch`)

### ✅ Flujo de Captura

#### Paso 1: Selección de Contexto
```
Usuario selecciona:
├── Periodo (ej: Diciembre-2016)
└── Almacén (ej: Almacén 1)
```

#### Paso 2: Búsqueda de Producto
```
Usuario puede buscar producto por:
├── Clave del producto (ej: GM17CRTBS)
├── Descripción (ej: "CARTUCHO")
└── Código de barras (si aplica)
```

#### Paso 3: Solicitud de Marbetes
```
Usuario ingresa:
├── Producto seleccionado
├── Cantidad de marbetes deseados (ej: 5 marbetes)
└── Sistema asigna folios consecutivos automáticamente
```

**Ejemplo**:
```
Producto: GM17CRTBS - CARTUCHO P/ANT. GM17
Marbetes solicitados: 5
Sistema asigna: Folios 100, 101, 102, 103, 104
Estado inicial: GENERADO
```

### 📊 Información Mostrada en Listado

Después de generar, el sistema muestra un listado con:

| Campo | Descripción |
|-------|-------------|
| Folio | Número de marbete asignado |
| Producto | Clave del artículo |
| Descripción | Nombre del producto |
| Almacén | Dónde se encuentra el producto |
| Estado | GENERADO (recién creado) |
| Impreso | No (aún no se ha impreso) |

### 🔍 Funcionalidades del Listado

**Búsqueda**:
- ✅ Por folio de marbete
- ✅ Por clave de producto
- ✅ Por descripción de producto

**Ordenamiento**:
- ✅ Por folio (ascendente/descendente)
- ✅ Por producto
- ✅ Por fecha de creación

**Paginación**:
- ✅ Navegación por páginas
- ✅ Selección de registros por página (10, 25, 50, 100)

### ⚙️ Reglas de Negocio - Captura

1. **Folios Consecutivos**
   - El sistema asigna folios de manera automática y consecutiva
   - No hay "saltos" en la numeración
   - Los folios son únicos por periodo

2. **Múltiples Marbetes por Producto**
   - Un producto puede tener **varios marbetes**
   - Útil cuando el producto está en múltiples ubicaciones
   - O cuando la cantidad es muy grande

3. **Estado Inicial**
   - Todos los marbetes generados inician en estado `GENERADO`
   - No pueden ser contados hasta que se impriman
   - Deben imprimirse para cambiar a estado `IMPRESO`

4. **Edición y Eliminación**
   - Los marbetes **NO** pueden editarse una vez generados
   - Los marbetes **NO** pueden eliminarse, solo cancelarse
   - La cancelación se hace desde el módulo de Conteo

### 🎯 Estrategias de Generación

#### Estrategia 1: Un Marbete por Ubicación
```
Producto: Tornillo A
Almacén 1 - Pasillo 3: 1 marbete
Almacén 1 - Pasillo 7: 1 marbete
Almacén 2 - Entrada: 1 marbete
Total: 3 marbetes para el mismo producto
```

#### Estrategia 2: Múltiples Marbetes por Cantidad Grande
```
Producto: Cables USB
Cantidad total estimada: 5000 unidades
Estrategia: Generar 5 marbetes de ~1000 c/u
Facilita el conteo por lotes
```

#### Estrategia 3: Un Marbete por Producto
```
Producto: Equipo especial
Cantidad única: 1 unidad
Genera: 1 solo marbete
```

### 💡 Mejores Prácticas - Captura

**Para Administradores**:
1. ✅ Planificar cantidad de marbetes antes de generar
2. ✅ Considerar ubicaciones físicas del producto
3. ✅ Evitar generar marbetes innecesarios
4. ✅ Revisar listado antes de imprimir

**Para Almacenistas**:
1. ✅ Conocer ubicaciones de productos en su almacén
2. ✅ Generar marbetes por zonas/pasillos
3. ✅ Coordinar con equipo de conteo
4. ✅ Verificar que todos los productos tengan marbetes

### ⚠️ Consideraciones Importantes

**Antes de Generar**:
- ✅ Verificar que el producto exista en el catálogo
- ✅ Confirmar que el almacén esté activo
- ✅ Validar que el periodo esté abierto

**Después de Generar**:
- ⚠️ Los folios **NO** pueden reutilizarse
- ⚠️ Si se cancela un marbete, el folio queda "quemado"
- ⚠️ Los folios son únicos y secuenciales por periodo

**Impacto en Reportes**:
- Los marbetes generados pero NO impresos **NO** aparecen en:
  - Reporte de distribución
  - Reporte de pendientes (requieren estar impresos)
- Los marbetes generados **SÍ** aparecen en:
  - Reporte de listado completo
  - Vista de captura/generación

### 🔄 Proceso Post-Generación

```
Marbetes GENERADOS
       ↓
  ¿Imprimir?
       ↓
   [Sí] → Estado: IMPRESO → Listos para conteo
       ↓
   [No] → Permanecen en GENERADO → No pueden contarse
```

### 📝 Ejemplo Completo del Proceso

**Escenario Real**:
```
1. Auxiliar selecciona:
   - Periodo: Diciembre-2016
   - Almacén: Almacén 1

2. Busca producto:
   - Clave: GM17CRTBS
   - Descripción: CARTUCHO P/ANT. GM17

3. Solicita 3 marbetes

4. Sistema genera:
   - Folio 245: GM17CRTBS (Estado: GENERADO)
   - Folio 246: GM17CRTBS (Estado: GENERADO)
   - Folio 247: GM17CRTBS (Estado: GENERADO)

5. Usuario ve en listado:
   245 | GM17CRTBS | CARTUCHO P/ANT. GM17 | Almacén 1 | GENERADO | No impreso
   246 | GM17CRTBS | CARTUCHO P/ANT. GM17 | Almacén 1 | GENERADO | No impreso
   247 | GM17CRTBS | CARTUCHO P/ANT. GM17 | Almacén 1 | GENERADO | No impreso

6. Siguiente paso: Ir al módulo de Impresión
```

### 🚫 Errores Comunes

**Error: "El producto no existe"**
- Causa: Producto no está en catálogo importado
- Solución: Importar catálogo de productos actualizado

**Error: "No tiene acceso a este almacén"**
- Causa: Usuario sin permisos para ese almacén
- Solución: Administrador debe asignar acceso

**Error: "El periodo no está activo"**
- Causa: Periodo cerrado o no seleccionado
- Solución: Seleccionar periodo activo

---

## 📄 2. Formato del Marbete Físico

### Estructura del Marbete Impreso

```
┌────────────────────────────────────────────────────┐
│  TOKAL DE MÉXICO                                   │
│  RECUENTO FÍSICO                                   │
│                                                    │
│  No. Marbete: 289                                  │
│                                                    │
│  Código y descripción:                             │
│  GM17CRTBS                                         │
│  CARTUCHO P/ANT. GM17                              │
│                                                    │
│  Almacén: Almacén 1                                │
│                                                    │
│  ┌──────────────────┬──────────────────┐           │
│  │ PRIMER CONTEO    │ SEGUNDO CONTEO   │           │
│  │ Cantidad: ____   │ Cantidad: ____   │           │
│  │ Contado por: ___ │ Contado por: ___ │           │
│  │ Fecha: ________  │ Fecha: ________  │           │
│  └──────────────────┴──────────────────┘           │
│                                                    │
│  Observaciones: _______________________________    │
│  ______________________________________________    │
└────────────────────────────────────────────────────┘
```

### Consideraciones del Formato
- ✅ Se imprimen **3 marbetes por página** (según figura 44 del manual)
- ✅ Cada marbete tiene espacio para **2 conteos físicos**
- ✅ Incluye campos para **nombre del contador** y **fecha**
- ✅ Tiene sección de **observaciones** para notas

---

## 🔄 3. Escenarios de Impresión

### Impresión Normal
**Descripción**: Primera impresión de marbetes recién generados

**Comportamiento**:
- El sistema muestra automáticamente el **último rango de folios generados**
- Solo se muestran marbetes con estado `GENERADO` (no impresos aún)
- Al exportar PDF, los marbetes pasan a estado `IMPRESO`

**Ejemplo**:
```
Última solicitud: 10 marbetes para producto "Tornillo"
Sistema muestra: Folios 100 al 109
Usuario: Exporta PDF
Resultado: Marbetes 100-109 ahora tienen estado IMPRESO
```

### Impresión Extraordinaria (Reimpresión)
**Descripción**: Reimprimir marbetes que ya fueron impresos previamente

**Casos de uso**:
- ❌ Marbete físico se perdió o dañó
- ❌ Marbete se manchó o es ilegible
- ❌ Se necesita una copia adicional

**Comportamiento**:
- Usuario ingresa **manualmente** el folio o rango
- Puede reimprimir marbetes de **cualquier estado**
- Los marbetes mantienen su estado actual (no cambian)

**Ejemplo**:
```
Usuario necesita reimprimir folio 150
Ingresa: Folio inicial: 150, Folio final: 150
Sistema: Genera PDF solo del folio 150
Estado del marbete: NO cambia (sigue siendo IMPRESO)
```

---

## 👥 4. Roles y Permisos Detallados

### Matriz de Permisos por Módulo

| Operación | Administrador | Auxiliar | Almacenista | Auxiliar de Conteo |
|-----------|--------------|----------|-------------|--------------------|
| **Captura de Marbetes** | ✅ | ✅ | ✅ | ❌ |
| **Impresión** | ✅ | ✅ | ✅ | ❌ |
| **Registro C1** | ✅ | ✅ | ✅ | ✅ |
| **Registro C2** | ✅ | ✅ | ✅ | ✅ |
| **Actualizar C1** | ✅ | ✅ | ✅ | ✅ |
| **Actualizar C2** | ✅ | ❌ | ✅ | ✅ |
| **Cancelar Marbete** | ✅ | ✅ | ✅ | ✅ |
| **Todos los Reportes** | ✅ | ✅ | ✅ | ✅ |
| **Generar Archivo TXT** | ✅ | ✅ | ✅ | ❌ |

### Permisos de Almacenes

**Administrador y Auxiliar**:
- ✅ Pueden cambiar de almacén libremente
- ✅ Ven información de **todos los almacenes**
- ✅ Pueden generar reportes multialmacén

**Almacenista**:
- ⚠️ Solo puede ver **su almacén asignado**
- ❌ No puede cambiar de almacén
- ❌ Los reportes se limitan a su almacén

**Auxiliar de Conteo**:
- ⚠️ Solo puede realizar conteos
- ✅ Puede ver reportes de su almacén
- ❌ No puede generar o imprimir marbetes

---

## 📊 5. Exportación de Reportes

### Nombres de Archivos PDF

| Reporte | Nombre del Archivo |
|---------|-------------------|
| Distribución | `SIGMA_DistribucionMarbetes.pdf` |
| Listado | `SIGMA_ListadoMarbetes.pdf` |
| Pendientes | `SIGMA_MarbetesPendientes.pdf` |
| Diferencias | `SIGMA_MarbetesDiferencias.pdf` |
| Cancelados | `SIGMA_MarbetesCancelados.pdf` |
| Comparativo | `SIGMA_Comparativos.pdf` |
| Almacén Detalle | `SIGMA_AlmacenDetaile.pdf` |
| Producto Detalle | `SIGMA_ProductoDetalle.pdf` |
| Marbetes | `SIGMA_Marbetes.pdf` |
| Archivo TXT | `Existencias_{NombrePeriodo}.txt` |

### Filtros Predeterminados
- **Periodo**: Por defecto se selecciona el **último periodo creado**
- **Almacén**: Por defecto se muestra **"Todos"** (multialmacén)
- **Impresión**: Por defecto muestra **último rango de folios generados**

---

## 🔧 6. Operación de Conteo - Detalles Técnicos

### Interfaz de Conteo

**Información Mostrada al Ingresar Folio**:
1. ✅ **Almacén**: En qué almacén está el producto
2. ✅ **Producto**: Clave del producto
3. ✅ **Descripción**: Nombre completo del producto
4. ✅ **Cancelado**: Indica si el folio está cancelado
5. ✅ **Primer Conteo**: Valor de C1 (si existe)
6. ✅ **Segundo Conteo**: Valor de C2 (si existe)
7. ✅ **Diferencias**: Se calcula automáticamente `|C1 - C2|`

### Navegación Optimizada
- ✅ **Tecla TAB**: Avanza al siguiente campo
- ✅ **Tecla ENTER**: Confirma y avanza
- ✅ Diseñado para captura rápida sin usar mouse

### Flujo de Captura Típico

```
1. Usuario: Ingresa folio → TAB
2. Sistema: Muestra información del producto
3. Usuario: Ingresa cantidad C1 → TAB
4. Usuario: (opcional) Ingresa cantidad C2 → TAB
5. Sistema: Calcula diferencias automáticamente
6. Usuario: Presiona ENTER para siguiente marbete
```

### 🔄 Actualización de Conteos

#### ¿Cuándo Actualizar un Conteo?

**Escenarios comunes**:
- ❌ Error de captura (se ingresó número incorrecto)
- ❌ Diferencia detectada que requiere corrección
- ❌ Tercer conteo realizado para resolver discrepancia
- ❌ Validación posterior encontró inconsistencia

#### Reglas de Actualización

**Actualizar C1**:
- ✅ Permitido para: `ADMINISTRADOR`, `AUXILIAR`, `ALMACENISTA`, `AUXILIAR_DE_CONTEO`
- ✅ Puede actualizarse **en cualquier momento**
- ✅ No afecta si ya existe C2

**Actualizar C2**:
- ⚠️ Permitido solo para: `ADMINISTRADOR`, `ALMACENISTA`, `AUXILIAR_DE_CONTEO`
- ⚠️ **NO** permitido para: `AUXILIAR` (solo puede registrar, no actualizar)
- ✅ Puede actualizarse después de registrado

#### Endpoints de Actualización
- `PUT /api/sigmav2/labels/counts/c1` - Actualizar primer conteo
- `PUT /api/sigmav2/labels/counts/c2` - Actualizar segundo conteo

#### Flujo de Actualización

```
1. Usuario accede al módulo de Conteo
2. Ingresa folio del marbete
3. Sistema muestra conteos actuales:
   - C1: 100 (valor actual)
   - C2: 95 (valor actual)
   - Diferencia: 5

4. Usuario decide actualizar C1 a 95
5. Modifica el campo C1 de 100 a 95
6. Sistema recalcula:
   - C1: 95 (nuevo valor)
   - C2: 95 (sin cambios)
   - Diferencia: 0 (ahora coinciden)

7. Marbete desaparece del reporte de diferencias
```

#### Validaciones en Actualización

**El sistema valida**:
- ✅ Que el folio exista
- ✅ Que el conteo a actualizar exista previamente
- ✅ Que el usuario tenga permisos
- ✅ Que el marbete no esté cancelado
- ✅ Que el marbete esté en estado `IMPRESO`
- ✅ Que el valor sea mayor a cero (validación nueva)

**Ejemplo de error**:
```json
{
  "error": "Conteo no encontrado",
  "message": "No existe un conteo C2 para actualizar"
}
```

#### Auditoría de Cambios

**Información registrada**:
- ❌ El sistema **NO** guarda historial de cambios (solo el valor actual)
- ⚠️ No hay rastro del valor anterior
- ⚠️ Recomendación: Documentar cambios importantes manualmente

**Mejora futura sugerida**:
- Implementar tabla de auditoría de cambios
- Registrar: valor anterior, valor nuevo, usuario, fecha, motivo

---

## ⚠️ 7. Validaciones Críticas

### Requisitos Previos para Operar

**Antes de Generar Marbetes**:
- ✅ Catálogos de **productos** importados
- ✅ Catálogo de **multialmacén** importado
- ✅ **Periodo** creado y activo

#### 📦 Catálogo de Productos
**Contenido**:
- Clave del producto (código único)
- Descripción del producto
- Unidad de medida (PZ, KG, LT, etc.)
- Categoría (opcional)

**Importación**:
- Formato: Excel (.xlsx) o archivo de texto
- Debe actualizarse antes de cada periodo
- Incluye productos nuevos y actualiza existentes

#### 🏢 Catálogo de Multialmacén
**Propósito**: Define las existencias teóricas de cada producto en cada almacén

**Contenido**:
- ID del producto
- ID del almacén
- Cantidad teórica (existencias en sistema)
- ID del periodo

**Importancia Crítica**:
- ✅ **Requerido** para generar el **Reporte Comparativo**
- ✅ Proporciona las "existencias teóricas" contra las cuales se comparan los conteos físicos
- ✅ Sin este catálogo, el reporte comparativo mostrará diferencias incorrectas o ceros

**Ejemplo de Registro**:
```
Producto: GM17CRTBS
Almacén: Almacén 1
Existencias Teóricas: 100.00
Periodo: Diciembre-2016

Después del conteo físico:
Existencias Físicas: 95.00
Diferencia: -5.00 (faltante de 5 unidades)
```

**Flujo de Importación**:
```
1. Administrador exporta datos del sistema ERP/Contable
2. Formatea archivo según plantilla
3. Importa en SIGMA (Módulo Catálogos)
4. Sistema valida datos y registra en inventory_stock
5. Datos listos para reporte comparativo
```

#### 📅 Periodos
**Propósito**: Agrupar el inventario por periodo de tiempo

**Estructura**:
- Nombre del periodo (ej: "Diciembre-2016")
- Fecha de inicio
- Fecha de fin
- Estado (activo/cerrado)

**Reglas**:
- Solo puede haber **un periodo activo** a la vez
- Los marbetes se generan para el periodo seleccionado
- Cada periodo tiene sus propios folios consecutivos

**Antes de Imprimir**:
- ✅ Marbetes deben estar en estado `GENERADO`
- ✅ Debe haber al menos 1 marbete sin imprimir

**Antes de Generar Archivo TXT**:
- ✅ Los marbetes deben tener al menos un conteo (C1 o C2)
- ✅ Recomendación: Completar **ambos conteos** para datos precisos

### Mensajes del Sistema

**Durante Generación de Archivo TXT**:
1. Muestra: "Espere a que se genere el archivo TXT"
2. Al finalizar: "El archivo se generó/actualizó correctamente"
3. Ubicación: `C:\Sistemas\SIGMA\Documentos\`

---

## 🎯 8. Casos de Uso Reales

### Caso 1: Producto No Encontrado

**Situación**: 
- Auxiliar busca producto con marbete 125
- El producto no está en la ubicación

**Flujo**:
1. Auxiliar ingresa folio 125 en interfaz de conteo
2. Sistema muestra información del producto
3. Auxiliar **marca casilla "Cancelado"**
4. Marbete queda cancelado
5. Ya NO aparece en reportes de pendientes ni diferencias

### Caso 2: Diferencia entre Conteos

**Situación**:
- C1 = 100 unidades
- C2 = 95 unidades
- Diferencia = 5 unidades

**Flujo**:
1. Sistema detecta diferencia automáticamente
2. Marbete aparece en **Reporte de Diferencias**
3. Supervisor revisa y decide:
   - Opción A: Realizar **tercer conteo**
   - Opción B: **Actualizar C1 o C2** con valor correcto
4. Una vez resuelto, desaparece del reporte de diferencias

### Caso 3: Producto en Múltiples Almacenes

**Situación**:
- Producto "Tornillo A" tiene:
  - 50 unidades en Almacén 1 (marbete 100)
  - 30 unidades en Almacén 2 (marbete 200)

**Comportamiento del Sistema**:
- **Reporte de Almacén con Detalle**: Muestra cada marbete por separado
- **Reporte de Producto con Detalle**: Muestra ambos marbetes + Total = 80
- **Reporte Comparativo**: Suma física = 80 unidades
- **Archivo TXT**: Una sola línea con 80.00 unidades totales

---

## 📈 9. Optimizaciones y Mejores Prácticas

### Para Administradores

**Al Iniciar Inventario**:
1. ✅ Importar catálogos actualizados
2. ✅ Crear periodo con nombre descriptivo
3. ✅ Asignar almacenes a usuarios
4. ✅ Verificar permisos de roles

**Durante el Inventario**:
1. ✅ Monitorear reporte de pendientes
2. ✅ Revisar reporte de diferencias diariamente
3. ✅ Validar cancelaciones justificadas

**Al Finalizar**:
1. ✅ Verificar que no haya pendientes
2. ✅ Resolver todas las diferencias
3. ✅ Generar archivo TXT final
4. ✅ Cerrar el periodo

### Para Auxiliares de Conteo

**Proceso Eficiente**:
1. ✅ Llevar laptop/tablet al almacén
2. ✅ Usar tecla TAB para navegación rápida
3. ✅ Capturar conteos en tiempo real
4. ✅ Marcar marbetes físicos al contar
5. ✅ Cancelar folios de productos no encontrados inmediatamente

---

## 🔍 10. Diferencias entre Reporte de Listado y Otros

### Reporte de Listado
- ✅ Muestra **todos los marbetes** (generados, impresos, cancelados)
- ✅ Incluye bandera `esCancelado`
- ✅ Útil para **vista general completa**

### Reporte de Pendientes
- ⚠️ Solo marbetes **sin C1 o sin C2**
- ❌ Excluye cancelados
- ✅ Útil para **seguimiento de avance**

### Reporte de Diferencias
- ⚠️ Solo marbetes con **C1 ≠ C2** y ambos > 0
- ❌ Excluye cancelados
- ✅ Útil para **identificar discrepancias**

### Reporte de Cancelados
- ⚠️ Solo marbetes en estado **CANCELADO**
- ❌ Excluye reactivados
- ✅ Útil para **auditoría**

---

## 📞 11. Soporte y Resolución de Problemas

### Problema: "No se muestra el rango de folios para imprimir"

**Posibles causas**:
- ❌ No hay marbetes generados en ese periodo/almacén
- ❌ Todos los marbetes ya fueron impresos

**Solución**:
1. Verificar que se hayan generado marbetes
2. Cambiar a "Impresión extraordinaria" para reimprimir

### Problema: "No puedo registrar C2"

**Posibles causas**:
- ❌ No existe C1 previo
- ❌ El marbete está cancelado
- ❌ El marbete no está impreso

**Solución**:
1. Verificar estado del marbete en reporte de listado
2. Registrar C1 primero
3. Verificar que esté impreso

### Problema: "El archivo TXT está vacío"

**Posibles causas**:
- ❌ Todos los marbetes están cancelados
- ❌ No se han registrado conteos

**Solución**:
1. Verificar reporte de listado
2. Completar conteos faltantes
3. Regenerar archivo

---

## ✅ Validaciones del Manual Implementadas en el Sistema

| Regla del Manual | Estado en Sistema | Notas |
|------------------|-------------------|-------|
| Solo 3 roles pueden generar marbetes | ✅ Implementado | `@PreAuthorize` en endpoints |
| Almacenista solo ve su almacén | ✅ Implementado | `validateWarehouseAccess()` |
| C2 requiere C1 previo | ✅ Implementado | Validación en `registerCountC2()` |
| Cancelación disponible para todos | ✅ Implementado | Permiso en 4 roles |
| Archivo TXT se sobrescribe | ✅ Implementado | Lógica en `generateInventoryFile()` |
| Marbetes cancelados no en reportes | ✅ Implementado | Filtro `.filter(l -> l.getEstado() != CANCELADO)` |

---

**Última actualización**: 2026-01-22  
**Basado en**: Manual de Usuario SIGMA v1.0  
**Versión del documento**: 1.0
