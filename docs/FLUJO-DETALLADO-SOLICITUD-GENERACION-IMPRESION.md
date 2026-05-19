Usuario solicita 10 folios
        ↓
[POST /labels/request]
        ↓
Tabla: label_requests
  ├─ requested_labels: 10
  └─ folios_generados: 0
        ↓
Usuario genera los marbetes
        ↓
[POST /labels/generate]
        ↓
Asigna folios: 1001-1010
        ↓
Tabla: labels (10 registros)
  ├─ folio: 1001, estado: GENERADO
  ├─ folio: 1002, estado: GENERADO
  └─ ... (hasta 1010)
        ↓
Usuario verifica si hay pendientes
        ↓
[POST /labels/pending-print-count]
        ↓
Respuesta: { count: 10 } ✅
        ↓
Usuario imprime
        ↓
[POST /labels/print]
        ↓
1. Busca marbetes GENERADO
2. Genera PDF con JasperReports
3. Actualiza a IMPRESO
4. Registra auditoría
        ↓
Tabla: labels (10 registros actualizados)
  ├─ folio: 1001, estado: IMPRESO ✅
  ├─ folio: 1002, estado: IMPRESO ✅
  └─ ... (hasta 1010)
        ↓
Tabla: label_prints (nuevo registro)
  ├─ folio_inicial: 1001
  ├─ folio_final: 1010
  ├─ cantidad_impresa: 10
  └─ printed_at: 2025-12-29 14:30:52
        ↓
Usuario descarga el PDF 📄Usuario solicita 10 folios
        ↓
[POST /labels/request]
        ↓
Tabla: label_requests
  ├─ requested_labels: 10
  └─ folios_generados: 0
        ↓
Usuario genera los marbetes
        ↓
[POST /labels/generate]
        ↓
Asigna folios: 1001-1010
        ↓
Tabla: labels (10 registros)
  ├─ folio: 1001, estado: GENERADO
  ├─ folio: 1002, estado: GENERADO
  └─ ... (hasta 1010)
        ↓
Usuario verifica si hay pendientes
        ↓
[POST /labels/pending-print-count]
        ↓
Respuesta: { count: 10 } ✅
        ↓
Usuario imprime
        ↓
[POST /labels/print]
        ↓
1. Busca marbetes GENERADO
2. Genera PDF con JasperReports
3. Actualiza a IMPRESO
4. Registra auditoría
        ↓
Tabla: labels (10 registros actualizados)
  ├─ folio: 1001, estado: IMPRESO ✅
  ├─ folio: 1002, estado: IMPRESO ✅
  └─ ... (hasta 1010)
        ↓
Tabla: label_prints (nuevo registro)
  ├─ folio_inicial: 1001
  ├─ folio_final: 1010
  ├─ cantidad_impresa: 10
  └─ printed_at: 2025-12-29 14:30:52
        ↓
Usuario descarga el PDF 📄Usuario solicita 10 folios
        ↓
[POST /labels/request]
        ↓
Tabla: label_requests
  ├─ requested_labels: 10
  └─ folios_generados: 0
        ↓
Usuario genera los marbetes
        ↓
[POST /labels/generate]
        ↓
Asigna folios: 1001-1010
        ↓
Tabla: labels (10 registros)
  ├─ folio: 1001, estado: GENERADO
  ├─ folio: 1002, estado: GENERADO
  └─ ... (hasta 1010)
        ↓
Usuario verifica si hay pendientes
        ↓
[POST /labels/pending-print-count]
        ↓
Respuesta: { count: 10 } ✅
        ↓
Usuario imprime
        ↓
[POST /labels/print]
        ↓
1. Busca marbetes GENERADO
2. Genera PDF con JasperReports
3. Actualiza a IMPRESO
4. Registra auditoría
        ↓
Tabla: labels (10 registros actualizados)
  ├─ folio: 1001, estado: IMPRESO ✅
  ├─ folio: 1002, estado: IMPRESO ✅
  └─ ... (hasta 1010)
        ↓
Tabla: label_prints (nuevo registro)
  ├─ folio_inicial: 1001
  ├─ folio_final: 1010
  ├─ cantidad_impresa: 10
  └─ printed_at: 2025-12-29 14:30:52
        ↓
Usuario descarga el PDF 📄Usuario solicita 10 folios
        ↓
[POST /labels/request]
        ↓
Tabla: label_requests
  ├─ requested_labels: 10
  └─ folios_generados: 0
        ↓
