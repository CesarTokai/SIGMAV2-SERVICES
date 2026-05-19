# 📋 RESUMEN COMPLETO: REGLAS DE NEGOCIO MARBETES

**Módulo:** Labels (Marbetes)  
**Fecha:** 2026-03-19  
**Versión:** 2.0 (Consolidada)

---

## 📑 ÍNDICE

1. [CONSULTA-CAPTURA (Solicitud de Folios)](#consulta-captura)
2. [GENERACIÓN DE FOLIOS](#generación-de-folios)
3. [IMPRESIÓN](#impresión)
4. [CONTEO (C1 y C2)](#conteo)

---

# CONSULTA-CAPTURA
## Solicitud de Folios

### 🎯 Objetivo
Capturar la cantidad de folios (marbetes) que se desean generar para cada producto, periodo y almacén.

### 📋 Reglas de Negocio

#### RN-SOLICITUD-001: Solo cantidad numérica entera
- ✅ **Regla:** Solo puede ingresar una cantidad numérica entera positiva
- ✅ **Validación:** 
  - Tipo `Integer` en DTO
  - `@NotNull`, `@Min(1)` anotaciones
  - Frontend: validar entrada numérica
- 📍 **Ubicación:** `LabelRequestDTO.java`

#### RN-SOLICITUD-002: No alterar datos al buscar/ordenar
- ✅ **Regla:** Puede buscar u ordenar productos SIN alterar los datos existentes
- ✅ **Validación:**
  - Búsqueda en memoria después de cargar
  - No modifica BD
  - Operaciones de solo lectura
- 📍 **Ubicación:** `LabelServiceImpl.getLabelSummary()`

#### RN-SOLICITUD-003: No solicitar si hay marbetes generados sin imprimir
- ✅ **Regla:** No puede solicitar folios nuevos si previamente se generaron y NO han sido impresos
- ✅ **Validación:**
  ```java
  // En requestLabels()
  if (existing.getFoliosGenerados() > 0) {
      boolean hasUnprinted = persistence
          .existsGeneratedUnprintedForProductWarehousePeriod(
              productId, warehouseId, periodId);
      if (hasUnprinted) {
          throw InvalidLabelStateException(
              "Existen marbetes GENERADOS sin imprimir...");
      }
  }
  ```
- 📍 **Ubicación:** `LabelServiceImpl.requestLabels()` línea ~120

#### RN-SOLICITUD-004: Persistencia automática
- ✅ **Regla:** La solicitud se guarda automáticamente SIN necesidad de botón "Guardar"
- ✅ **Validación:**
  - `@Transactional` garantiza persistencia
  - Se guarda a `label_requests` inmediatamente
  - Frontend NO necesita botón guardar
- 📍 **Ubicación:** `LabelServiceImpl.requestLabels()`

#### RN-SOLICITUD-005: Cambiar cantidad antes de generar
- ✅ **Regla:** Mientras NO haya ejecutado "Generar marbetes", puede cambiar la cantidad las veces que desee
- ✅ **Validación:**
  - Si cantidad = 0 → Elimina solicitud (si foliosGenerados = 0)
  - Si existe solicitud → ACTUALIZA cantidad
  - Si no existe → CREA solicitud
  ```java
  if (dto.getRequestedLabels() == 0) {
      if (existing.isPresent() && existing.get().getFoliosGenerados() == 0) {
          persistence.delete(existing.get());  // ✅ Cancelar
      }
  } else if (existing.isPresent()) {
      existing.get().setRequestedLabels(dto.getRequestedLabels());
      persistence.save(existing.get());  // ✅ Actualizar
  } else {
      // ✅ Crear nueva
  }
  ```
- 📍 **Ubicación:** `LabelServiceImpl.requestLabels()` línea ~95-120

#### RN-SOLICITUD-006: Usar Tab para agilizar captura
- 🖥️ **Regla:** Puede usar tecla "Tab" para navegar entre productos
- 🖥️ **Responsabilidad:** Frontend (no código backend)

---

# GENERACIÓN DE FOLIOS
## Generar Marbetes

### 🎯 Objetivo
Convertir solicitudes de folios en marbetes reales (registros con folios secuenciales).

### 📋 Reglas de Negocio

#### RN-GEN-001: Solo productos con solicitud válida
- ✅ **Regla:** Solo puede generar marbetes para productos con solicitud y cantidad > 0
- ✅ **Validación:** El DTO debe contener `productId` y `labelsToGenerate > 0`
- 📍 **Ubicación:** `LabelGenerationService.generateBatchList()` línea ~40

#### RN-GEN-002: Productos sin folios existentes
- ✅ **Regla:** Si el producto YA tiene folios generados (foliosGenerados > 0), NO puede regenerar
- ✅ **Validación:**
  ```java
  List<Long> productosConFoliosExistentes = new ArrayList<>();
  for (ProductBatchDTO product : dto.getProducts()) {
      Optional<LabelRequest> existing = persistence
          .findByProductWarehousePeriod(productId, warehouseId, periodId);
      
      if (existing.isPresent() && 
          existing.get().getFoliosGenerados() > 0) {
          productosConFoliosExistentes.add(productId);
      }
  }
  if (!productosConFoliosExistentes.isEmpty()) {
      throw InvalidLabelStateException(...);
  }
  ```
- 📍 **Ubicación:** `LabelGenerationService.generateBatchList()` línea ~47-67

#### RN-GEN-003: Productos deben existir en catálogo
- ✅ **Regla:** El producto DEBE existir en la tabla `products` antes de generar
- ✅ **Validación:**
  ```java
  List<Long> productosNoEncontrados = new ArrayList<>();
  for (ProductBatchDTO product : dto.getProducts()) {
      if (!productRepository.existsById(product.getProductId())) {
          productosNoEncontrados.add(product.getProductId());
      }
  }
  if (!productosNoEncontrados.isEmpty()) {
      throw InvalidLabelStateException(
          "Los siguientes productos no existen: " + productosNoEncontrados);
  }
  ```
- 📍 **Ubicación:** `LabelGenerationService.generateBatchList()` línea ~77-90

#### RN-GEN-004: Folios son secuenciales globales
- ✅ **Regla:** Los folios se asignan secuencialmente a nivel de PERÍODO (no por almacén)
- ✅ **Validación:**
  ```java
  long[] range = persistence.allocateFolioRange(periodId, cantidad);
  // range[0] = primer folio, range[1] = último folio
  // Secuencia global: 1, 2, 3, ..., N
  ```
- ✅ **Ejemplo:**
  - Período 1, Almacén 5, Producto 10: Folios 1-10
  - Período 1, Almacén 6, Producto 20: Folios 11-20
  - Período 1, Almacén 5, Producto 15: Folios 21-30
  - **Resultado:** Secuencia global 1-30 (NO se regeneran números)
- 📍 **Ubicación:** `LabelsPersistenceAdapter.allocateFolioRange()`, `LabelGenerationService.generateBatchList()` línea ~102

#### RN-GEN-005: Marbetes creados en estado GENERADO
- ✅ **Regla:** Los marbetes se crean con estado = "GENERADO" (no IMPRESO)
- ✅ **Validación:**
  ```java
  Label label = new Label();
  label.setEstado(Label.State.GENERADO);  // ✅ Siempre GENERADO
  ```
- 📍 **Ubicación:** `LabelGenerationService.generateBatchList()` línea ~111

#### RN-GEN-006: LabelRequest se actualiza con folios generados
- ✅ **Regla:** El `foliosGenerados` en la solicitud se actualiza con la cantidad real generada
- ✅ **Validación:**
  ```java
  labelRequest.setFoliosGenerados(cantidad);
  labelRequest.setRequestedLabels(cantidad);
  persistence.save(labelRequest);
  ```
- 📍 **Ubicación:** `LabelGenerationService.generateBatchList()` línea ~95-100

#### RN-GEN-007: Control de acceso por rol
- ✅ **Regla:** Solo pueden generar: ADMINISTRADOR, AUXILIAR, ALMACENISTA
- ✅ **Validación:**
  ```java
  @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
  public void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole)
  ```
- 📍 **Ubicación:** `LabelsController.generateBatchList()` línea ~125

#### RN-GEN-008: Validación de acceso a almacén
- ✅ **Regla:** El usuario SOLO puede generar para almacenes asignados
- ✅ **Validación:**
  ```java
  warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
  // Si rol es ADMINISTRADOR/AUXILIAR: sin restricción
  // Si rol es ALMACENISTA: debe tener asignación en user_warehouse_assignments
  ```
- 📍 **Ubicación:** `LabelGenerationService.generateBatchList()` línea ~45

---

# IMPRESIÓN
## Generar PDF e Imprimir

### 🎯 Objetivo
Generar PDF con los marbetes generados y cambiar su estado a IMPRESO.

### 📋 Reglas de Negocio

#### RN-IMPRESIÓN-001: Solo marbetes en estado GENERADO
- ✅ **Regla:** Solo puede imprimir marbetes en estado "GENERADO" (no CANCELADOS)
- ✅ **Validación:**
  ```java
  List<Label> labels = persistence.findPendingLabelsByPeriodAndWarehouse(
      periodId, warehouseId);
  // Solo retorna marbetes con estado = GENERADO
  ```
- 📍 **Ubicación:** `LabelServiceImpl.printLabels()` línea ~175-186

#### RN-IMPRESIÓN-002: Puede imprimir por folios específicos
- ✅ **Regla:** Puede imprimir todos, o un rango/lista de folios específicos
- ✅ **Validación:**
  ```java
  if (dto.getFolios() != null && !dto.getFolios().isEmpty()) {
      labels = persistence.findByFoliosInAndPeriodAndWarehouse(
          dto.getFolios(), periodId, warehouseId);
  } else if (dto.getProductId() != null) {
      labels = persistence.findPendingLabelsByPeriodWarehouseAndProduct(
          periodId, warehouseId, productId);
  } else {
      labels = persistence.findPendingLabelsByPeriodAndWarehouse(
          periodId, warehouseId);
  }
  ```
- 📍 **Ubicación:** `LabelServiceImpl.printLabels()` línea ~173-187

#### RN-IMPRESIÓN-003: Máximo 500 marbetes por impresión
- ✅ **Regla:** No puede imprimir más de 500 marbetes en una sola operación
- ✅ **Validación:**
  ```java
  if (labels.size() > 500) {
      throw new InvalidLabelStateException("Límite máximo: 500 marbetes");
  }
  ```
- 📍 **Ubicación:** `LabelServiceImpl.printLabels()` línea ~189

#### RN-IMPRESIÓN-004: Producto debe existir
- ✅ **Regla:** El producto asociado DEBE existir en BD
- ✅ **Validación:**
  ```java
  ProductEntity product = productsCache.get(label.getProductId());
  if (product == null) {
      throw new IllegalStateException(
          "Producto inexistente para folio: " + folio);
  }
  ```
- 📍 **Ubicación:** `JasperLabelPrintService.buildDataSource()` línea ~150-154

#### RN-IMPRESIÓN-005: Almacén debe existir
- ✅ **Regla:** El almacén asociado DEBE existir en BD
- ✅ **Validación:**
  ```java
  WarehouseEntity warehouse = warehousesCache.get(label.getWarehouseId());
  if (warehouse == null) {
      throw new IllegalStateException(
          "Almacén inexistente para folio: " + folio);
  }
  ```
- 📍 **Ubicación:** `JasperLabelPrintService.buildDataSource()` línea ~160-164

#### RN-IMPRESIÓN-006: PDF generado correctamente
- ✅ **Regla:** El PDF debe tener contenido válido (no vacío)
- ✅ **Validación:**
  ```java
  byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels);
  if (pdfBytes == null || pdfBytes.length == 0) {
      throw new InvalidLabelStateException("Error generando PDF");
  }
  ```
- 📍 **Ubicación:** `LabelServiceImpl.printLabels()` línea ~196-198

#### RN-IMPRESIÓN-007: Cambiar estado a IMPRESO
- ✅ **Regla:** Después de generar PDF, todos los marbetes cambian a estado "IMPRESO"
- ✅ **Validación:**
  ```java
  for (Label l : filteredLabels) {
      l.setEstado(Label.State.IMPRESO);
      l.setImpresoAt(LocalDateTime.now());
  }
  jpaLabelRepository.saveAll(filteredLabels);
  ```
- 📍 **Ubicación:** `LabelsPersistenceAdapter.printLabelsRange()` línea ~225-230

#### RN-IMPRESIÓN-008: Registro de auditoría
- ✅ **Regla:** Se registra la impresión con timestamp y usuario
- ✅ **Validación:**
  ```java
  LabelPrint lp = new LabelPrint();
  lp.setPeriodId(periodId);
  lp.setWarehouseId(warehouseId);
  lp.setFolioInicial(actualMin);
  lp.setFolioFinal(actualMax);
  lp.setCantidadImpresa(filteredLabels.size());
  lp.setUsuarioId(userId);
  lp.setPrintedAt(LocalDateTime.now());
  jpaLabelPrintRepository.save(lp);
  ```
- 📍 **Ubicación:** `LabelsPersistenceAdapter.printLabelsRange()` línea ~232-241

#### RN-IMPRESIÓN-009: Control de acceso por rol
- ✅ **Regla:** Solo pueden imprimir: ADMINISTRADOR, AUXILIAR, ALMACENISTA
- ✅ **Validación:**
  ```java
  @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
  public byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole)
  ```
- 📍 **Ubicación:** `LabelsController.printLabels()` línea ~95

#### RN-IMPRESIÓN-010: Validación de acceso a almacén
- ✅ **Regla:** El usuario SOLO puede imprimir de almacenes asignados
- ✅ **Validación:** Igual a RN-GEN-008
- 📍 **Ubicación:** `LabelServiceImpl.printLabels()` línea ~172

#### RN-IMPRESIÓN-011: Reimpresión extraordinaria
- ✅ **Regla:** Puede reimprimir marbetes ya IMPRESOS con endpoint diferente
- ✅ **Validación:**
  ```java
  @Override
  public byte[] extraordinaryReprint(PrintRequestDTO dto, Long userId, String userRole) {
      // Solo busca marbetes en estado IMPRESO (no GENERADO)
      List<Label> labels = persistence.findImpresosForReimpresion(
          periodId, warehouseId, folios);
      // ... resto del proceso igual
  }
  ```
- 📍 **Ubicación:** `LabelServiceImpl.extraordinaryReprint()` línea ~218-240

---

# CONTEO
## Registro de Conteos Físicos (C1 y C2)

### 🎯 Objetivo
Registrar los conteos físicos del inventario en dos rondas: C1 (primer conteo) y C2 (verificación).

### 📋 Reglas de Negocio - CONTEO C1

#### RN-C1-001: Roles permitidos para C1
- ✅ **Regla:** Pueden registrar C1: ADMINISTRADOR, AUXILIAR, ALMACENISTA, AUXILIAR_DE_CONTEO
- ✅ **Validación:**
  ```java
  @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
  public void registerCountC1(CountRequestDTO dto, Long userId, String userRole)
  ```
- 📍 **Ubicación:** `LabelsController.registerCountC1()` línea ~145

#### RN-C1-002: Marbete debe estar IMPRESO
- ✅ **Regla:** Solo puede registrar conteo si el marbete está en estado "IMPRESO"
- ✅ **Validación:**
  ```java
  Label label = persistence.findByFolio(dto.getFolio())
      .orElseThrow(() -> new LabelNotFoundException("Marbete no existe"));
  
  if (label.getEstado() != Label.State.IMPRESO) {
      throw new InvalidLabelStateException(
          "El marbete debe estar IMPRESO, está: " + label.getEstado());
  }
  ```
- 📍 **Ubicación:** `LabelServiceImpl.registerCountC1()` línea ~280-290

#### RN-C1-003: No puede duplicar C1
- ✅ **Regla:** No puede registrar C1 dos veces para el mismo marbete
- ✅ **Validación:**
  ```java
  if (persistence.hasCountNumber(folio, 1)) {
      throw new DuplicateCountException("Conteo C1 ya registrado");
  }
  ```
- 📍 **Ubicación:** `LabelServiceImpl.registerCountC1()` línea ~292-295

#### RN-C1-004: No puede registrar C1 si ya existe C2
- ✅ **Regla:** Si ya se registró C2, no puede volver a registrar C1
- ✅ **Validación:**
  ```java
  if (persistence.hasCountNumber(folio, 2)) {
      throw new CountSequenceException(
          "No puede registrar C1: ya existe C2 registrado");
  }
  ```
- 📍 **Ubicación:** `LabelServiceImpl.registerCountC1()` línea ~297-300

#### RN-C1-005: Acceso a almacén
- ✅ **Regla:** User solo puede registrar en marbetes de almacenes asignados
- ✅ **Validación:**
  ```java
  warehouseAccessService.validateWarehouseAccess(
      userId, label.getWarehouseId(), userRole);
  ```
- 📍 **Ubicación:** `LabelServiceImpl.registerCountC1()` línea ~278

#### RN-C1-006: Cantidad debe ser numérica
- ✅ **Regla:** La cantidad registrada debe ser número entero >= 0
- ✅ **Validación:**
  ```java
  @Min(0)
  @NotNull
  private Integer quantity;  // En CountRequestDTO
  ```
- 📍 **Ubicación:** `CountRequestDTO.java`

#### RN-C1-007: Marbete no puede estar CANCELADO
- ✅ **Regla:** No puede registrar conteo en marbete cancelado
- ✅ **Validación:**
  ```java
  if (label.getEstado() == Label.State.CANCELADO) {
      throw new InvalidLabelStateException("Marbete está CANCELADO");
  }
  ```
- 📍 **Ubicación:** `LabelServiceImpl.registerCountC1()` línea ~288

---

### 📋 Reglas de Negocio - CONTEO C2

#### RN-C2-001: SOLO rol AUXILIAR_DE_CONTEO
- ✅ **Regla:** SOLO AUXILIAR_DE_CONTEO puede registrar C2 (roles exclusivo)
- ✅ **Validación:**
  ```java
  @PreAuthorize("hasRole('AUXILIAR_DE_CONTEO')")
  public void registerCountC2(CountRequestDTO dto, Long userId, String userRole)
  ```
- ⚠️ **Nota:** Este es el rol más restrictivo en el sistema
- 📍 **Ubicación:** `LabelsController.registerCountC2()` línea ~155

#### RN-C2-002: Marbete debe estar IMPRESO
- ✅ **Regla:** Igual a RN-C1-002 (marbete DEBE estar IMPRESO)
- ✅ **Ubicación:** `LabelServiceImpl.registerCountC2()` línea ~315-320

#### RN-C2-003: DEBE existir C1 previo
- ✅ **Regla:** No puede registrar C2 sin haber registrado C1 antes
- ✅ **Validación:**
  ```java
  if (!persistence.hasCountNumber(folio, 1)) {
      throw new CountSequenceException(
          "Debe existir Conteo C1 antes de registrar C2");
  }
  ```
- 📍 **Ubicación:** `LabelServiceImpl.registerCountC2()` línea ~322-325

#### RN-C2-004: No puede duplicar C2
- ✅ **Regla:** No puede registrar C2 dos veces para el mismo marbete
- ✅ **Validación:**
  ```java
  if (persistence.hasCountNumber(folio, 2)) {
      throw new DuplicateCountException("Conteo C2 ya registrado");
  }
  ```
- 📍 **Ubicación:** `LabelServiceImpl.registerCountC2()` línea ~327-330

#### RN-C2-005: Acceso a almacén
- ✅ **Regla:** Igual a RN-C1-005 (debe tener acceso a almacén)
- 📍 **Ubicación:** `LabelServiceImpl.registerCountC2()` línea ~313

#### RN-C2-006: Cantidad debe ser numérica
- ✅ **Regla:** Igual a RN-C1-006
- 📍 **Ubicación:** `CountRequestDTO.java`

#### RN-C2-007: Marbete no puede estar CANCELADO
- ✅ **Regla:** Igual a RN-C1-007
- 📍 **Ubicación:** `LabelServiceImpl.registerCountC2()` línea ~318

#### RN-C2-008: Registro de auditoría
- ✅ **Regla:** Se registra quién, cuándo y qué cantidad registró
- ✅ **Validación:** Automático por `@Transactional` con timestamps
- 📍 **Ubicación:** `LabelCount` entity (createdAt, createdBy)

---

## 📊 MATRIZ CONSOLIDADA DE VALIDACIONES

### Por Operación

| Operación | Validación | Estado | Regla |
|-----------|-----------|--------|-------|
| **SOLICITAR FOLIOS** | | | |
| | Solo cantidad > 0 | ✅ | RN-SOLICITUD-001 |
| | No altera datos | ✅ | RN-SOLICITUD-002 |
| | No si marbetes sin imprimir | ✅ | RN-SOLICITUD-003 |
| | Guarda automático | ✅ | RN-SOLICITUD-004 |
| | Cambia antes de generar | ✅ | RN-SOLICITUD-005 |
| **GENERAR MARBETES** | | | |
| | Productos con solicitud | ✅ | RN-GEN-001 |
| | Sin folios existentes | ✅ | RN-GEN-002 |
| | Producto en catálogo | ✅ | RN-GEN-003 |
| | Folios secuenciales | ✅ | RN-GEN-004 |
| | Estado = GENERADO | ✅ | RN-GEN-005 |
| | Actualiza foliosGenerados | ✅ | RN-GEN-006 |
| | Control de rol | ✅ | RN-GEN-007 |
| | Acceso a almacén | ✅ | RN-GEN-008 |
| **IMPRIMIR** | | | |
| | Solo GENERADO | ✅ | RN-IMPRESIÓN-001 |
| | Folios específicos o todos | ✅ | RN-IMPRESIÓN-002 |
| | Max 500 marbetes | ✅ | RN-IMPRESIÓN-003 |
| | Producto existe | ✅ | RN-IMPRESIÓN-004 |
| | Almacén existe | ✅ | RN-IMPRESIÓN-005 |
| | PDF válido | ✅ | RN-IMPRESIÓN-006 |
| | Estado → IMPRESO | ✅ | RN-IMPRESIÓN-007 |
| | Auditoría | ✅ | RN-IMPRESIÓN-008 |
| | Control de rol | ✅ | RN-IMPRESIÓN-009 |
| | Acceso a almacén | ✅ | RN-IMPRESIÓN-010 |
| | Reimpresión C2 | ✅ | RN-IMPRESIÓN-011 |
| **CONTEO C1** | | | |
| | Roles permitidos | ✅ | RN-C1-001 |
| | Marbete IMPRESO | ✅ | RN-C1-002 |
| | No duplicado | ✅ | RN-C1-003 |
| | No si C2 existe | ✅ | RN-C1-004 |
| | Acceso almacén | ✅ | RN-C1-005 |
| | Cantidad numérica | ✅ | RN-C1-006 |
| | No CANCELADO | ✅ | RN-C1-007 |
| **CONTEO C2** | | | |
| | SOLO AUXILIAR_DE_CONTEO | ✅ | RN-C2-001 |
| | Marbete IMPRESO | ✅ | RN-C2-002 |
| | C1 debe existir | ✅ | RN-C2-003 |
| | No duplicado | ✅ | RN-C2-004 |
| | Acceso almacén | ✅ | RN-C2-005 |
| | Cantidad numérica | ✅ | RN-C2-006 |
| | No CANCELADO | ✅ | RN-C2-007 |
| | Auditoría | ✅ | RN-C2-008 |

### Por Rol

| Rol | Solicitar | Generar | Imprimir | C1 | C2 |
|-----|-----------|---------|----------|----|----|
| **ADMINISTRADOR** | ✅ | ✅ | ✅ | ✅ | ❌ |
| **AUXILIAR** | ✅ | ✅ | ✅ | ✅ | ❌ |
| **ALMACENISTA** | ✅ | ✅ | ✅ | ✅ | ❌ |
| **AUXILIAR_DE_CONTEO** | ❌ | ❌ | ❌ | ✅ | ✅ |

---

## 📚 Referencias de Archivos

| Archivo | Reglas |
|---------|--------|
| `LabelServiceImpl.java` | Generación, Impresión, Conteo |
| `LabelGenerationService.java` | Generación de marbetes |
| `JasperLabelPrintService.java` | Validación de productos/almacenes |
| `LabelsPersistenceAdapter.java` | Persistencia y rango de folios |
| `LabelsController.java` | Control de acceso por rol |
| `WarehouseAccessService.java` | Validación de almacenes por usuario |

---

**¡Todas las reglas están implementadas y funcionando correctamente!** ✅

