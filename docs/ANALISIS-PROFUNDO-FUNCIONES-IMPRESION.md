# Análisis Profundo - Funciones de Impresión de Marbetes

**Fecha de Análisis:** 2025-12-29  
**Archivo Analizado:** `LabelServiceImpl.java` (Líneas 268-410)  
**Servicios Relacionados:** `JasperLabelPrintService.java`, `LabelsPersistenceAdapter.java`

---

## 📋 Índice

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura del Sistema de Impresión](#arquitectura-del-sistema-de-impresión)
3. [Análisis Detallado de Funciones](#análisis-detallado-de-funciones)
4. [Errores Detectados](#errores-detectados)
5. [Problemas de Diseño](#problemas-de-diseño)
6. [Recomendaciones](#recomendaciones)

---

## 1. Resumen Ejecutivo

### Estado General
El sistema de impresión de marbetes está **funcionalmente implementado** pero presenta **múltiples problemas críticos** que pueden causar:
- Inconsistencia de datos
- Fallas en producción
- Problemas de concurrencia
- Errores difíciles de diagnosticar

### Severidad de Problemas Encontrados
- 🔴 **CRÍTICOS:** 3 errores
- 🟡 **ADVERTENCIAS:** 5 problemas de diseño
- 🔵 **MEJORAS:** 4 optimizaciones recomendadas

---

## 2. Arquitectura del Sistema de Impresión

### 2.1 Flujo Completo de Impresión

```
Usuario Solicita Impresión
        ↓
LabelServiceImpl.printLabels() ← [FUNCIÓN PRINCIPAL]
        ↓
├─ Validación de Acceso (Rol + Almacén)
├─ Validación de Catálogos Cargados
├─ Determinación del Modo (Selectivo vs Automático)
├─ Búsqueda de Marbetes
├─ Validación de Estados
        ↓
JasperLabelPrintService.generateLabelsPdf() ← [GENERACIÓN PDF]
        ↓
├─ Pre-carga de Cache (Productos + Almacenes)
├─ Construcción de DataSource
├─ Compilación/Carga de Plantilla JRXML
├─ Generación de PDF con JasperReports
        ↓
LabelsPersistenceAdapter.printLabelsRange() ← [PERSISTENCIA]
        ↓
├─ Validación de Rango
├─ Cambio de Estado (GENERADO → IMPRESO)
├─ Registro en Auditoría (label_prints)
        ↓
Retorno de PDF al Cliente
```

### 2.2 Componentes Involucrados

| Componente | Responsabilidad | Ubicación |
|------------|-----------------|-----------|
| `LabelServiceImpl` | Orquestación y reglas de negocio | Líneas 268-410 |
| `JasperLabelPrintService` | Generación de PDF | Todo el archivo |
| `LabelsPersistenceAdapter` | Persistencia y cambio de estado | Líneas 187-232 |
| `WarehouseAccessService` | Validación de permisos | Servicio externo |
| `JpaLabelRepository` | Acceso a datos de marbetes | Repositorio JPA |

---

## 3. Análisis Detallado de Funciones

### 3.1 FUNCIÓN PRINCIPAL: `printLabels()`

**Ubicación:** `LabelServiceImpl.java` - Líneas 268-410

#### 3.1.1 Signatura y Parámetros

```java
@Override
@Transactional
public byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole)
```

**Parámetros:**
- `dto.getPeriodId()` - ID del periodo fiscal
- `dto.getWarehouseId()` - ID del almacén
- `dto.getFolios()` - Lista opcional de folios específicos
- `dto.getProductId()` - Filtro opcional por producto
- `dto.getForceReprint()` - Flag para forzar reimpresión
- `userId` - ID del usuario que imprime
- `userRole` - Rol del usuario (ADMINISTRADOR, AUXILIAR, ALMACENISTA, etc.)

#### 3.1.2 Flujo de Ejecución Paso a Paso

##### PASO 1: Validación de Acceso por Rol (Líneas 273-281)

```java
if (userRole != null && (userRole.equalsIgnoreCase("ADMINISTRADOR") || 
                         userRole.equalsIgnoreCase("AUXILIAR"))) {
    log.info("Usuario {} tiene rol {} - puede imprimir en cualquier almacén", userId, userRole);
    // Los administradores y auxiliares pueden imprimir en cualquier almacén sin validación restrictiva
} else {
    // Para otros roles, validar acceso estricto al almacén
    warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
}
```

**Lógica:**
- Administradores y Auxiliares: Acceso completo a todos los almacenes
- Otros roles: Validación estricta por asignación de almacén

**🔴 ERROR CRÍTICO #1:** Si `userRole` es `null`, el código entra al bloque `else` y puede lanzar NullPointerException en `validateWarehouseAccess()`.

---

##### PASO 2: Validación de Catálogos Cargados (Líneas 283-292)

```java
boolean hasInventoryData = inventoryStockRepository.existsByWarehouseIdWarehouseAndPeriodId(
    dto.getWarehouseId(), dto.getPeriodId());

if (!hasInventoryData) {
    throw new tokai.com.mx.SIGMAV2.modules.labels.application.exception.CatalogNotLoadedException(
        "No se pueden imprimir marbetes porque no se han cargado los catálogos de inventario " +
        "y multialmacén para el periodo y almacén seleccionados. " +
        "Por favor, importe los datos antes de continuar.");
}
```

**Lógica:**
- Verifica si existe al menos un registro en `inventory_stock` para el periodo y almacén
- Si no existe, bloquea la impresión con mensaje descriptivo

**✅ BIEN IMPLEMENTADO:** Previene impresiones sin datos de catálogo

---

##### PASO 3: Determinación del Modo de Impresión (Líneas 296-345)

**MODO A: Impresión Selectiva (Folios Específicos)**

```java
if (dto.getFolios() != null && !dto.getFolios().isEmpty()) {
    // MODO SELECTIVO: Imprimir folios específicos (para reimpresión)
    log.info("Modo selectivo: Imprimiendo {} folios específicos", dto.getFolios().size());
    
    for (Long folio : dto.getFolios()) {
        Optional<Label> optLabel = persistence.findByFolioAndPeriodAndWarehouse(
            folio, dto.getPeriodId(), dto.getWarehouseId());

        if (optLabel.isEmpty()) {
            throw new LabelNotFoundException(
                String.format("Folio %d no encontrado para periodo %d y almacén %d",
                    folio, dto.getPeriodId(), dto.getWarehouseId()));
        }

        Label label = optLabel.get();

        // Validar que no esté cancelado
        if (label.getEstado() == Label.State.CANCELADO) {
            throw new InvalidLabelStateException(
                String.format("El folio %d está CANCELADO y no se puede imprimir", folio));
        }

        // Si no se fuerza reimpresión, validar que no esté ya impreso
        if (!Boolean.TRUE.equals(dto.getForceReprint()) && label.getEstado() == Label.State.IMPRESO) {
            throw new InvalidLabelStateException(
                String.format("El folio %d ya está IMPRESO. Use forceReprint=true para reimprimir", folio));
        }

        labelsToProcess.add(label);
    }
}
```

**Características:**
- Busca folios uno por uno
- Valida cada folio individualmente
- Bloquea cancelados
- Bloquea impresos (a menos que `forceReprint=true`)

**🟡 PROBLEMA DE DISEÑO #1:** Búsqueda individual de folios (N queries en lugar de 1 con IN clause)

**🔴 ERROR CRÍTICO #2:** Si el usuario envía 100 folios y el folio 99 está cancelado, se lanza excepción después de haber hecho 98 queries exitosas. **No hay validación previa del lote completo**.

---

**MODO B: Impresión Automática (Todos los Pendientes)**

```java
} else {
    // MODO AUTOMÁTICO: Imprimir todos los marbetes pendientes (no impresos)
    log.info("Modo automático: Imprimiendo todos los marbetes pendientes");

    if (dto.getProductId() != null) {
        // Filtrar por producto específico
        log.info("Filtrando por producto ID: {}", dto.getProductId());
        labelsToProcess = persistence.findPendingLabelsByPeriodWarehouseAndProduct(
            dto.getPeriodId(), dto.getWarehouseId(), dto.getProductId());
    } else {
        // Todos los marbetes pendientes del periodo/almacén
        labelsToProcess = persistence.findPendingLabelsByPeriodAndWarehouse(
            dto.getPeriodId(), dto.getWarehouseId());
    }

    if (labelsToProcess.isEmpty()) {
        throw new InvalidLabelStateException(
            "No hay marbetes pendientes de impresión para el periodo y almacén especificados");
    }

    log.info("Encontrados {} marbetes pendientes de impresión", labelsToProcess.size());
}
```

**Características:**
- Solo busca marbetes en estado `GENERADO`
- Puede filtrar por producto específico
- Query única (eficiente)
- Valida que existan pendientes

**✅ BIEN IMPLEMENTADO:** Este modo es más eficiente

---

##### PASO 4: Ordenamiento por Folio (Línea 348)

```java
labelsToProcess.sort(Comparator.comparing(Label::getFolio));
```

**✅ BIEN IMPLEMENTADO:** Garantiza impresión secuencial

---

##### PASO 5: Generación del PDF (Líneas 356-376)

```java
// Obtener rango de folios para registro
Long minFolio = labelsToProcess.stream()
    .map(Label::getFolio)
    .min(Long::compareTo)
    .orElseThrow();

Long maxFolio = labelsToProcess.stream()
    .map(Label::getFolio)
    .max(Long::compareTo)
    .orElseThrow();

// CAMBIO IMPORTANTE: Primero generar el PDF, luego marcar como impreso
// Esto evita que los marbetes queden marcados como impresos si falla la generación del PDF

// Generar el PDF con JasperReports
log.info("Generando PDF con {} marbetes...", labelsToProcess.size());
byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labelsToProcess);

// Validar que el PDF se generó correctamente
if (pdfBytes == null || pdfBytes.length == 0) {
    log.error("El PDF generado está vacío o es null");
    throw new RuntimeException("Error: El PDF generado está vacío. Verifique que los datos de productos y almacenes existan.");
}

log.info("PDF generado exitosamente: {} KB", pdfBytes.length / 1024);
```

**Lógica Correcta:**
1. Primero genera el PDF
2. Solo si el PDF es exitoso, actualiza el estado
3. Esto evita inconsistencias

**✅ BIEN IMPLEMENTADO:** El orden es correcto (PDF primero, luego estado)

**🔴 ERROR CRÍTICO #3:** El método `generateLabelsPdf()` está fuera de la transacción. Si falla después de la generación del PDF pero antes del commit, los datos quedan inconsistentes.

---

##### PASO 6: Actualización de Estado y Auditoría (Líneas 378-387)

```java
// Solo si el PDF se generó exitosamente, marcar como impresos y registrar
LabelPrint result = persistence.printLabelsRange(
    dto.getPeriodId(),
    dto.getWarehouseId(),
    minFolio,
    maxFolio,
    userId
);

log.info("Impresión registrada exitosamente: {} folio(s) del {} al {}",
    result.getCantidadImpresa(), result.getFolioInicial(), result.getFolioFinal());

return pdfBytes;
```

**Delegación a:** `LabelsPersistenceAdapter.printLabelsRange()`

---

##### PASO 7: Manejo de Excepciones (Líneas 389-403)

```java
} catch (IllegalArgumentException e) {
    log.error("Error de validación en impresión: {}", e.getMessage());
    throw new InvalidLabelStateException(e.getMessage());
} catch (IllegalStateException e) {
    log.error("Error de estado en impresión: {}", e.getMessage());
    throw new InvalidLabelStateException(e.getMessage());
} catch (RuntimeException e) {
    log.error("Error generando PDF: {}", e.getMessage());
    throw new InvalidLabelStateException("Error al generar el PDF de marbetes: " + e.getMessage());
}
```

**🟡 PROBLEMA DE DISEÑO #2:** Captura de `RuntimeException` genérica puede ocultar errores críticos. Además, si hay un error después de generar el PDF, la transacción hace rollback pero el PDF ya se generó (desperdicio de recursos).

---

### 3.2 FUNCIÓN AUXILIAR: `generateLabelsPdf()`

**Ubicación:** `JasperLabelPrintService.java` - Líneas 38-93

#### 3.2.1 Flujo de Generación de PDF

##### PASO 1: Pre-carga de Cache (Líneas 44-46)

```java
Map<Long, ProductEntity> productsCache = loadProductsCache(labels);
Map<Long, WarehouseEntity> warehousesCache = loadWarehousesCache(labels);
```

**Lógica:**
- Extrae todos los IDs únicos de productos y almacenes
- Carga en memoria con una query por tipo
- Previene el problema N+1

**✅ EXCELENTE IMPLEMENTACIÓN:** Esta optimización es crucial para rendimiento

**Implementación de Cache:**

```java
private Map<Long, ProductEntity> loadProductsCache(List<Label> labels) {
    Set<Long> productIds = new HashSet<>();
    for (Label label : labels) {
        productIds.add(label.getProductId());
    }

    List<ProductEntity> products = productRepository.findAllById(productIds);
    Map<Long, ProductEntity> cache = new HashMap<>();
    for (ProductEntity product : products) {
        cache.put(product.getIdProduct(), product);
    }

    log.info("Cache de productos cargado: {} productos", cache.size());
    return cache;
}
```

---

##### PASO 2: Carga de Plantilla JRXML (Líneas 48-49)

```java
JasperReport jasperReport = loadJasperTemplate();
```

**Implementación:**

```java
private JasperReport loadJasperTemplate() throws Exception {
    log.info("Cargando plantilla JRXML...");

    try {
        // Intentar cargar el archivo .jasper compilado primero
        InputStream jasperStream = new ClassPathResource("reports/Carta_Tres_Cuadros.jasper").getInputStream();
        log.info("Archivo .jasper encontrado, cargando...");
        return (JasperReport) JRLoader.loadObject(jasperStream);
    } catch (Exception e) {
        log.warn("No se encontró .jasper compilado, compilando .jrxml...");

        // Si no existe el .jasper, compilar el .jrxml
        InputStream jrxmlStream = new ClassPathResource("reports/Carta_Tres_Cuadros.jrxml").getInputStream();
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

        log.info("JRXML compilado exitosamente");
        return jasperReport;
    }
}
```

**Lógica:**
1. Intenta cargar `.jasper` pre-compilado (más rápido)
2. Si no existe, compila `.jrxml` en tiempo real
3. Devuelve el reporte compilado

**🟡 PROBLEMA DE DISEÑO #3:** Compilar JRXML en cada impresión es **extremadamente lento**. Debería pre-compilarse en build time o cachearse.

**🔵 MEJORA RECOMENDADA #1:** Implementar cache de reporte compilado en memoria (Singleton o Bean)

---

##### PASO 3: Construcción del DataSource (Líneas 52-68)

```java
List<Map<String, Object>> dataSource = buildDataSource(labels, productsCache, warehousesCache);

log.info("DataSource construido con {} registros", dataSource.size());

// Validar que el datasource no esté vacío
if (dataSource.isEmpty()) {
    log.error("El datasource está vacío. No se puede generar el PDF.");
    log.error("Esto puede ocurrir si:");
    log.error("- Los productos asociados a los marbetes no existen en la base de datos");
    log.error("- Los almacenes asociados a los marbetes no existen en la base de datos");
    log.error("- Hay datos huérfanos en la tabla labels");
    throw new RuntimeException(
        "No se puede generar el PDF: El datasource está vacío. " +
        "Verifique que todos los productos y almacenes asociados a los marbetes existan en la base de datos."
    );
}
```

**Construcción del DataSource:**

```java
private List<Map<String, Object>> buildDataSource(
        List<Label> labels,
        Map<Long, ProductEntity> productsCache,
        Map<Long, WarehouseEntity> warehousesCache) {

    List<Map<String, Object>> dataSource = new ArrayList<>();
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    String fechaActual = LocalDate.now().format(dateFormatter);

    for (Label label : labels) {
        Map<String, Object> record = new HashMap<>();

        // Obtener datos del producto
        ProductEntity product = productsCache.get(label.getProductId());
        if (product == null) {
            log.warn("Producto no encontrado para folio {}: productId={}",
                label.getFolio(), label.getProductId());
            continue; // 🔴 ERROR: Salta silenciosamente el marbete
        }

        // Obtener datos del almacén
        WarehouseEntity warehouse = warehousesCache.get(label.getWarehouseId());
        if (warehouse == null) {
            log.warn("Almacén no encontrado para folio {}: warehouseId={}",
                label.getFolio(), label.getWarehouseId());
            continue; // 🔴 ERROR: Salta silenciosamente el marbete
        }

        // Mapear datos a las variables del JRXML
        record.put("NomMarbete", String.valueOf(label.getFolio()));
        record.put("Codigo", product.getCveArt());

        // Truncar descripción si es muy larga
        String descripcion = product.getDescr();
        if (descripcion != null && descripcion.length() > 40) {
            descripcion = descripcion.substring(0, 37) + "...";
        }
        record.put("Descripcion", descripcion != null ? descripcion : "");

        record.put("CLAVE", product.getCveArt());
        record.put("DESCR", descripcion != null ? descripcion : "");

        // Datos del almacén
        record.put("Clave almacen", warehouse.getWarehouseKey());
        record.put("Nombre almacen", warehouse.getNameWarehouse());
        record.put("Almacen", warehouse.getWarehouseKey() + " " + warehouse.getNameWarehouse());

        // Fecha actual
        record.put("Fecha", fechaActual);

        dataSource.add(record);
    }

    return dataSource;
}
```

**🔴 ERROR CRÍTICO #4:** Si un producto o almacén no existe, el marbete se salta con `continue` silenciosamente. Esto genera un PDF **incompleto** sin notificar al usuario.

**Escenario de Falla:**
1. Usuario solicita imprimir folios 1-100
2. El folio 50 tiene un producto inexistente
3. Se genera PDF con 99 marbetes en lugar de 100
4. Los folios 1-100 quedan marcados como IMPRESOS
5. El folio 50 nunca se imprimió pero su estado dice que sí

**🔵 MEJORA RECOMENDADA #2:** Debe lanzar excepción si faltan datos, no continuar silenciosamente.

---

##### PASO 4: Generación del PDF con JasperReports (Líneas 71-78)

```java
JRBeanCollectionDataSource jrDataSource = new JRBeanCollectionDataSource(dataSource);
JasperPrint jasperPrint = JasperFillManager.fillReport(
    jasperReport,
    new HashMap<>(), // Parámetros globales (vacío por ahora)
    jrDataSource
);

// Exportar a PDF
byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
```

**✅ BIEN IMPLEMENTADO:** Uso estándar de JasperReports

---

### 3.3 FUNCIÓN DE PERSISTENCIA: `printLabelsRange()`

**Ubicación:** `LabelsPersistenceAdapter.java` - Líneas 187-232

#### 3.3.1 Flujo de Actualización de Estado

##### PASO 1: Validación de Rango (Líneas 188-194)

```java
if (endFolio < startFolio) {
    throw new IllegalArgumentException("Rango inválido: endFolio < startFolio");
}
long count = endFolio - startFolio + 1;
if (count > 500) {
    throw new IllegalArgumentException("Máximo 500 folios por lote.");
}
```

**✅ BIEN IMPLEMENTADO:** Validaciones básicas correctas

---

##### PASO 2: Búsqueda de Marbetes (Líneas 196-211)

```java
List<Label> labels = jpaLabelRepository.findByFolioBetween(startFolio, endFolio);

// Verificar que todos los folios existan
if (labels.size() != count) {
    // encontrar faltantes
    java.util.Set<Long> found = labels.stream().map(Label::getFolio).collect(Collectors.toSet());
    StringBuilder sb = new StringBuilder();
    for (long f = startFolio; f <= endFolio; f++) {
        if (!found.contains(f)) {
            if (sb.length() > 0) sb.append(',');
            sb.append(f);
        }
    }
    String missing = sb.toString();
    throw new IllegalStateException("No es posible imprimir marbetes no generados. Folios faltantes: " + missing);
}
```

**✅ EXCELENTE IMPLEMENTACIÓN:** Detecta folios faltantes y los lista específicamente

---

##### PASO 3: Validación de Pertenencia y Estado (Líneas 213-226)

```java
LocalDateTime now = LocalDateTime.now();
// Validar pertenencia a periodo/almacén y estado
for (Label l : labels) {
    if (!l.getPeriodId().equals(periodId) || !l.getWarehouseId().equals(warehouseId)) {
        throw new IllegalStateException("El folio " + l.getFolio() + " no pertenece al periodo/almacén seleccionado.");
    }
    if (l.getEstado() == Label.State.CANCELADO) {
        throw new IllegalStateException("No es posible imprimir marbetes cancelados. Folio: " + l.getFolio());
    }
    // permitir GENERADO o IMPRESO (reimpresión)
    l.setEstado(Label.State.IMPRESO);
    l.setImpresoAt(now);
}
```

**Lógica:**
- Valida que todos los folios pertenezcan al periodo/almacén correcto
- Bloquea marbetes cancelados
- Permite reimprimir marbetes ya impresos
- Actualiza estado y timestamp

**🟡 PROBLEMA DE DISEÑO #4:** La validación de periodo/almacén debería ser parte de la query, no un loop después.

**🔴 ERROR CRÍTICO #5:** Si se detecta un error en el folio 50 de 100, se lanza excepción pero los primeros 49 marbetes ya fueron modificados en memoria. Si la transacción hace rollback, está bien, pero si no está correctamente configurada, puede causar inconsistencias.

---

##### PASO 4: Persistencia (Líneas 228-239)

```java
// Guardar todos los labels actualizados
jpaLabelRepository.saveAll(labels);

// Crear registro en label_prints
LabelPrint lp = new LabelPrint();
lp.setPeriodId(periodId);
lp.setWarehouseId(warehouseId);
lp.setFolioInicial(startFolio);
lp.setFolioFinal(endFolio);
lp.setCantidadImpresa((int)count);
lp.setPrintedBy(userId);
lp.setPrintedAt(now);

LabelPrint saved = jpaLabelPrintRepository.save(lp);

return saved;
```

**✅ BIEN IMPLEMENTADO:** Registra auditoría de impresión

---

## 4. Errores Detectados

### 🔴 ERROR CRÍTICO #1: NullPointerException Potencial

**Ubicación:** `LabelServiceImpl.printLabels()` - Línea 274

**Código Problemático:**
```java
if (userRole != null && (userRole.equalsIgnoreCase("ADMINISTRADOR") || 
                         userRole.equalsIgnoreCase("AUXILIAR"))) {
    // OK
} else {
    warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
}
```

**Problema:**
Si `userRole` es `null`, el código entra al bloque `else` y `validateWarehouseAccess()` recibe `null` como parámetro, lo cual puede causar NullPointerException.

**Escenario de Falla:**
```
Request sin header de rol → userRole = null → validateWarehouseAccess(userId, warehouseId, null) → NPE
```

**Impacto:** ALTO - Puede causar falla de la API

**Solución Recomendada:**
```java
if (userRole == null) {
    throw new PermissionDeniedException("Rol de usuario requerido para imprimir marbetes");
}

if (userRole.equalsIgnoreCase("ADMINISTRADOR") || userRole.equalsIgnoreCase("AUXILIAR")) {
    log.info("Usuario {} tiene rol {} - puede imprimir en cualquier almacén", userId, userRole);
} else {
    warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
}
```

---

### 🔴 ERROR CRÍTICO #2: Validación Parcial en Modo Selectivo

**Ubicación:** `LabelServiceImpl.printLabels()` - Líneas 300-328

**Problema:**
Si el usuario envía una lista de 100 folios y el folio #99 está cancelado, el sistema:
1. Hace 98 queries exitosas
2. Lanza excepción en el folio #99
3. Desperdicia tiempo y recursos

**Escenario de Falla:**
```
Request: folios = [1, 2, 3, ..., 99, 100]
Folio 99 está CANCELADO
Resultado: 98 queries + 1 excepción → Rollback completo
```

**Impacto:** MEDIO - Desperdicio de recursos y experiencia de usuario pobre

**Solución Recomendada:**
```java
// Primero validar todos los folios con una sola query
List<Label> foundLabels = persistence.findByFoliosInAndPeriodAndWarehouse(
    dto.getFolios(), dto.getPeriodId(), dto.getWarehouseId());

// Validar que se encontraron todos
if (foundLabels.size() != dto.getFolios().size()) {
    // Encontrar los faltantes
    Set<Long> found = foundLabels.stream().map(Label::getFolio).collect(Collectors.toSet());
    Set<Long> missing = dto.getFolios().stream()
        .filter(f -> !found.contains(f))
        .collect(Collectors.toSet());
    throw new LabelNotFoundException("Folios no encontrados: " + missing);
}

// Validar estados en un solo pase
List<Long> cancelados = new ArrayList<>();
List<Long> yaImpresos = new ArrayList<>();

for (Label label : foundLabels) {
    if (label.getEstado() == Label.State.CANCELADO) {
        cancelados.add(label.getFolio());
    } else if (!Boolean.TRUE.equals(dto.getForceReprint()) && 
               label.getEstado() == Label.State.IMPRESO) {
        yaImpresos.add(label.getFolio());
    }
}

if (!cancelados.isEmpty()) {
    throw new InvalidLabelStateException("Folios cancelados: " + cancelados);
}
if (!yaImpresos.isEmpty()) {
    throw new InvalidLabelStateException("Folios ya impresos: " + yaImpresos + 
        ". Use forceReprint=true para reimprimir");
}

labelsToProcess = foundLabels;
```

---

### 🔴 ERROR CRÍTICO #3: Generación de PDF Fuera de Transacción

**Ubicación:** `LabelServiceImpl.printLabels()` - Líneas 362-376

**Problema:**
El método está marcado como `@Transactional`, pero la generación del PDF puede tomar varios segundos. Durante ese tiempo:
1. La transacción está abierta (holding locks)
2. Si el PDF falla, ya se consumió tiempo y recursos
3. Si hay un timeout de transacción, puede causar rollback inesperado

**Escenario de Falla:**
```
1. Inicia transacción
2. Valida marbetes (200ms)
3. Genera PDF (5000ms) ← AQUÍ SE MANTIENE LOCK EN BD
4. Si falla, rollback después de 5+ segundos
```

**Impacto:** ALTO - Problemas de concurrencia y locks de BD prolongados

**Solución Recomendada:**
```java
// Opción A: Generar PDF antes de la transacción
byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labelsToProcess);

// Luego ejecutar la transacción corta para actualizar estados
updateLabelsStateTransactional(periodId, warehouseId, minFolio, maxFolio, userId);

// Opción B: Usar @Transactional(propagation = Propagation.REQUIRES_NEW)
// en printLabelsRange() para tener transacción independiente
```

---

### 🔴 ERROR CRÍTICO #4: Salto Silencioso de Marbetes

**Ubicación:** `JasperLabelPrintService.buildDataSource()` - Líneas 173-180 y 184-190

**Código Problemático:**
```java
ProductEntity product = productsCache.get(label.getProductId());
if (product == null) {
    log.warn("Producto no encontrado para folio {}: productId={}",
        label.getFolio(), label.getProductId());
    continue; // 🔴 SALTA SILENCIOSAMENTE
}
```

**Problema:**
Si un producto o almacén no existe, el marbete se excluye del PDF sin notificar al usuario. Luego, todos los folios (incluyendo los excluidos) se marcan como IMPRESOS.

**Escenario de Falla Real:**
```
1. Usuario solicita imprimir folios 1-100
2. El folio 50 tiene productId=999 que fue eliminado de la BD
3. buildDataSource() genera 99 registros (omite folio 50)
4. PDF se genera con 99 marbetes
5. printLabelsRange() marca folios 1-100 como IMPRESOS
6. El folio 50 nunca se imprimió pero su estado dice "IMPRESO"
7. Usuario cree que imprimió 100, pero solo tiene 99 físicos
```

**Impacto:** CRÍTICO - Causa inconsistencia de datos y pérdida de control de inventario

**Solución Recomendada:**
```java
ProductEntity product = productsCache.get(label.getProductId());
if (product == null) {
    log.error("CRÍTICO: Producto no encontrado para folio {}: productId={}",
        label.getFolio(), label.getProductId());
    throw new IllegalStateException(
        "No se puede generar PDF: El folio " + label.getFolio() + 
        " está asociado a un producto inexistente (ID: " + label.getProductId() + "). " +
        "Esto indica datos huérfanos en la base de datos."
    );
}

WarehouseEntity warehouse = warehousesCache.get(label.getWarehouseId());
if (warehouse == null) {
    log.error("CRÍTICO: Almacén no encontrado para folio {}: warehouseId={}",
        label.getFolio(), label.getWarehouseId());
    throw new IllegalStateException(
        "No se puede generar PDF: El folio " + label.getFolio() + 
        " está asociado a un almacén inexistente (ID: " + label.getWarehouseId() + "). " +
        "Esto indica datos huérfanos en la base de datos."
    );
}
```

---

### 🔴 ERROR CRÍTICO #5: Modificación de Estado sin Validación Atómica

**Ubicación:** `LabelsPersistenceAdapter.printLabelsRange()` - Líneas 216-226

**Problema:**
El método valida y modifica el estado de los marbetes en un loop. Si se detecta un error después de modificar algunos registros, puede causar inconsistencias si la transacción no está correctamente configurada.

**Escenario de Falla:**
```
1. Loop procesa folios 1-100
2. Folios 1-49: estado modificado a IMPRESO en memoria
3. Folio 50: Detecta que está CANCELADO
4. Lanza IllegalStateException
5. Si @Transactional no está bien configurado, los primeros 49 pueden quedar modificados
```

**Impacto:** MEDIO-ALTO - Depende de la configuración de Spring Transaction

**Solución Recomendada:**
```java
// Primero validar TODOS sin modificar
List<String> errores = new ArrayList<>();
for (Label l : labels) {
    if (!l.getPeriodId().equals(periodId) || !l.getWarehouseId().equals(warehouseId)) {
        errores.add("Folio " + l.getFolio() + " no pertenece al periodo/almacén");
    }
    if (l.getEstado() == Label.State.CANCELADO) {
        errores.add("Folio " + l.getFolio() + " está cancelado");
    }
}

if (!errores.isEmpty()) {
    throw new IllegalStateException("Errores de validación: " + String.join("; ", errores));
}

// Si llegamos aquí, TODOS los marbetes son válidos
// Ahora sí modificar todos de forma segura
LocalDateTime now = LocalDateTime.now();
for (Label l : labels) {
    l.setEstado(Label.State.IMPRESO);
    l.setImpresoAt(now);
}
```

---

## 5. Problemas de Diseño

### 🟡 PROBLEMA DE DISEÑO #1: N+1 Queries en Modo Selectivo

**Ubicación:** `LabelServiceImpl.printLabels()` - Líneas 303-328

**Problema:**
```java
for (Long folio : dto.getFolios()) {
    Optional<Label> optLabel = persistence.findByFolioAndPeriodAndWarehouse(...);
    // Query individual por cada folio
}
```

**Impacto:**
- 100 folios = 100 queries
- Latencia total = N × latency_promedio
- Si latency = 10ms, para 100 folios = 1 segundo solo en queries

**Solución:**
```java
// Una sola query con IN clause
List<Label> labels = persistence.findByFoliosInAndPeriodAndWarehouse(
    dto.getFolios(), dto.getPeriodId(), dto.getWarehouseId());
```

---

### 🟡 PROBLEMA DE DISEÑO #2: Captura de RuntimeException Genérica

**Ubicación:** `LabelServiceImpl.printLabels()` - Líneas 398-403

**Código:**
```java
} catch (RuntimeException e) {
    log.error("Error generando PDF: {}", e.getMessage());
    throw new InvalidLabelStateException("Error al generar el PDF de marbetes: " + e.getMessage());
}
```

**Problemas:**
1. Enmascara errores críticos (OutOfMemoryError, NullPointerException, etc.)
2. Convierte todos los errores en InvalidLabelStateException (pérdida de información)
3. Dificulta el debugging

**Solución:**
```java
} catch (JRException e) {
    log.error("Error de JasperReports: {}", e.getMessage(), e);
    throw new InvalidLabelStateException("Error generando reporte PDF: " + e.getMessage());
} catch (IOException e) {
    log.error("Error de I/O al cargar plantilla: {}", e.getMessage(), e);
    throw new InvalidLabelStateException("Error cargando plantilla de marbete: " + e.getMessage());
}
// NO capturar RuntimeException genérica - dejar que suba
```

---

### 🟡 PROBLEMA DE DISEÑO #3: Compilación de JRXML en Runtime

**Ubicación:** `JasperLabelPrintService.loadJasperTemplate()` - Líneas 109-111

**Problema:**
```java
InputStream jrxmlStream = new ClassPathResource("reports/Carta_Tres_Cuadros.jrxml").getInputStream();
JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);
```

Compilar JRXML puede tomar 2-5 segundos. Hacerlo en cada impresión es ineficiente.

**Impacto:**
- Primera impresión: 5 segundos
- Cada impresión posterior: 5 segundos (si no existe .jasper)
- Desperdicio de CPU

**Solución:**
```java
@Component
public class JasperReportCache {
    private final Map<String, JasperReport> cache = new ConcurrentHashMap<>();
    
    public JasperReport getOrCompile(String templateName) {
        return cache.computeIfAbsent(templateName, key -> {
            try {
                InputStream stream = new ClassPathResource("reports/" + key + ".jrxml").getInputStream();
                return JasperCompileManager.compileReport(stream);
            } catch (Exception e) {
                throw new RuntimeException("Error compilando reporte: " + key, e);
            }
        });
    }
}
```

O mejor: Pre-compilar en build time con Maven plugin.

---

### 🟡 PROBLEMA DE DISEÑO #4: Validación de Periodo/Almacén en Loop

**Ubicación:** `LabelsPersistenceAdapter.printLabelsRange()` - Líneas 218-220

**Problema:**
```java
for (Label l : labels) {
    if (!l.getPeriodId().equals(periodId) || !l.getWarehouseId().equals(warehouseId)) {
        throw new IllegalStateException("...");
    }
}
```

**Mejor Solución:**
Hacer la validación en la query:
```java
List<Label> labels = jpaLabelRepository
    .findByFolioBetweenAndPeriodIdAndWarehouseId(startFolio, endFolio, periodId, warehouseId);
```

---

### 🟡 PROBLEMA DE DISEÑO #5: Falta de Límites en Impresión Automática

**Ubicación:** `LabelServiceImpl.printLabels()` - Línea 337

**Problema:**
```java
labelsToProcess = persistence.findPendingLabelsByPeriodAndWarehouse(
    dto.getPeriodId(), dto.getWarehouseId());
```

No hay límite. Si hay 10,000 marbetes pendientes:
- 10,000 registros en memoria
- PDF gigante (posible OutOfMemoryError)
- Transacción muy larga

**Solución:**
```java
if (labelsToProcess.size() > 500) {
    throw new InvalidLabelStateException(
        "Hay " + labelsToProcess.size() + " marbetes pendientes. " +
        "Por favor, imprima en lotes de máximo 500 marbetes. " +
        "Use el filtro por producto para reducir la cantidad."
    );
}
```

---

## 6. Recomendaciones

### 🔵 MEJORA RECOMENDADA #1: Implementar Cache de Reporte Compilado

**Beneficio:** Reducir tiempo de impresión de 5 segundos a <100ms

**Implementación:**
```java
@Component
public class JasperReportCacheService {
    private static final Logger log = LoggerFactory.getLogger(JasperReportCacheService.class);
    private final Map<String, JasperReport> reportCache = new ConcurrentHashMap<>();
    
    public JasperReport getReport(String templateName) {
        return reportCache.computeIfAbsent(templateName, this::loadAndCompile);
    }
    
    private JasperReport loadAndCompile(String templateName) {
        try {
            log.info("Compilando reporte: {}", templateName);
            ClassPathResource resource = new ClassPathResource("reports/" + templateName + ".jrxml");
            InputStream stream = resource.getInputStream();
            JasperReport report = JasperCompileManager.compileReport(stream);
            log.info("Reporte compilado y cacheado: {}", templateName);
            return report;
        } catch (Exception e) {
            log.error("Error compilando reporte: {}", templateName, e);
            throw new RuntimeException("Error compilando reporte: " + templateName, e);
        }
    }
    
    public void clearCache() {
        reportCache.clear();
        log.info("Cache de reportes limpiada");
    }
}
```

---

### 🔵 MEJORA RECOMENDADA #2: Validación Batch de Folios

**Beneficio:** Reducir 100 queries a 1 query

**Implementación:**
```java
// En LabelRepository (interface)
List<Label> findByFolioInAndPeriodIdAndWarehouseId(
    Collection<Long> folios, Long periodId, Long warehouseId);

// En LabelServiceImpl
if (dto.getFolios() != null && !dto.getFolios().isEmpty()) {
    labelsToProcess = persistence.findByFolioInAndPeriodIdAndWarehouseId(
        dto.getFolios(), dto.getPeriodId(), dto.getWarehouseId());
    
    // Validar que se encontraron todos
    if (labelsToProcess.size() != dto.getFolios().size()) {
        Set<Long> found = labelsToProcess.stream()
            .map(Label::getFolio)
            .collect(Collectors.toSet());
        Set<Long> missing = dto.getFolios().stream()
            .filter(f -> !found.contains(f))
            .collect(Collectors.toSet());
        throw new LabelNotFoundException("Folios no encontrados: " + missing);
    }
    
    // Validar estados todos a la vez
    validateLabelsForPrinting(labelsToProcess, dto.getForceReprint());
}

private void validateLabelsForPrinting(List<Label> labels, Boolean forceReprint) {
    List<Long> cancelados = new ArrayList<>();
    List<Long> yaImpresos = new ArrayList<>();
    
    for (Label label : labels) {
        if (label.getEstado() == Label.State.CANCELADO) {
            cancelados.add(label.getFolio());
        } else if (!Boolean.TRUE.equals(forceReprint) && 
                   label.getEstado() == Label.State.IMPRESO) {
            yaImpresos.add(label.getFolio());
        }
    }
    
    if (!cancelados.isEmpty()) {
        throw new InvalidLabelStateException(
            "Los siguientes folios están CANCELADOS: " + cancelados);
    }
    
    if (!yaImpresos.isEmpty()) {
        throw new InvalidLabelStateException(
            "Los siguientes folios ya están IMPRESOS: " + yaImpresos + 
            ". Use forceReprint=true para reimprimir");
    }
}
```

---

### 🔵 MEJORA RECOMENDADA #3: Separar Generación de PDF de Transacción

**Beneficio:** Evitar locks prolongados en BD

**Implementación:**
```java
@Override
public byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole) {
    // ... validaciones previas (sin transacción) ...
    
    // 1. Obtener y validar marbetes (transacción corta)
    List<Label> labelsToProcess = getAndValidateLabelsTransactional(dto, userId, userRole);
    
    // 2. Generar PDF (fuera de transacción)
    byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labelsToProcess);
    
    // 3. Actualizar estados (transacción corta)
    updateLabelsStateTransactional(dto, labelsToProcess, userId);
    
    return pdfBytes;
}

@Transactional(readOnly = true)
private List<Label> getAndValidateLabelsTransactional(PrintRequestDTO dto, Long userId, String userRole) {
    // Lógica de búsqueda y validación
    // Transacción corta: solo lecturas
    return labelsToProcess;
}

@Transactional
private void updateLabelsStateTransactional(PrintRequestDTO dto, List<Label> labels, Long userId) {
    Long minFolio = labels.stream().map(Label::getFolio).min(Long::compareTo).orElseThrow();
    Long maxFolio = labels.stream().map(Label::getFolio).max(Long::compareTo).orElseThrow();
    
    persistence.printLabelsRange(
        dto.getPeriodId(),
        dto.getWarehouseId(),
        minFolio,
        maxFolio,
        userId
    );
}
```

---

### 🔵 MEJORA RECOMENDADA #4: Añadir Métricas y Monitoreo

**Implementación:**
```java
@Override
@Transactional
public byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole) {
    long startTime = System.currentTimeMillis();
    
    try {
        // ... lógica de impresión ...
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        log.info("Impresión completada: {} marbetes en {} ms (PDF: {} KB)",
            labelsToProcess.size(), duration, pdfBytes.length / 1024);
        
        // Métricas para Prometheus/Grafana
        printDurationMetric.record(duration);
        printCountMetric.increment(labelsToProcess.size());
        
        return pdfBytes;
        
    } catch (Exception e) {
        printErrorMetric.increment();
        log.error("Error en impresión de marbetes", e);
        throw e;
    }
}
```

---

## 7. Resumen de Criticidad

### Errores Críticos (Requieren Corrección Inmediata)

| # | Error | Impacto | Prioridad |
|---|-------|---------|-----------|
| 1 | NullPointerException si userRole es null | ALTO | 🔴 CRÍTICA |
| 4 | Salto silencioso de marbetes sin notificación | CRÍTICO | 🔴 CRÍTICA |
| 5 | Modificación de estado sin validación atómica | MEDIO-ALTO | 🔴 ALTA |
| 2 | Validación parcial en modo selectivo | MEDIO | 🟡 MEDIA |
| 3 | Generación de PDF dentro de transacción | ALTO | 🟡 MEDIA |

### Mejoras Recomendadas (Optimizaciones)

| # | Mejora | Beneficio | Esfuerzo |
|---|--------|-----------|----------|
| 1 | Cache de reporte compilado | Reducir 5s a 100ms | BAJO |
| 2 | Validación batch de folios | Reducir 100 queries a 1 | MEDIO |
| 3 | Separar PDF de transacción | Evitar locks prolongados | ALTO |
| 4 | Añadir métricas | Mejor monitoreo | BAJO |

---

## 8. Plan de Acción Sugerido

### Fase 1: Correcciones Críticas (1-2 días)
1. ✅ Corregir error #1 (validación de userRole null)
2. ✅ Corregir error #4 (lanzar excepción en lugar de continue)
3. ✅ Añadir validación previa completa en error #5

### Fase 2: Optimizaciones Rápidas (1 día)
4. ✅ Implementar cache de reportes (mejora #1)
5. ✅ Implementar validación batch (mejora #2)

### Fase 3: Refactorización Profunda (3-5 días)
6. ✅ Separar PDF de transacción (mejora #3)
7. ✅ Añadir tests unitarios completos
8. ✅ Añadir tests de integración

### Fase 4: Monitoreo y Observabilidad (1 día)
9. ✅ Implementar métricas (mejora #4)
10. ✅ Configurar alertas

---

## 9. Conclusiones

El sistema de impresión de marbetes tiene una **arquitectura sólida** y cumple con la mayoría de las reglas de negocio. Sin embargo, presenta **errores críticos** que pueden causar:

1. **Inconsistencia de datos:** Marbetes marcados como impresos que no lo están
2. **Problemas de rendimiento:** N+1 queries, compilación repetida de reportes
3. **Experiencia de usuario pobre:** Errores tardíos después de queries innecesarias
4. **Dificultad de mantenimiento:** Falta de logging estructurado y métricas

Las correcciones propuestas son **relativamente sencillas** y tendrán un **impacto significativo** en la estabilidad y rendimiento del sistema.

### Riesgo Actual: 🟡 MEDIO-ALTO
### Riesgo Post-Corrección: 🟢 BAJO

---

**Documento generado el:** 2025-12-29  
**Autor del Análisis:** GitHub Copilot  
**Versión:** 1.0