Usuario genera los marbetes
        ↓
[POST /labels/generate]
        ↓
Asigna folios: 1001-1010
        ↓
Tabla: labels (10 registros)
  ├─ folio: 1001, estado: GENERADO
  ├─ folio: 1002, estado: GENERADO
  └─ ... (hasta 1010)
        ↓
Usuario verifica si hay pendientes
        ↓
[POST /labels/pending-print-count]
        ↓
Respuesta: { count: 10 } ✅
        ↓
Usuario imprime
        ↓
[POST /labels/print]
        ↓
1. Busca marbetes GENERADO
2. Genera PDF con JasperReports
3. Actualiza a IMPRESO
4. Registra auditoría
        ↓
Tabla: labels (10 registros actualizados)
  ├─ folio: 1001, estado: IMPRESO ✅
  ├─ folio: 1002, estado: IMPRESO ✅
  └─ ... (hasta 1010)
        ↓
Tabla: label_prints (nuevo registro)
  ├─ folio_inicial: 1001
  ├─ folio_final: 1010
  ├─ cantidad_impresa: 10
  └─ printed_at: 2025-12-29 14:30:52
        ↓
Usuario descarga el PDF 📄Usuario solicita 10 folios
        ↓
[POST /labels/request]
        ↓
Tabla: label_requests
  ├─ requested_labels: 10
  └─ folios_generados: 0
        ↓
Usuario genera los marbetes
        ↓
[POST /labels/generate]
        ↓
Asigna folios: 1001-1010
        ↓
Tabla: labels (10 registros)
  ├─ folio: 1001, estado: GENERADO
  ├─ folio: 1002, estado: GENERADO
  └─ ... (hasta 1010)
        ↓
Usuario verifica si hay pendientes
        ↓
[POST /labels/pending-print-count]
        ↓
Respuesta: { count: 10 } ✅
        ↓
Usuario imprime
        ↓
[POST /labels/print]
        ↓
1. Busca marbetes GENERADO
2. Genera PDF con JasperReports
3. Actualiza a IMPRESO
4. Registra auditoría
        ↓
Tabla: labels (10 registros actualizados)
  ├─ folio: 1001, estado: IMPRESO ✅
  ├─ folio: 1002, estado: IMPRESO ✅
  └─ ... (hasta 1010)
        ↓
Tabla: label_prints (nuevo registro)
  ├─ folio_inicial: 1001
  ├─ folio_final: 1010
  ├─ cantidad_impresa: 10
  └─ printed_at: 2025-12-29 14:30:52
        ↓
Usuario descarga el PDF 📄# 🔄 Flujo Detallado: Solicitud → Generación → Impresión de Marbetes

**Fecha:** 2025-12-29  
**Documento:** Explicación paso a paso del flujo principal

---

## 📋 Resumen del Flujo

```
1. POST /labels/request          → Solicitar folios
2. POST /labels/generate         → Generar marbetes
3. POST /labels/pending-print-count → Verificar pendientes ⚠️
4. POST /labels/print           → Imprimir marbetes 📄
```

---

## 🎯 PASO 1: POST `/labels/request` - Solicitar Folios

### 📌 Propósito
Crear una **solicitud de folios** para un producto específico. Esto es como "reservar" cuántos marbetes vas a necesitar para ese producto.

### 📥 Request
```json
{
  "productId": 123,
  "warehouseId": 14,
  "periodId": 1,
  "requestedLabels": 10
}
```

### 🔍 ¿Qué hace internamente?

#### A. Valida acceso al almacén
```java
warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
```
- Solo puedes solicitar folios del almacén al que tienes acceso
- Excepto ADMINISTRADOR y AUXILIAR que tienen acceso total

#### B. Busca si ya existe una solicitud
```java
Optional<LabelRequest> existingRequest = persistence.findByProductWarehousePeriod(
    dto.getProductId(),
    dto.getWarehouseId(),
    dto.getPeriodId()
);
```

#### C. Valida la cantidad solicitada

**Caso 1: requestedLabels = 0**
- ✅ Si el producto NO existe en inventario → Cancela la solicitud anterior (si existe)
- ❌ Si el producto SÍ existe en inventario → Error (debe solicitar al menos 1)

```java
if (dto.getRequestedLabels() == 0) {
    Optional<InventoryStockEntity> stockOpt = inventoryStockRepository
        .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(...);
    
    if (stockOpt.isPresent()) {
        throw new InvalidLabelStateException(
            "No se puede solicitar 0 folios para un producto que existe en el inventario."
        );
    }
}
```

**Caso 2: requestedLabels > 0**
- Valida que NO haya marbetes GENERADOS sin imprimir
- Si ya existe una solicitud, la actualiza
- Si no existe, crea una nueva

```java
if (existingRequest.isPresent()) {
    // Verificar que no haya marbetes sin imprimir
    boolean hasUnprinted = persistence.existsGeneratedUnprintedForProductWarehousePeriod(...);
    if (hasUnprinted) {
        throw new InvalidLabelStateException(
            "Existen marbetes GENERADOS sin imprimir. Imprima los existentes primero."
        );
    }
    
    // Actualizar cantidad
    existing.setRequestedLabels(dto.getRequestedLabels());
    persistence.save(existing);
} else {
    // Crear nueva solicitud
    LabelRequest req = new LabelRequest();
    req.setProductId(dto.getProductId());
    req.setWarehouseId(dto.getWarehouseId());
    req.setPeriodId(dto.getPeriodId());
    req.setRequestedLabels(dto.getRequestedLabels());
    req.setFoliosGenerados(0);  // Aún no se han generado
    persistence.save(req);
}
```

### 📤 Response
```
HTTP 201 Created
(Sin cuerpo)
```

### 💾 Estado en Base de Datos

**Tabla: `label_requests`**
```sql
INSERT INTO label_requests (
    product_id, 
    warehouse_id, 
    period_id, 
    requested_labels, 
    folios_generados
) VALUES (
    123,  -- producto
    14,   -- almacén
    1,    -- periodo
    10,   -- folios solicitados
    0     -- aún no generados
);
```

### 🎯 Resultado
- ✅ Se crea o actualiza un registro en `label_requests`
- ✅ El sistema sabe que necesitas 10 marbetes para el producto 123
- ⏳ Aún NO se crean los marbetes físicos

---

## 🎯 PASO 2: POST `/labels/generate` - Generar Marbetes

### 📌 Propósito
**Crear físicamente los marbetes** basándose en la solicitud anterior. Aquí es donde se asignan los folios consecutivos.

### 📥 Request
```json
{
  "requestId": 456,      // ID de la solicitud creada en paso 1
  "periodId": 1,
  "warehouseId": 14,
  "labelsToGenerate": 10  // Cuántos generar en este lote
}
```

### 🔍 ¿Qué hace internamente?

#### A. Valida que existe la solicitud
```java
Optional<LabelRequest> opt = persistence.findByProductWarehousePeriod(
    dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId()
);

if (opt.isEmpty()) {
    throw new LabelNotFoundException("No existe una solicitud para el producto/almacén/periodo.");
}

LabelRequest req = opt.get();
```

#### B. Calcula cuántos puede generar
```java
int remaining = req.getRequestedLabels() - req.getFoliosGenerados();
// Ejemplo: 10 solicitados - 0 generados = 10 restantes

int toGenerate = Math.min(remaining, dto.getLabelsToGenerate());
// Genera el mínimo entre lo solicitado en el lote y lo que falta
```

**Ejemplo:**
- Solicité 10 folios en total
- Ya generé 0
- Quiero generar 10 en este lote
- Resultado: Genera 10

#### C. Consulta existencias (informativo)
```java
int existencias = 0;
var stockOpt = inventoryStockRepository
    .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(...);

if (stockOpt.isPresent()) {
    existencias = stockOpt.get().getExistQty().intValue();
}
```

#### D. Asigna folios consecutivos (TRANSACCIONAL)
```java
long[] range = persistence.allocateFolioRange(dto.getPeriodId(), toGenerate);
long primer = range[0];  // Ejemplo: 1001
long ultimo = range[1];  // Ejemplo: 1010
```

**¿Cómo funciona `allocateFolioRange`?**
```java
@Transactional
public synchronized long[] allocateFolioRange(Long periodId, int quantity) {
    // Busca el último folio usado en este periodo
    LabelFolioSequence seq = jpaLabelFolioSequenceRepository.findById(periodId)
        .orElseGet(() -> {
            LabelFolioSequence newSeq = new LabelFolioSequence();
            newSeq.setPeriodId(periodId);
            newSeq.setUltimoFolio(0L);
            return newSeq;
        });
    
    // Asigna el siguiente rango
    long primer = seq.getUltimoFolio() + 1;  // 1001
    long ultimo = seq.getUltimoFolio() + quantity;  // 1010
    
    // Actualiza el contador
    seq.setUltimoFolio(ultimo);
    jpaLabelFolioSequenceRepository.save(seq);
    
    return new long[]{primer, ultimo};
}
```

**Tabla: `label_folio_sequences`**
```sql
-- Antes
period_id | ultimo_folio
1         | 1000

-- Después
period_id | ultimo_folio
1         | 1010
```

#### E. Crea los marbetes individuales
```java
persistence.saveLabelsBatch(
    req.getIdLabelRequest(), 
    dto.getPeriodId(),
    dto.getWarehouseId(), 
    dto.getProductId(), 
    primer,   // 1001
    ultimo,   // 1010
    userId
);
```

**¿Qué hace `saveLabelsBatch`?**
```java
public void saveLabelsBatch(Long requestId, Long periodId, Long warehouseId, 
                            Long productId, long primer, long ultimo, Long createdBy) {
    List<Label> labels = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    
    // Crea un marbete por cada folio
    for (long folio = primer; folio <= ultimo; folio++) {
        Label label = new Label();
        label.setFolio(folio);              // 1001, 1002, 1003...
        label.setLabelRequestId(requestId);
        label.setPeriodId(periodId);
        label.setWarehouseId(warehouseId);
        label.setProductId(productId);
        label.setEstado(Label.State.GENERADO);  // ⭐ Estado inicial
        label.setCreatedBy(createdBy);
        label.setCreatedAt(now);
        labels.add(label);
    }
    
    // Guarda todos de una vez
    jpaLabelRepository.saveAll(labels);
}
```

#### F. Actualiza la solicitud
```java
int nuevosFoliosGenerados = req.getFoliosGenerados() + toGenerate;
req.setFoliosGenerados(nuevosFoliosGenerados);
persistence.save(req);
```

### 📤 Response
```json
{
  "totalGenerados": 10,
  "generadosConExistencias": 10,
  "generadosSinExistencias": 0,
  "primerFolio": 1001,
  "ultimoFolio": 1010,
  "mensaje": "Generación completada: 10 marbete(s) generados exitosamente"
}
```

### 💾 Estado en Base de Datos

**Tabla: `labels`**
```sql
folio | label_request_id | period_id | warehouse_id | product_id | estado    | created_at
1001  | 456              | 1         | 14           | 123        | GENERADO  | 2025-12-29 10:00:00
1002  | 456              | 1         | 14           | 123        | GENERADO  | 2025-12-29 10:00:00
1003  | 456              | 1         | 14           | 123        | GENERADO  | 2025-12-29 10:00:00
...
1010  | 456              | 1         | 14           | 123        | GENERADO  | 2025-12-29 10:00:00
```

**Tabla: `label_requests` (actualizada)**
```sql
UPDATE label_requests 
SET folios_generados = 10 
WHERE id = 456;
```

### 🎯 Resultado
- ✅ Se crean 10 registros en la tabla `labels`
- ✅ Cada uno tiene un folio único (1001-1010)
- ✅ Todos en estado `GENERADO`
- ✅ La solicitud se marca como "10 de 10 generados"

---

## 🎯 PASO 3: POST `/labels/pending-print-count` - Verificar Pendientes ⚠️

### 📌 Propósito
**VERIFICAR** cuántos marbetes están pendientes de impresión **ANTES** de intentar imprimir.

### ⚠️ ¿Por qué es importante?
- Si `count = 0` → No puedes imprimir (no hay marbetes en estado GENERADO)
- Si `count > 0` → Puedes proceder a imprimir

### 📥 Request
```json
{
  "periodId": 1,
  "warehouseId": 14,
  "productId": 123  // Opcional: filtrar por producto
}
```

### 🔍 ¿Qué hace internamente?

#### A. Busca marbetes en estado GENERADO
```java
List<Label> pendingLabels;

if (dto.getProductId() != null) {
    // Filtrar por producto específico
    pendingLabels = persistence.findPendingLabelsByPeriodWarehouseAndProduct(
        dto.getPeriodId(), dto.getWarehouseId(), dto.getProductId());
} else {
    // Todos los pendientes del periodo/almacén
    pendingLabels = persistence.findPendingLabelsByPeriodAndWarehouse(
        dto.getPeriodId(), dto.getWarehouseId());
}

long count = pendingLabels.size();
```

**Query SQL equivalente:**
```sql
SELECT COUNT(*) 
FROM labels 
WHERE period_id = 1 
  AND warehouse_id = 14 
  AND product_id = 123  -- si se especifica
  AND estado = 'GENERADO';
```

#### B. Obtiene información adicional
```java
String warehouseName = warehouseRepository.findById(dto.getWarehouseId())
    .map(w -> w.getNameWarehouse())
    .orElse(null);

String periodName = jpaPeriodRepository.findById(dto.getPeriodId())
    .map(p -> p.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    .orElse(null);
```

### 📤 Response
```json
{
  "count": 10,
  "periodId": 1,
  "warehouseId": 14,
  "warehouseName": "Almacén Central",
  "periodName": "2025-12-01"
}
```

### 🎯 Interpretación

**Si `count = 10`:**
- ✅ Hay 10 marbetes en estado GENERADO
- ✅ **PUEDES PROCEDER** al paso 4 (imprimir)
- ✅ El frontend puede mostrar: "Hay 10 marbetes listos para imprimir"

**Si `count = 0`:**
- ❌ NO hay marbetes pendientes
- ❌ **NO PUEDES IMPRIMIR**
- ❌ Debes generar marbetes primero (volver al paso 2)

### 💡 Uso en Frontend
```javascript
// SIEMPRE verificar antes de imprimir
async function verificarEImprimir() {
  // 1. Verificar pendientes
  const response = await axios.post('/api/sigmav2/labels/pending-print-count', {
    periodId: 1,
    warehouseId: 14
  });

  if (response.data.count === 0) {
    alert('❌ No hay marbetes pendientes de impresión. Genera marbetes primero.');
    return;
  }

  // 2. Mostrar confirmación
  const confirmar = confirm(
    `¿Desea imprimir ${response.data.count} marbetes del ${response.data.warehouseName}?`
  );

  if (!confirmar) return;

  // 3. Imprimir
  await imprimirMarbetes();
}
```

---

## 🎯 PASO 4: POST `/labels/print` - Imprimir Marbetes 📄

### 📌 Propósito
**Generar el PDF** con los marbetes y **cambiar su estado** de `GENERADO` a `IMPRESO`.

### 📥 Request - Modo Automático
```json
{
  "periodId": 1,
  "warehouseId": 14,
  "productId": 123  // Opcional: solo de este producto
}
```

### 📥 Request - Modo Selectivo
```json
{
  "periodId": 1,
  "warehouseId": 14,
  "folios": [1001, 1002, 1003],  // Folios específicos
  "forceReprint": false
}
```

### 🔍 ¿Qué hace internamente?

#### A. Validaciones iniciales
```java
// 1. Validar userRole
if (userRole == null || userRole.trim().isEmpty()) {
    throw new PermissionDeniedException("Rol de usuario requerido");
}

// 2. Validar acceso al almacén
validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

// 3. Validar que los catálogos estén cargados
validateCatalogsLoaded(dto.getWarehouseId(), dto.getPeriodId());
```

#### B. Obtener marbetes a imprimir (TRANSACCIÓN DE SOLO LECTURA)
```java
@Transactional(readOnly = true)
private List<Label> getAndValidateLabelsForPrinting(PrintRequestDTO dto) {
    List<Label> labelsToProcess;

    if (dto.getFolios() != null && !dto.getFolios().isEmpty()) {
        // MODO SELECTIVO: Folios específicos
        labelsToProcess = getAndValidateSpecificFolios(dto);
    } else {
        // MODO AUTOMÁTICO: Todos los pendientes
        labelsToProcess = getPendingLabels(dto);
    }

    return labelsToProcess;
}
```

**Modo Automático:**
```java
private List<Label> getPendingLabels(PrintRequestDTO dto) {
    List<Label> labels;
    
    if (dto.getProductId() != null) {
        // Solo del producto especificado
        labels = persistence.findPendingLabelsByPeriodWarehouseAndProduct(
            dto.getPeriodId(), dto.getWarehouseId(), dto.getProductId());
    } else {
        // Todos los pendientes
        labels = persistence.findPendingLabelsByPeriodAndWarehouse(
            dto.getPeriodId(), dto.getWarehouseId());
    }
    
    if (labels.isEmpty()) {
        throw new InvalidLabelStateException(
            "No hay marbetes pendientes de impresión"
        );
    }
    
    return labels;
}
```

**Query SQL equivalente:**
```sql
SELECT * FROM labels 
WHERE period_id = 1 
  AND warehouse_id = 14 
  AND estado = 'GENERADO'
ORDER BY folio;
```

#### C. Validar límite de seguridad
```java
if (labelsToProcess.size() > 500) {
    throw new InvalidLabelStateException(
        "Límite máximo: 500 marbetes por impresión"
    );
}
```

#### D. Ordenar por folio
```java
labelsToProcess.sort(Comparator.comparing(Label::getFolio));
```

#### E. Generar PDF (FUERA DE TRANSACCIÓN)
```java
byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labelsToProcess);

if (pdfBytes == null || pdfBytes.length == 0) {
    throw new InvalidLabelStateException("El PDF generado está vacío");
}
```

**¿Qué hace `generateLabelsPdf`?**
```java
public byte[] generateLabelsPdf(List<Label> labels) {
    // 1. Pre-cargar productos y almacenes (evita N+1 queries)
    Map<Long, ProductEntity> productsCache = loadProductsCache(labels);
    Map<Long, WarehouseEntity> warehousesCache = loadWarehousesCache(labels);
    
    // 2. Cargar plantilla JRXML (con cache)
    JasperReport jasperReport = reportCacheService.getReport("Carta_Tres_Cuadros");
    
    // 3. Construir datasource
    List<Map<String, Object>> dataSource = buildDataSource(
        labels, productsCache, warehousesCache
    );
    
    // 4. Validar que todos los productos existen
    if (dataSource.isEmpty()) {
        throw new IllegalStateException(
            "Datasource vacío - verifica que productos y almacenes existan"
        );
    }
    
    // 5. Generar PDF con JasperReports
    JRBeanCollectionDataSource jrDataSource = 
        new JRBeanCollectionDataSource(dataSource);
    JasperPrint jasperPrint = JasperFillManager.fillReport(
        jasperReport, new HashMap<>(), jrDataSource
    );
    
    // 6. Exportar a bytes
    byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
    
    return pdfBytes;
}
```

**Estructura del datasource:**
```java
for (Label label : labels) {
    ProductEntity product = productsCache.get(label.getProductId());
    
    // ⚠️ CORRECCIÓN CRÍTICA: Lanza error si falta producto
    if (product == null) {
        throw new IllegalStateException(
            "Folio " + label.getFolio() + " tiene producto inexistente"
        );
    }
    
    Map<String, Object> record = new HashMap<>();
    record.put("NomMarbete", String.valueOf(label.getFolio()));
    record.put("Codigo", product.getCveArt());
    record.put("Descripcion", product.getDescr());
    record.put("Almacen", warehouse.getNameWarehouse());
    record.put("Fecha", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    
    dataSource.add(record);
}
```

#### F. Actualizar estados (TRANSACCIÓN CORTA)
```java
@Transactional
private LabelPrint updateLabelsStateAfterPrint(
    Long periodId, Long warehouseId, Long minFolio, Long maxFolio, Long userId
) {
    LabelPrint result = persistence.printLabelsRange(
        periodId, warehouseId, minFolio, maxFolio, userId
    );
    return result;
}
```

**¿Qué hace `printLabelsRange`?**
```java
@Transactional
public synchronized LabelPrint printLabelsRange(
    Long periodId, Long warehouseId, Long startFolio, Long endFolio, Long userId
) {
    // 1. Buscar marbetes del rango
    List<Label> labels = jpaLabelRepository.findByFolioBetween(startFolio, endFolio);
    
    // 2. VALIDAR TODOS PRIMERO (sin modificar)
    List<String> errores = new ArrayList<>();
    for (Label l : labels) {
        if (!l.getPeriodId().equals(periodId) || 
            !l.getWarehouseId().equals(warehouseId)) {
            errores.add("Folio " + l.getFolio() + " no pertenece al periodo/almacén");
        }
        if (l.getEstado() == Label.State.CANCELADO) {
            errores.add("Folio " + l.getFolio() + " está cancelado");
        }
    }
    
    if (!errores.isEmpty()) {
        throw new IllegalStateException("Errores: " + String.join("; ", errores));
    }
    
    // 3. Si todo OK, modificar TODOS
    LocalDateTime now = LocalDateTime.now();
    for (Label l : labels) {
        l.setEstado(Label.State.IMPRESO);  // ⭐ Cambio de estado
        l.setImpresoAt(now);
    }
    
    // 4. Guardar cambios
    jpaLabelRepository.saveAll(labels);
    
    // 5. Registrar en auditoría
    LabelPrint lp = new LabelPrint();
    lp.setPeriodId(periodId);
    lp.setWarehouseId(warehouseId);
    lp.setFolioInicial(startFolio);
    lp.setFolioFinal(endFolio);
    lp.setCantidadImpresa((int)(endFolio - startFolio + 1));
    lp.setPrintedBy(userId);
    lp.setPrintedAt(now);
    
    return jpaLabelPrintRepository.save(lp);
}
```

### 📤 Response - Éxito
```
HTTP 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="marbetes_P1_A14_20251229_143052.pdf"
Content-Length: 245760

[BINARY PDF DATA]
```

### 📤 Response - Error
```json
HTTP 400 Bad Request
{
  "error": "Estado inválido",
  "message": "No hay marbetes pendientes de impresión para el periodo y almacén especificados"
}
```

### 💾 Estado en Base de Datos

**Tabla: `labels` (actualizada)**
```sql
-- Antes
folio | estado    | impreso_at
1001  | GENERADO  | NULL
1002  | GENERADO  | NULL
...

-- Después
folio | estado   | impreso_at
1001  | IMPRESO  | 2025-12-29 14:30:52
1002  | IMPRESO  | 2025-12-29 14:30:52
...
```

**Tabla: `label_prints` (nueva entrada)**
```sql
INSERT INTO label_prints (
    period_id, 
    warehouse_id, 
    folio_inicial, 
    folio_final, 
    cantidad_impresa, 
    printed_by, 
    printed_at
) VALUES (
    1, 
    14, 
    1001, 
    1010, 
    10, 
    10,  -- userId
    '2025-12-29 14:30:52'
);
```

### 🎯 Resultado
- ✅ PDF generado con 10 marbetes
- ✅ 10 marbetes cambiaron de `GENERADO` → `IMPRESO`
- ✅ Registro de auditoría creado en `label_prints`
- ✅ Usuario puede descargar el PDF

---

## 📊 Diagrama de Estados

```
┌─────────────────────────────────────────────────────────┐
│                  CICLO DE VIDA DE UN MARBETE            │
└─────────────────────────────────────────────────────────┘

1. POST /labels/request
   └─> SOLICITUD CREADA (label_requests)
       └─ Estado: requested_labels=10, folios_generados=0

2. POST /labels/generate
   └─> MARBETES CREADOS (labels)
       └─ Estado: GENERADO
          └─ folio: 1001-1010
          └─ product_id: 123
          └─ warehouse_id: 14
          └─ created_at: 2025-12-29 10:00:00

3. POST /labels/pending-print-count
   └─> VERIFICA: count=10 ✅
       └─ Hay 10 marbetes en estado GENERADO

4. POST /labels/print
   └─> MARBETES IMPRESOS (labels actualizados)
       └─ Estado: IMPRESO
          └─ impreso_at: 2025-12-29 14:30:52
       └─> AUDITORÍA (label_prints)
           └─ folio_inicial: 1001
           └─ folio_final: 1010
           └─ cantidad_impresa: 10
```

---

## 🔄 Flujo Completo con Ejemplos Reales

### Ejemplo 1: Producto con 100 unidades en inventario

```javascript
// PASO 1: Solicitar 5 folios
await axios.post('/api/sigmav2/labels/request', {
  productId: 123,
  warehouseId: 14,
  periodId: 1,
  requestedLabels: 5
});
// ✅ Solicitud creada: requested_labels=5, folios_generados=0

// PASO 2: Generar los 5 marbetes
const genResponse = await axios.post('/api/sigmav2/labels/generate', {
  requestId: 456,
  periodId: 1,
  warehouseId: 14,
  labelsToGenerate: 5
});
// ✅ Marbetes creados: folios 1001-1005 en estado GENERADO

console.log(genResponse.data);
// {
//   totalGenerados: 5,
//   primerFolio: 1001,
//   ultimoFolio: 1005
// }

// PASO 3: Verificar pendientes
const countResponse = await axios.post('/api/sigmav2/labels/pending-print-count', {
  periodId: 1,
  warehouseId: 14
});
// ✅ count: 5

console.log(`Hay ${countResponse.data.count} marbetes listos para imprimir`);
// "Hay 5 marbetes listos para imprimir"

// PASO 4: Imprimir
const pdfResponse = await axios.post('/api/sigmav2/labels/print', {
  periodId: 1,
  warehouseId: 14
}, {
  responseType: 'blob'
});
// ✅ PDF generado con 5 marbetes
// ✅ Folios 1001-1005 ahora en estado IMPRESO

// Descargar el PDF
const blob = new Blob([pdfResponse.data], { type: 'application/pdf' });
const url = window.URL.createObjectURL(blob);
const link = document.createElement('a');
link.href = url;
link.download = 'marbetes.pdf';
link.click();
```

### Ejemplo 2: Generar en lotes

```javascript
// Solicitar 20 folios
await axios.post('/api/sigmav2/labels/request', {
  productId: 124,
  warehouseId: 14,
  periodId: 1,
  requestedLabels: 20
});

// Generar primer lote de 10
await axios.post('/api/sigmav2/labels/generate', {
  requestId: 457,
  periodId: 1,
  warehouseId: 14,
  labelsToGenerate: 10
});
// ✅ Folios 1006-1015 generados

// Generar segundo lote de 10
await axios.post('/api/sigmav2/labels/generate', {
  requestId: 457,
  periodId: 1,
  warehouseId: 14,
  labelsToGenerate: 10
});
// ✅ Folios 1016-1025 generados

// Verificar pendientes
const count = await axios.post('/api/sigmav2/labels/pending-print-count', {
  periodId: 1,
  warehouseId: 14
});
// count: 20 (5 del producto anterior + 20 de este)

// Imprimir solo del producto 124
const pdf = await axios.post('/api/sigmav2/labels/print', {
  periodId: 1,
  warehouseId: 14,
  productId: 124
}, {
  responseType: 'blob'
});
// ✅ PDF con 20 marbetes del producto 124
```

---

## ⚠️ Errores Comunes y Soluciones

### Error 1: "No hay marbetes pendientes de impresión"

**Causa:** Llamaste a `/labels/print` sin generar marbetes primero

**Solución:**
```javascript
// ❌ MAL
await axios.post('/api/sigmav2/labels/print', { ... });
// Error: No hay marbetes pendientes

// ✅ BIEN
// 1. Solicitar
await axios.post('/api/sigmav2/labels/request', { ... });

// 2. Generar
await axios.post('/api/sigmav2/labels/generate', { ... });

// 3. Verificar
const count = await axios.post('/api/sigmav2/labels/pending-print-count', { ... });

// 4. Solo si count > 0, imprimir
if (count.data.count > 0) {
  await axios.post('/api/sigmav2/labels/print', { ... });
}
```

### Error 2: "No existe una solicitud para el producto/almacén/periodo"

**Causa:** Intentaste generar sin solicitar primero

**Solución:**
```javascript
// ❌ MAL
await axios.post('/api/sigmav2/labels/generate', {
  requestId: 999,  // No existe
  ...
});

// ✅ BIEN
// Primero solicitar
await axios.post('/api/sigmav2/labels/request', { ... });
// Luego generar
await axios.post('/api/sigmav2/labels/generate', { ... });
```

### Error 3: "Existen marbetes GENERADOS sin imprimir"

**Causa:** Intentaste solicitar más folios sin imprimir los anteriores

**Solución:**
```javascript
// ❌ MAL
await axios.post('/api/sigmav2/labels/request', { requestedLabels: 10 });
await axios.post('/api/sigmav2/labels/generate', { ... });
// Olvidaste imprimir
await axios.post('/api/sigmav2/labels/request', { requestedLabels: 20 });
// Error: Hay marbetes sin imprimir

// ✅ BIEN
await axios.post('/api/sigmav2/labels/request', { requestedLabels: 10 });
await axios.post('/api/sigmav2/labels/generate', { ... });
await axios.post('/api/sigmav2/labels/print', { ... });  // ✅ Imprimir primero
// Ahora sí puedes solicitar más
await axios.post('/api/sigmav2/labels/request', { requestedLabels: 20 });
```

---

## 📝 Resumen del Flujo

| Paso | API | Acción | Resultado |
|------|-----|--------|-----------|
| 1 | `/labels/request` | Solicitar 10 folios | Registro en `label_requests` |
| 2 | `/labels/generate` | Generar marbetes | 10 registros en `labels` (GENERADO) |
| 3 | `/labels/pending-print-count` | Verificar pendientes | count: 10 |
| 4 | `/labels/print` | Imprimir | PDF + actualiza a IMPRESO |

**Estados:**
- Después del paso 1: Solicitud creada (0 marbetes)
- Después del paso 2: 10 marbetes en estado `GENERADO`
- Después del paso 3: Confirmación de 10 pendientes
- Después del paso 4: 10 marbetes en estado `IMPRESO` + PDF descargable

---

**Documento generado:** 2025-12-29  
**Versión:** 1.0  
**Autor:** GitHub Copilot

